package org.stansamples.filesupdating.provider

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class FinalTimes : Times {
    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun elapsed(timeStart: Duration): Duration {
        return System.currentTimeMillis().milliseconds - timeStart
    }
}
