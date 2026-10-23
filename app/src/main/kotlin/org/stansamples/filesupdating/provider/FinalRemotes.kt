package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.GetActualFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatResponse

internal class FinalRemotes : Remotes {
    override fun heartbeat(serialNumber: String): HeartbeatResponse {
        return HeartbeatResponse(
            needsPackagesUpdate = emptyList(), // todo
        )
    }

    override fun dequeue(serialNumber: String, id: String) {
        TODO("FinalRemotes:dequeue($id)")
    }

    override fun getActualFilePart(serialNumber: String, id: String, range: LongRange): GetActualFilePartResponse {
        TODO("FinalRemotes:getActualFilePart($id, $range)")
    }
}
