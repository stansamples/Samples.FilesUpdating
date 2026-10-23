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
import kotlinx.coroutines.withContext
import org.stansamples.filesupdating.entity.CloseableBody
import org.stansamples.filesupdating.entity.GetActualFilePartException
import org.stansamples.filesupdating.entity.PackageUpdate
import org.stansamples.filesupdating.entity.RangeNotSatisfiableException
import org.stansamples.filesupdating.entity.Secrets
import org.stansamples.filesupdating.entity.TooManyRequestsException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration.Companion.seconds

internal class FinalHeartbeats(
    private val contexts: Contexts,
    private val dirs: Dirs,
    private val remotes: Remotes,
    private val secrets: Secrets,
    loggers: Loggers,
) : Heartbeats {
    private val logger = loggers.create("[Heartbeats]")
    private val serialNumber = "foobarbaz" // todo
    private val _started = MutableStateFlow(false)
    override val started = _started.asStateFlow()
    val coroutineScope = CoroutineScope(contexts.main + SupervisorJob())

    init {
        coroutineScope.launch {
            withContext(contexts.default) {
                var job: Job? = null
                _started.drop(1).collect { newState ->
                    logger.debug("started: $newState")
                    if (newState) {
                        job = coroutineScope.launch {
                            withContext(contexts.default) {
                                while (_started.value) {
                                    try {
                                        heartbeat()
                                    } catch (error: Throwable) {
                                        logger.warning("heartbeat error: $error")
                                        _started.value = false
                                    }
                                    delay(8.seconds)
                                }
                            }
                        }
                    } else {
                        job?.cancelAndJoin()
                    }
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

    private fun write(body: CloseableBody, dst: File): Long {
        if (body.contentLength < 0) {
            error("update negative content length")
        }
        val read = FileOutputStream(dst, true).use { dst ->
            dst.write(src = body.src)
        }
        if (read != body.contentLength) {
            error("update wrong read length")
        }
        return body.contentLength
    }

    private fun download(id: String, dst: File) {
        var index = dst.length()
        val partSize = 64_000_000L
        while (true) {
            val response = try {
                remotes.getActualFilePart(serialNumber = serialNumber, id = id, range = LongRange(index, index + partSize - 1))
            } catch (error: Throwable) {
                throw GetActualFilePartException(cause = error)
            }
            val contentLength = response.body.use { body ->
                write(body = body, dst = dst)
            }
            index += contentLength
            val read = dst.length()
            if (read > response.fullLength) {
                error("update wrong full length")
            }
            if (read == response.fullLength) return
            if (contentLength == 0L) {
                error("update wrong content length")
            }
        }
    }

    private fun commit(tmp: File, target: File, sha256: ByteArray) {
        if (!tmp.inputStream().use(secrets::sha256).contentEquals(sha256)) {
            tmp.delete()
            error("update hash error")
        }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            error("update target error")
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
            if (error !is GetActualFilePartException || error.cause is RangeNotSatisfiableException) {
                tmp.delete()
            }
            return null
        } finally {
            runCatching {
                remotes.dequeue(serialNumber = serialNumber, id = pu.id)
            }.onFailure { error ->
                logger.warning("dequeue error: $error")
            }
        }
        commit(tmp = tmp, target = target, sha256 = pu.sha256)
        return target
    }

    private fun heartbeat() {
        logger.debug("heartbeat request...")
        val response = remotes.heartbeat(serialNumber = serialNumber)
        for (pu in response.needsPackagesUpdate) {
            logger.debug("updating package: $pu")
            val file = runCatching {
                updatePackage(pu = pu)
            }.onFailure { error ->
                logger.warning("updating package error: $error")
            }.getOrNull() ?: continue
            try {
                install(file = file)
            } catch (error: Throwable) {
                logger.warning("install package error: $error")
                continue
            }
        }
    }

    override fun start() {
        coroutineScope.launch {
            withContext(contexts.default) {
                if (!_started.value) {
                    logger.debug("starting")
                    _started.value = true
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            withContext(contexts.default) {
                if (_started.value) {
                    logger.debug("stopping")
                    _started.value = false
                }
            }
        }
    }
}
