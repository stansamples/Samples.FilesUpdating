package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.Mocks
import org.stansamples.filesupdating.entity.ConflictException
import org.stansamples.filesupdating.entity.DequeueRequest
import org.stansamples.filesupdating.entity.GetFilePartRequest
import org.stansamples.filesupdating.entity.GetFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatRequest
import org.stansamples.filesupdating.entity.HeartbeatResponse
import org.stansamples.filesupdating.entity.PackageUpdate

internal class FinalRemotes : Remotes {
    override fun heartbeat(request: HeartbeatRequest): HeartbeatResponse {
        Thread.sleep(1_000)
        if (!Mocks.remoteTerminals.contains(request.serialNumber)) throw ConflictException()
        val needsPackagesUpdate = mutableListOf<PackageUpdate>()
        for (expected in Mocks.apps) {
            val actual = request.apps.firstOrNull { it.packageName == expected.packageName }
            if (expected == actual) {
                needsPackagesUpdate.add(Mocks.getPackageUpdate(appInfo = expected))
            }
        }
        return HeartbeatResponse(
            needsPackagesUpdate = needsPackagesUpdate,
        )
    }

    override fun dequeue(request: DequeueRequest) {
        Thread.sleep(1_000)
        if (!Mocks.remoteTerminals.contains(request.serialNumber)) throw ConflictException()
        // noop
    }

    override fun getFilePart(request: GetFilePartRequest): GetFilePartResponse {
        Thread.sleep(1_000)
        if (!Mocks.remoteTerminals.contains(request.serialNumber)) throw ConflictException()
        TODO("FinalRemotes:getActualFilePart(${request.serialNumber}, ${request.id}, ${request.range})")
    }
}
