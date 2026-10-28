package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.Mocks
import org.stansamples.filesupdating.entity.ConflictException
import org.stansamples.filesupdating.entity.GetActualFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatRequest
import org.stansamples.filesupdating.entity.HeartbeatResponse
import org.stansamples.filesupdating.entity.PackageUpdate

internal class FinalRemotes : Remotes {
    override fun heartbeat(request: HeartbeatRequest): HeartbeatResponse {
        Thread.sleep(1_000)
        if (!Mocks.remoteTerminals.contains(request.serialNumber)) throw ConflictException()
        val needsPackagesUpdate = mutableListOf<PackageUpdate>()
        for (pu in Mocks.needsPackagesUpdate) {
            val appInfo = request.apps.firstOrNull { it.packageName == pu.packageName }
            if (appInfo == null || pu.version != "${appInfo.versionName}-${appInfo.versionCode}") {
                needsPackagesUpdate.add(pu)
            }
        }
        return HeartbeatResponse(
            needsPackagesUpdate = needsPackagesUpdate,
        )
    }

    override fun dequeue(serialNumber: String, id: String) {
        TODO("FinalRemotes:dequeue($id)")
    }

    override fun getActualFilePart(serialNumber: String, id: String, range: LongRange): GetActualFilePartResponse {
        TODO("FinalRemotes:getActualFilePart($id, $range)")
    }
}
