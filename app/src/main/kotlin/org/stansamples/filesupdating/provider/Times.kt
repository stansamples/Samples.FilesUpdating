package org.stansamples.filesupdating.provider

import kotlin.time.Duration

internal interface Times {
    fun now(): Duration
    fun elapsed(timeStart: Duration): Duration
}
