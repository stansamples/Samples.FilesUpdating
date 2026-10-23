package org.stansamples.filesupdating.provider

import kotlinx.coroutines.flow.StateFlow

internal interface Heartbeats {
    val started: StateFlow<Boolean>

    fun start()
    fun stop()
}
