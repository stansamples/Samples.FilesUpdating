package org.stansamples.filesupdating.entity

import kotlin.time.Duration

internal data class LogEvent(
    val id: String,
    val date: Duration,
    val type: Type,
    val payload: Map<String, String>,
) {
    enum class Type {
        DisconnectTimeout,
        DownloadPackageError,
        HeartbeatRequestError,
        HeartbeatsSuccessAfterFailure,
        HeartbeatsUnknownError,
        InstallPackageError,
        StartHeartbeat,
        StopHeartbeat,
        UpdatingPackage,
    }
}
