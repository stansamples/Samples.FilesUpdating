package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.GetActualFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatResponse

internal interface Remotes {
    fun heartbeat(): HeartbeatResponse
    fun dequeue(id: String)
    fun getActualFilePart(id: String, range: LongRange): GetActualFilePartResponse
}
