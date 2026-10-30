package org.stansamples.filesupdating.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.stansamples.filesupdating.entity.LogEvent
import org.stansamples.filesupdating.util.hex
import org.stansamples.filesupdating.util.toBytes
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

internal class FinalAnalytics(
    private val times: Times,
    contexts: Contexts,
    locals: Locals,
    loggers: Loggers,
) : Analytics {
    private val coroutineScope = CoroutineScope(contexts.io + SupervisorJob())
    private val indices = AtomicInteger(locals.launched.inWholeSeconds.toInt())
    private val logger = loggers.create("[Analytics]")

    private fun LogEvent.toJSONObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("date", date.inWholeMilliseconds)
        obj.put("type", type.name)
        if (payload.isNotEmpty()) {
            val entries = JSONObject()
            payload.forEach { (key, value) ->
                entries.put(key, value)
            }
            obj.put("payload", entries)
        }
        return obj
    }

    private fun check(purpose: Analytics.Purpose, date: Duration) {
        // todo
    }

    private fun report(purpose: Analytics.Purpose, event: LogEvent) {
        // todo
    }

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        payload: Map<String, String>,
    ) {
        val date = times.now()
        coroutineScope.launch {
            val event = LogEvent(
                id = indices.incrementAndGet().toBytes().hex(),
                date = date,
                type = type,
                payload = payload,
            )
            logger.debug("event: ${event.type.name}")
            runCatching {
                check(purpose = purpose, date = date)
                report(purpose = purpose, event = event)
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
