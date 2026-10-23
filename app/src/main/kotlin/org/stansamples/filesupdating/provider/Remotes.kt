package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.GetActualFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatResponse

internal interface Remotes {
    fun heartbeat(serialNumber: String): HeartbeatResponse
    fun dequeue(serialNumber: String, id: String)
    fun getActualFilePart(serialNumber: String, id: String, range: LongRange): GetActualFilePartResponse
}
