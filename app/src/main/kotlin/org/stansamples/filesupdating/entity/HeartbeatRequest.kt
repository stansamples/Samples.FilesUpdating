package org.stansamples.filesupdating.entity

internal data class HeartbeatRequest(
    val serialNumber: String,
    val apps: List<AppInfo>,
)
