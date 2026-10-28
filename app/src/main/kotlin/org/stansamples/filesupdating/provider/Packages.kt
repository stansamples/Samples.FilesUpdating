package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal interface Packages {
    interface InstallListener {
        fun onProgress(packageName: String, fullLength: Long, read: Long)
        fun onStartInstall(packageName: String)
        fun onFinishInstall(packageName: String)
    }

    fun install(
        file: File,
        listener: InstallListener,
        expectedPackageName: String,
        expectedVersion: String,
    ): String
    fun getAppInfo(packageName: String): AppInfo
}
