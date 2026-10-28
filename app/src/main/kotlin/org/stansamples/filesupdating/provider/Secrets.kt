package org.stansamples.filesupdating.provider

import java.io.InputStream

internal interface Secrets {
    fun sha256(encoded: ByteArray): ByteArray
    fun sha256(src: InputStream): ByteArray
}
