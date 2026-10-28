package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.DequeueRequest
import org.stansamples.filesupdating.entity.GetFilePartRequest
import org.stansamples.filesupdating.entity.GetFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatRequest
import org.stansamples.filesupdating.entity.HeartbeatResponse

internal interface Remotes {
    fun heartbeat(request: HeartbeatRequest): HeartbeatResponse
    fun dequeue(request: DequeueRequest)
    fun getFilePart(request: GetFilePartRequest): GetFilePartResponse
}
