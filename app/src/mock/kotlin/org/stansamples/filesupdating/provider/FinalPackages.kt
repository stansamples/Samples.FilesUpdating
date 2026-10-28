package org.stansamples.filesupdating.provider

import org.stansamples.filesupdating.entity.AppInfo
import java.io.File

internal class FinalPackages : Packages {
    override fun install(
        file: File,
        listener: Packages.InstallListener,
    ): String {
        TODO("FinalPackages:install Not yet implemented")
    }

    override fun getAppInfo(packageName: String): AppInfo {
        TODO("FinalPackages:getAppInfo Not yet implemented")
    }
}
