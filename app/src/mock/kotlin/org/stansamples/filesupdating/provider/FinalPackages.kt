package org.stansamples.filesupdating.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.stansamples.filesupdating.Mocks
import org.stansamples.filesupdating.entity.AppInfo
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class FinalPackages : Packages {
    override suspend fun install(
        file: File,
        started: StateFlow<Boolean>,
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
        delay(1.seconds)
        file.inputStream().use { src ->
            val buffer = ByteArray(14)
            var read = 0L
            while (true) {
                if (!started.value) error("install ${appInfo.packageName} stopped")
                val length = src.read(buffer)
                delay(1.seconds)
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
        delay(1.seconds)
        return appInfo.packageName
    }

    override fun getAppInfo(packageName: String): AppInfo {
        return Mocks.installed.firstOrNull { it.packageName == packageName } ?: TODO("No $packageName!")
    }
}
