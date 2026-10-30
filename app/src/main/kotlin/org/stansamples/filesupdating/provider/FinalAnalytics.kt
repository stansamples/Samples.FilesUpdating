package org.stansamples.filesupdating.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.stansamples.filesupdating.entity.LogEvent
import org.stansamples.filesupdating.util.hex
import org.stansamples.filesupdating.util.toBytes
import java.util.concurrent.atomic.AtomicInteger

internal class FinalAnalytics(
    private val times: Times,
    contexts: Contexts,
    locals: Locals,
    loggers: Loggers,
) : Analytics {
    private val coroutineScope = CoroutineScope(contexts.io + SupervisorJob())
    private val indices = AtomicInteger(locals.launched.inWholeSeconds.toInt())
    private val logger = loggers.create("[Analytics]")

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        payload: Map<String, String>,
    ) {
        val date = times.now()
        coroutineScope.launch {
            runCatching {
                val event = LogEvent(
                    id = indices.incrementAndGet().toBytes().hex(),
                    date = date,
                    type = type,
                    payload = payload,
                )
                logger.debug("event: ${event.type.name}")
                // todo
            }.onFailure { error ->
                logger.warning("report error: $error")
            }
        }
    }

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        error: Throwable,
    ) {
        val payload = mutableMapOf<String, String>()
        payload["error"] = error::class.java.name
        error.cause?.also {
            payload["cause"] = it::class.java.name
        }
        report(purpose = purpose, type = type, payload = payload)
    }
}
