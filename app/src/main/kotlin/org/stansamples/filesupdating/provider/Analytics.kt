package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.LogEvent

internal interface Analytics {
    enum class Purpose {
        Events,
    }

    val events: List<LogEvent>

    fun report(purpose: Purpose, type: LogEvent.Type, payload: Map<String, String> = emptyMap())
    fun report(purpose: Purpose, type: LogEvent.Type, error: Throwable)
}
