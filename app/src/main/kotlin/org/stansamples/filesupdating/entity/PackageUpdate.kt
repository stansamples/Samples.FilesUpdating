package org.stansamples.filesupdating.entity

internal class PackageUpdate(
    val id: String,
    val sha256: ByteArray,
) {
    override fun toString(): String {
        return "PackageUpdate($id)"
    }
}
