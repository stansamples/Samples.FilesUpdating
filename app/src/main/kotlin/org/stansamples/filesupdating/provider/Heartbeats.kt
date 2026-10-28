package org.stansamples.filesupdating.provider

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

internal interface Heartbeats {
    sealed interface State {
        data object Updating : State
        data object Stopped : State
        data class DownloadingPackage(
            val packageName: String,
            val contentLength: Long,
            val read: Long,
        ) : State
        data class InstallingPackage(
            val packageName: String,
            val contentLength: Long,
            val read: Long,
        ) : State
        class Waiting(val timeUpdated: Duration?) : State {
            override fun toString(): String {
                return "Waiting(timeUpdated: ${timeUpdated?.inWholeMilliseconds})"
            }
        }
    }

    val started: StateFlow<Boolean>
    val states: StateFlow<State>

    fun start()
    fun stop()
}
