package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.HeartbeatResponse

internal interface Remotes {
    fun heartbeat(): HeartbeatResponse
    fun dequeue(id: String)
}
