package org.stansamples.filesupdating

import org.stansamples.filesupdating.entity.AppInfo
import org.stansamples.filesupdating.entity.PackageUpdate
import java.security.MessageDigest
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object Mocks {
    private val sha256 = MessageDigest.getInstance("sha256")
    val terminals: Set<String> = (0 until 16).mapTo(mutableSetOf()) { index ->
        String.format(Locale.US, "SERIAL%08d", index)
    }
    val serialNumber = terminals.firstOrNull()
    val apps = (0 until 16).map { index ->
        val packageName = String.format(Locale.US, "PACKAGE%08d", index)
        AppInfo(
            packageName = packageName,
            versionName = "0.0.$index",
            versionCode = index.toLong(),
        )
    }
    val installed = mutableListOf<AppInfo>()
    val dcTimeout: Duration? = null
    val ttl: Duration = 6.seconds
    val filePartSize: Long = 4

    fun getBytes(appInfo: AppInfo): ByteArray {
        return sha256.digest("${appInfo.packageName}-${appInfo.versionName}-${appInfo.versionCode}".toByteArray())
    }

    fun getPackageUpdate(appInfo: AppInfo): PackageUpdate {
        val id = "${appInfo.packageName}-${appInfo.versionName}-${appInfo.versionCode}"
        val bytes = getBytes(appInfo = appInfo)
        return PackageUpdate(
            id = id,
            packageName = appInfo.packageName,
            version = "${appInfo.versionName}-${appInfo.versionCode}",
            sha256 = sha256.digest(bytes),
        )
    }
}
