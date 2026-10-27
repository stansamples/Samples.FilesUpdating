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
import org.stansamples.filesupdating.entity.GetActualFilePartException
import org.stansamples.filesupdating.entity.HeartbeatRequestException
import org.stansamples.filesupdating.entity.HeartbeatsStoppedException
import org.stansamples.filesupdating.entity.HeartbeatsTTLException
import org.stansamples.filesupdating.entity.PackageUpdate
import org.stansamples.filesupdating.entity.RangeNotSatisfiableException
import org.stansamples.filesupdating.entity.TooManyRequestsException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FinalHeartbeats(
    private val dirs: Dirs,
    private val remotes: Remotes,
    private val secrets: Secrets,
    private val times: Times,
    contexts: Contexts,
    loggers: Loggers,
) : Heartbeats {
    private val logger = loggers.create("[Heartbeats]")
    private val serialNumber = "foobarbaz" // todo
    private val ttl = 8.seconds // todo
    private var _requested: Duration? = null
    private var _updated: Duration? = null
    private val _started = MutableStateFlow(false)
    override val started = _started.asStateFlow()
    private val _states = MutableStateFlow<Heartbeats.State>(Heartbeats.State.Stopped)
    override val states = _states.asStateFlow()
    private val coroutineScope = CoroutineScope(contexts.default + SupervisorJob())

    init {
        coroutineScope.launch {
            var job: Job? = null
            _started.drop(1).collect { newState ->
                logger.debug("started: $newState")
                if (newState) {
                    _requested = null
                    _updated = null
                    job = coroutineScope.launch {
                        while (_started.value) {
                            _states.value = Heartbeats.State.Updating
                            val timeStart = times.now()
                            try {
                                heartbeat()
                            } catch (error: Throwable) {
                                logger.warning("heartbeat error: $error")
                                if (error is HeartbeatRequestException) {
                                    _states.value = Heartbeats.State.Waiting(updated = _updated)
                                } else {
                                    _started.value = false
                                }
                            }
                            if (_started.value && _states.value is Heartbeats.State.Waiting) {
                                delay(ttl - times.elapsed(timeStart = timeStart))
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

    private fun download(id: String, dst: File) {
        var _index = dst.length()
        var _fullLength: Long? = null
        val partSize = 64_000_000L
        while (true) {
            if (!_started.value) throw HeartbeatsStoppedException()
            val requested = _requested
            if (requested != null && times.elapsed(requested) > ttl) {
                throw HeartbeatsTTLException()
            }
            val range = LongRange(_index, _index + partSize - 1)
            if (range.isEmpty()) error("download empty range")
            val response = try {
                remotes.getActualFilePart(serialNumber = serialNumber, id = id, range = range)
            } catch (error: Throwable) {
                when (error) {
                    is TooManyRequestsException -> throw error
                    is RangeNotSatisfiableException -> throw error
                    else -> throw GetActualFilePartException(cause = error)
                }
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
                    error("download wrong read length")
                }
            }
            _index += response.body.contentLength
            val read = dst.length()
            if (read > response.fullLength) {
                error("download wrong read length")
            }
            if (read == response.fullLength) return
        }
    }

    private fun commit(tmp: File, target: File, sha256: ByteArray) {
        if (!tmp.inputStream().use(secrets::sha256).contentEquals(sha256)) {
            error("commit hash error")
        }
        if (!tmp.renameTo(target)) {
            error("commit target error")
        }
    }

    private fun install(file: File) {
        TODO("install($file)")
    }

    private fun getPackageFile(dir: File, id: String): File {
        if (!dir.exists()) {
            if (!dir.mkdirs()) TODO()
        }
        return dir.resolve("${id}.apk")
    }

    private fun updatePackage(pu: PackageUpdate): File? {
        val dir = dirs.cache.resolve("packages")
        val target = dir.resolve("target.apk")
        if (target.exists() && !target.delete()) {
            error("update cannot delete old target")
        }
        val tmp = getPackageFile(dir = dir, id = pu.id)
        try {
            download(id = pu.id, dst = tmp)
        } catch (error: Throwable) {
            when (error) {
                is HeartbeatsTTLException,
                is TooManyRequestsException,
                is GetActualFilePartException -> Unit
                else -> tmp.delete()
            }
            return null
        } finally {
            try {
                remotes.dequeue(serialNumber = serialNumber, id = pu.id)
            } catch (error: Throwable) {
                logger.warning("dequeue error: $error")
            }
        }
        try {
            commit(tmp = tmp, target = target, sha256 = pu.sha256)
        } finally {
            tmp.delete()
        }
        return target
    }

    private fun heartbeat() {
        logger.debug("heartbeat request...")
        val requested = times.now()
        _requested = requested
        val response = try {
            remotes.heartbeat(serialNumber = serialNumber)
        } catch (error: Throwable) {
            throw HeartbeatRequestException(cause = error)
        }
        _updated = times.now()
        for (pu in response.needsPackagesUpdate) {
            if (!_started.value) return
            if (times.elapsed(requested) > ttl) return
            logger.debug("updating package: $pu")
            val file = try {
                updatePackage(pu = pu)
            } catch (error: Throwable) {
                logger.warning("updating package error: $error")
                continue
            } ?: continue
            try {
                install(file = file)
            } catch (error: Throwable) {
                logger.warning("install package error: $error")
                continue
            }
        }
        _states.value = Heartbeats.State.Waiting(updated = _updated)
    }

    override fun start() {
        coroutineScope.launch {
            if (!_started.value) {
                logger.debug("starting")
                _started.value = true
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            if (_started.value) {
                logger.debug("stopping")
                _started.value = false
            }
        }
    }
}
