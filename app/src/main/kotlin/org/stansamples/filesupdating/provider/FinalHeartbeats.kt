package org.stansamples.filesupdating.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.stansamples.filesupdating.entity.AppInfo
import org.stansamples.filesupdating.entity.GetActualFilePartException
import org.stansamples.filesupdating.entity.HeartbeatRequest
import org.stansamples.filesupdating.entity.HeartbeatRequestException
import org.stansamples.filesupdating.entity.HeartbeatsStoppedException
import org.stansamples.filesupdating.entity.HeartbeatsTTLException
import org.stansamples.filesupdating.entity.RangeNotSatisfiableException
import org.stansamples.filesupdating.util.hex
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

internal class FinalHeartbeats(
    private val dirs: Dirs,
    private val locals: Locals,
    private val packages: Packages,
    private val remotes: Remotes,
    private val secrets: Secrets,
    private val times: Times,
    contexts: Contexts,
    loggers: Loggers,
) : Heartbeats {
    private val _started = MutableStateFlow(false)
    private val _states = MutableStateFlow<Heartbeats.State>(Heartbeats.State.Stopped)
    private val coroutineScope = CoroutineScope(contexts.default + SupervisorJob())
    private val logger = loggers.create("[Heartbeats]")
    private val mutex = Mutex()
    private var _apps = listOf<AppInfo>()
    private var _lastError: Throwable? = null
    private var _timeRequested: Duration? = null
    private var _timeStarted: Duration = Duration.ZERO
    private var _timeUpdated: Duration? = null
    override val started = _started.asStateFlow()
    override val states = _states.asStateFlow()

    init {
        coroutineScope.launch {
            var job: Job? = null
            _started.drop(1).collect { newState ->
                logger.debug("started: $newState")
                if (newState) {
                    _lastError = null
                    _timeRequested = null
                    _timeStarted = times.now()
                    _timeUpdated = null
                    job = coroutineScope.launch {
                        while (_started.value) {
                            _states.value = Heartbeats.State.Updating
                            val timeStart = times.now()
                            try {
                                heartbeat()
                            } catch (error: Throwable) {
                                logger.warning("heartbeat error: $error")
                                if (error is HeartbeatRequestException) {
                                    _states.value = Heartbeats.State.Waiting(timeUpdated = _timeUpdated)
                                } else {
                                    _started.value = false
                                }
                            }
                            if (_started.value && _states.value is Heartbeats.State.Waiting) {
                                delay(locals.ttl - times.elapsed(timeStart = timeStart))
                            }
                        }
                    }
                } else {
                    job?.cancelAndJoin()
                    _states.value = Heartbeats.State.Stopped
                }
            }
        }
    }

    private fun expires(timeRequested: Duration? = _timeRequested): Boolean {
        return timeRequested != null && times.elapsed(timeRequested) > locals.ttl
    }

    private fun OutputStream.write(src: InputStream): Long {
        val buffer = ByteArray(8_192)
        var read = 0L
        while (true) {
            val length = src.read(buffer)
            if (length < 0) break
            write(buffer, 0, length)
            read += length
        }
        flush()
        return read
    }

    private fun download(
        serialNumber: String,
        packageName: String,
        id: String,
        dst: File,
    ) {
        var _index = dst.length()
        var _fullLength: Long? = null
        val partSize = 64_000_000L
        while (true) {
            if (!_started.value) throw HeartbeatsStoppedException()
            if (expires()) throw HeartbeatsTTLException()
            val range = LongRange(_index, _index + partSize - 1)
            if (range.isEmpty()) error("download empty range")
            val response = try {
                remotes.getActualFilePart(serialNumber = serialNumber, id = id, range = range)
            } catch (error: Throwable) {
                throw GetActualFilePartException(cause = error)
            }
            response.body.use { body ->
                if (_fullLength == null) {
                    _fullLength = response.fullLength
                } else if (response.fullLength != _fullLength) {
                    error("download wrong full length")
                }
                if (body.contentLength <= 0) {
                    error("download wrong content length")
                }
                val read = FileOutputStream(dst, true).use { dst ->
                    dst.write(src = body.src)
                }
                if (read != body.contentLength) {
                    error("download wrong read body length")
                }
            }
            _index += response.body.contentLength
            val read = dst.length()
            if (read > response.fullLength) {
                error("download wrong read length")
            }
            _states.value = Heartbeats.State.DownloadingPackage(
                packageName = packageName,
                contentLength = response.fullLength,
                read = read,
            )
            if (read == response.fullLength) return
        }
    }

    private fun commit(packageName: String, sha256: ByteArray, tmp: File, target: File) {
        if (!tmp.inputStream().use(secrets::sha256).contentEquals(sha256)) {
            error("commit $packageName hash error")
        }
        if (!tmp.renameTo(target)) {
            error("commit $packageName target error")
        }
    }

    private fun getPackageFile(dir: File, id: String): File {
        if (!dir.exists()) dir.mkdir()
        val timeNow = times.now()
        val hash = secrets.sha256(id.toByteArray()).copyOf(8).hex()
        for (file in dir.listFiles().orEmpty()) {
            var split = file.name.split(".")
            if (split.size != 2 || split[0].isEmpty() || split[1] != "apk") {
                file.delete()
                continue
            }
            split = split[0].split("_")
            if (split.size != 2 || split[0].isEmpty() || split[1].isEmpty()) {
                file.delete()
                continue
            }
            val time = split[0].toLongOrNull()?.milliseconds
            if (time == null || time > timeNow || (timeNow - time) > 24.hours) {
                file.delete()
                continue
            }
            if (split[1] == hash) {
                return file
            }
        }
        return dir.resolve("${timeNow.inWholeMilliseconds}_${hash}.apk")
    }

    private fun download(
        serialNumber: String,
        packageName: String,
        id: String,
        sha256: ByteArray,
    ): File {
        val dir = dirs.cache.resolve("packages")
        val target = dir.resolve("target.apk")
        if (target.exists() && !target.delete()) {
            error("cannot delete old target")
        }
        val tmp = getPackageFile(dir = dir, id = id)
        try {
            download(serialNumber = serialNumber, packageName = packageName, id = id, dst = tmp)
        } catch (error: Throwable) {
            when (error) {
                is HeartbeatsTTLException -> Unit
                is GetActualFilePartException -> {
                    if (error.cause is RangeNotSatisfiableException) tmp.delete()
                }
                else -> tmp.delete()
            }
            throw error
        } finally {
            try {
                remotes.dequeue(serialNumber = serialNumber, id = id)
            } catch (error: Throwable) {
                logger.warning("dequeue error: $error")
            }
        }
        try {
            commit(packageName = packageName, tmp = tmp, target = target, sha256 = sha256)
        } finally {
            tmp.delete()
        }
        return target
    }

    private fun install(file: File): String {
        val listener = object : Packages.InstallListener {
            override fun onProgress(
                packageName: String,
                contentLength: Long,
                read: Long,
            ) {
                _states.value = Heartbeats.State.InstallingPackage(
                    packageName = packageName,
                    contentLength = contentLength,
                    read = read,
                )
            }

            override fun onStartInstall(packageName: String) {
                logger.debug("install $packageName start")
            }

            override fun onFinishInstall(packageName: String) {
                logger.debug("install $packageName finish")
            }
        }
        return packages.install(
            file = file,
            listener = listener,
        )
    }

    private fun updateApps(packageNames: Set<String>) {
        val apps = mutableListOf<AppInfo>()
        for (packageName in packageNames) {
            apps += try {
                packages.getAppInfo(packageName = packageName)
            } catch (error: Throwable) {
                logger.warning("get info $packageName error: $error")
                continue
            }
        }
        _apps = apps
    }

    private fun heartbeat() {
        val serialNumber = locals.serialNumber ?: error("No serial number!")
        val packageNames = _apps.mapTo(mutableSetOf()) { it.packageName }
        updateApps(packageNames = packageNames)
        val dcTimeout = locals.dcTimeout
        if (dcTimeout != null && times.elapsed(_timeUpdated ?: _timeStarted) > dcTimeout) {
            logger.debug("dc timeout")
            _started.value = false
            return
        }
        logger.debug("heartbeat request...")
        val lastRequested = _timeRequested
        _timeRequested = times.now()
        val response = try {
            remotes.heartbeat(HeartbeatRequest(serialNumber = serialNumber, apps = _apps))
        } catch (error: Throwable) {
            _lastError = error
            throw HeartbeatRequestException(cause = error)
        }
        val lastUpdated = _timeUpdated
        val lastError = _lastError
        if (lastError != null) {
            _lastError = null
            val message = """
                success after failure
                last requested: ${lastRequested?.inWholeMilliseconds}
                last updated: ${lastUpdated?.inWholeMilliseconds}
                last error: $lastError
            """.trimIndent()
            logger.debug(message)
        }
        _timeUpdated = times.now()
        for (pu in response.needsPackagesUpdate) {
            if (!_started.value) return
            if (expires()) return
            _states.value = Heartbeats.State.UpdatingPackage(packageName = pu.packageName, version = pu.version)
            logger.debug("updating ${pu.packageName}")
            if (!packageNames.contains(pu.packageName)) {
                val appInfo = try {
                    packages.getAppInfo(packageName = pu.packageName)
                } catch (_: Throwable) {
                    null
                }
                if (appInfo != null && pu.version == "${appInfo.versionName}-${appInfo.versionCode}") {
                    packageNames += appInfo.packageName
                    continue
                }
            }
            val file = try {
                download(
                    serialNumber = serialNumber,
                    packageName = pu.packageName,
                    id = pu.id,
                    sha256 = pu.sha256,
                )
            } catch (error: Throwable) {
                logger.warning("download ${pu.packageName} error: $error")
                continue
            }
            packageNames += try {
                install(file = file)
            } catch (error: Throwable) {
                logger.warning("install ${pu.packageName} error: $error")
                continue
            }
        }
        _states.value = Heartbeats.State.Updating
        updateApps(packageNames = packageNames)
        _states.value = Heartbeats.State.Waiting(timeUpdated = _timeUpdated)
    }

    override fun start() {
        coroutineScope.launch {
            mutex.withLock {
                if (!_started.value) {
                    logger.debug("starting")
                    _started.value = true
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            mutex.withLock {
                if (_started.value) {
                    logger.debug("stopping")
                    _started.value = false
                }
            }
        }
    }
}
