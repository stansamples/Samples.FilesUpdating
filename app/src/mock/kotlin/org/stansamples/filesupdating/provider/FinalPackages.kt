package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.Mocks
import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal class FinalPackages : Packages {
    override fun install(
        file: File,
        listener: Packages.InstallListener,
        expectedPackageName: String,
        expectedVersion: String,
    ): String {
        val expected = Mocks.getBytes(packageName = expectedPackageName, version = expectedVersion)
        val actual = file.readBytes()
        if (!expected.contentEquals(actual)) TODO("Wrong file!")
        listener.onStartInstall(packageName = expectedPackageName)
        Thread.sleep(1_000)
        file.inputStream().use { src ->
            val buffer = ByteArray(8_192)
            var read = 0L
            while (true) {
                val length = src.read(buffer)
                Thread.sleep(1_000)
                if (length < 0) break
                read += length
                listener.onProgress(packageName = expectedPackageName, contentLength = actual.size.toLong(), read = read)
            }
        }
        listener.onFinishInstall(packageName = expectedPackageName)
        Thread.sleep(1_000)
        return expectedPackageName
    }

    override fun getAppInfo(packageName: String): AppInfo {
        return Mocks.installed.firstOrNull { it.packageName == packageName } ?: TODO("No $packageName!")
    }
}
