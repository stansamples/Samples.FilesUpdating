package org.stansamples.filesupdating

import org.stansamples.filesupdating.entity.PackageUpdate
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object Mocks {
    val remoteTerminals: Set<String> = (0 until 10).mapTo(mutableSetOf()) { index ->
        String.format(Locale.US, "MOCK%08d", index)
    }
    val serialNumber = remoteTerminals.firstOrNull()
    val needsPackagesUpdate: List<PackageUpdate> = emptyList()
    val dcTimeout: Duration? = null
    val ttl: Duration = 4.seconds
}
