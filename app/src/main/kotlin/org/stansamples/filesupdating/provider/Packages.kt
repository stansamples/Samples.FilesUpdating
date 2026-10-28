package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal interface Packages {
    interface InstallListener {
        fun onProgress(packageName: String, contentLength: Long, read: Long)
        fun onStartInstall(packageName: String)
        fun onFinishInstall(packageName: String)
    }

    fun install(
        file: File,
        listener: InstallListener,
    ): String
    fun getAppInfo(packageName: String): AppInfo
}
