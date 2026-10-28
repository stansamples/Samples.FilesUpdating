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
        val appInfo = Mocks.apps.firstOrNull { it.packageName == expectedPackageName } ?: TODO("No ${expectedPackageName}!")
        if (expectedVersion != "${appInfo.versionName}-${appInfo.versionCode}") TODO("Wrong version!")
        val expected = Mocks.getBytes(appInfo = appInfo)
        val actual = file.readBytes()
        if (!expected.contentEquals(actual)) TODO("Wrong file!")
        listener.onStartInstall(packageName = appInfo.packageName)
        Thread.sleep(1_000)
        file.inputStream().use { src ->
            val buffer = ByteArray(14)
            var read = 0L
            while (true) {
                val length = src.read(buffer)
                Thread.sleep(1_000)
                if (length < 0) break
                read += length
                listener.onProgress(packageName = appInfo.packageName, fullLength = actual.size.toLong(), read = read)
            }
        }
        for (i in Mocks.installed.indices) {
            val it = Mocks.installed[i]
            if (it.packageName == appInfo.packageName) {
                Mocks.installed.removeAt(i)
                break
            }
        }
        Mocks.installed += appInfo
        listener.onFinishInstall(packageName = appInfo.packageName)
        Thread.sleep(1_000)
        return appInfo.packageName
    }

    override fun getAppInfo(packageName: String): AppInfo {
        return Mocks.installed.firstOrNull { it.packageName == packageName } ?: TODO("No $packageName!")
    }
}
