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
import org.stansamples.filesupdating.entity.HeartbeatResponse
import org.stansamples.filesupdating.entity.PackageUpdate
import kotlin.time.Duration.Companion.seconds

internal class FinalHeartbeats(
    private val contexts: Contexts,
    private val dirs: Dirs,
    loggers: Loggers,
) : Heartbeats {
    private val logger = loggers.create("[Heartbeats]")
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

    private fun request(): HeartbeatResponse {
        return HeartbeatResponse(
            needsPackagesUpdate = emptyList(), // todo
        )
    }

    private fun updatePackage(pu: PackageUpdate) {
        val dir = dirs.cache.resolve("packages")
        val target = dir.resolve("target.apk")
        if (target.exists() && !target.delete()) {
            error("update ${pu.packageName} cannot delete old target")
        }
        val file = dir.resolve("${pu.id}.apk")
        TODO("updatePackage($pu)")
    }

    private fun heartbeat() {
        logger.debug("heartbeat request...")
        val response = request()
        for (pu in response.needsPackagesUpdate) {
            logger.debug("updating package: $pu")
            try {
                updatePackage(pu = pu)
            } catch (error: Throwable) {
                logger.warning("updating package $pu error: $error")
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
