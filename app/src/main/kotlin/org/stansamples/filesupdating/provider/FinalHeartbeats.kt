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
import kotlin.time.Duration.Companion.seconds

internal class FinalHeartbeats(
    private val contexts: Contexts,
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

    private fun heartbeat() {
        logger.debug("heartbeat request...")
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
