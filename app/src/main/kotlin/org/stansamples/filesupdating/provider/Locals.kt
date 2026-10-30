package org.stansamples.filesupdating.provider

import kotlin.time.Duration

internal interface Locals {
    val dcTimeout: Duration?
    val serialNumber: String?
    val ttl: Duration
    val filePartSize: Long
    val launched: Duration
}
