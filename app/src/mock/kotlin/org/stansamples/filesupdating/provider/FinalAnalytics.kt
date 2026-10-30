package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.LogEvent

internal class FinalAnalytics(
    private val times: Times,
) : Analytics {
    private var _indices = 0

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        payload: Map<String, String>,
    ) {
        val event = LogEvent(
            id = _indices.inc().toString(),
            date = times.now(),
            type = type,
            payload = payload,
        )
        // todo
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
