package org.stansamples.filesupdating

import org.stansamples.filesupdating.entity.AppInfo
import org.stansamples.filesupdating.entity.PackageUpdate
import java.security.MessageDigest
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object Mocks {
    private val sha256 = MessageDigest.getInstance("sha256")
    val remoteTerminals: Set<String> = (0 until 16).mapTo(mutableSetOf()) { index ->
        String.format(Locale.US, "SERIAL%08d", index)
    }
    val serialNumber = remoteTerminals.firstOrNull()
    val apps = (0 until 16).map { index ->
        val packageName = String.format(Locale.US, "PACKAGE%08d", index)
        AppInfo(
            packageName = packageName,
            versionName = "0.0.$index",
            versionCode = index.toLong(),
        )
    }
    val dcTimeout: Duration? = null
    val ttl: Duration = 8.seconds
    val filePartSize: Long = 4

    fun getBytes(appInfo: AppInfo): ByteArray {
        val id = "${appInfo.packageName}-${appInfo.versionName}-${appInfo.versionCode}"
        return sha256.digest(id.toByteArray())
    }

    fun getPackageUpdate(appInfo: AppInfo): PackageUpdate {
        val id = "${appInfo.packageName}-${appInfo.versionName}-${appInfo.versionCode}"
        return PackageUpdate(
            id = id,
            packageName = appInfo.packageName,
            version = "${appInfo.versionName}-${appInfo.versionCode}",
            sha256 = sha256.digest(getBytes(appInfo = appInfo)),
        )
    }
}
