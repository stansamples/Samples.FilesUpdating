package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.Mocks
import kotlin.time.Duration

internal class FinalLocals(
    override val launched: Duration,
) : Locals {
    override val dcTimeout: Duration? = Mocks.dcTimeout
    override val serialNumber: String? = Mocks.serialNumber
    override val ttl: Duration = Mocks.ttl
    override val filePartSize: Long = Mocks.filePartSize
    override val packageName: String = "org.stansamples.filesupdating.mock"
}
