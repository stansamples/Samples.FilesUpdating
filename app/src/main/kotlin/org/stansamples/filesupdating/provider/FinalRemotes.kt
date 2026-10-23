package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.GetActualFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatResponse

internal class FinalRemotes : Remotes {
    override fun heartbeat(): HeartbeatResponse {
        return HeartbeatResponse(
            needsPackagesUpdate = emptyList(), // todo
        )
    }

    override fun dequeue(id: String) {
        TODO("FinalRemotes:dequeue($id)")
    }

    override fun getActualFilePart(id: String, range: LongRange): GetActualFilePartResponse {
        TODO("FinalRemotes:getActualFilePart($id, $range)")
    }
}
