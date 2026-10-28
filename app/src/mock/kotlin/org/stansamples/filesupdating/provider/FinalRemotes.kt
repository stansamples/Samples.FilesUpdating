package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.Mocks
import org.stansamples.filesupdating.entity.CloseableBody
import org.stansamples.filesupdating.entity.ConflictException
import org.stansamples.filesupdating.entity.DequeueRequest
import org.stansamples.filesupdating.entity.GetFilePartRequest
import org.stansamples.filesupdating.entity.GetFilePartResponse
import org.stansamples.filesupdating.entity.HeartbeatRequest
import org.stansamples.filesupdating.entity.HeartbeatResponse
import org.stansamples.filesupdating.entity.PackageUpdate
import org.stansamples.filesupdating.util.write
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal class FinalRemotes : Remotes {
    override fun heartbeat(request: HeartbeatRequest): HeartbeatResponse {
        Thread.sleep(1_000)
        if (!Mocks.remoteTerminals.contains(request.serialNumber)) throw ConflictException()
        val needsPackagesUpdate = mutableListOf<PackageUpdate>()
        for (expected in Mocks.apps) {
            val actual = request.apps.firstOrNull { it.packageName == expected.packageName }
            if (expected != actual) {
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
        val appInfo = Mocks.apps.firstOrNull { Mocks.getPackageUpdate(it).id == request.id } ?: TODO("No ${request.id}!")
        val bytes = Mocks.getBytes(appInfo = appInfo)
        val partSize = request.range.last - request.range.first + 1
        val leftSize = bytes.size - request.range.first
        val filePart = ByteArrayInputStream(bytes).use { src ->
            ByteArrayOutputStream().use { dst ->
                dst.write(src = src, maxSize = kotlin.math.min(partSize, leftSize))
                dst.toByteArray()
            }
        }
        return GetFilePartResponse(
            body = object : CloseableBody {
                override val contentLength = filePart.size.toLong()
                override val src = filePart.inputStream()
                override fun close() = src.close()
            },
            fullLength = bytes.size.toLong(),
        )
    }
}
