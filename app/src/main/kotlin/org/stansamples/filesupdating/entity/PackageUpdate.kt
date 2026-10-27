package org.stansamples.filesupdating.entity

internal class PackageUpdate(
    val id: String,
    val packageName: String,
    val version: String,
    val sha256: ByteArray,
) {
    override fun toString(): String {
        return "PackageUpdate($id, $packageName, $version)"
    }
}
