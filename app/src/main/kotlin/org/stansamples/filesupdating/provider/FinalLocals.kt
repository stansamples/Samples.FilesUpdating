package org.stansamples.filesupdating.provider

import kotlin.time.Duration

internal class FinalLocals(
    override val dcTimeout: Duration?,
    override val serialNumber: String,
    override val ttl: Duration,
) : Locals
