package org.stansamples.filesupdating.provider

import kotlinx.coroutines.flow.StateFlow
import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal interface Packages {
    interface InstallListener {
        fun onProgress(packageName: String, fullLength: Long, read: Long)
        fun onStartInstall(packageName: String)
        fun onFinishInstall(packageName: String)
    }

    suspend fun install(
        file: File,
        started: StateFlow<Boolean>,
        listener: InstallListener,
        expectedPackageName: String,
        expectedVersion: String,
    ): String
    fun getAppInfo(packageName: String): AppInfo
}
