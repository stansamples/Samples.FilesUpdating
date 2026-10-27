package org.stansamples.filesupdating.provider

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

internal interface Heartbeats {
    sealed interface State {
        data object Updating : State
        data object Stopped : State
        data class DownloadingFile(
            val contentLength: Long,
            val read: Long,
        ) : State
        data object InstallingPackage : State
        class Waiting(val updated: Duration?) : State {
            override fun toString(): String {
                return "Waiting(updated: ${updated?.inWholeMilliseconds})"
            }
        }
    }

    val started: StateFlow<Boolean>
    val states: StateFlow<State>

    fun start()
    fun stop()
}
