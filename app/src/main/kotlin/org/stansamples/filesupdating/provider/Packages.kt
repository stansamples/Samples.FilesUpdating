package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal interface Packages {
    fun install(file: File): String
    fun getAppInfo(packageName: String): AppInfo
}
