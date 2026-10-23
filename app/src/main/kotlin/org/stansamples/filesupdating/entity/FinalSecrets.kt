package org.stansamples.filesupdating.entity

import java.io.InputStream
import java.security.MessageDigest

internal class FinalSecrets : Secrets {
    override fun sha256(src: InputStream): ByteArray {
        val md = MessageDigest.getInstance("sha256")
        val buffer = ByteArray(1 shl 15)
        while (true) {
            val read = src.read(buffer)
            if (read == -1) break
            md.update(buffer, 0, read)
        }
        return md.digest()
    }
}
