package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.HeartbeatResponse

internal class FinalRemotes : Remotes {
    override fun heartbeat(): HeartbeatResponse {
        return HeartbeatResponse(
            needsPackagesUpdate = emptyList(), // todo
        )
    }

    override fun dequeue(id: String) {
        // todo
    }
}
