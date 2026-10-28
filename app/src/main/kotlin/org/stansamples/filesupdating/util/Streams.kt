package org.stansamples.filesupdating.util

import java.io.InputStream
import java.io.OutputStream

internal fun OutputStream.write(src: InputStream, bufferSize: Int = 8_192): Long {
    val buffer = ByteArray(bufferSize)
    var read = 0L
    while (true) {
        val length = src.read(buffer)
        if (length < 0) break
        write(buffer, 0, length)
        read += length
    }
    flush()
    return read
}

internal fun OutputStream.write(src: InputStream, bufferSize: Int = 8_192, maxSize: Long): Long {
    val buffer = ByteArray(bufferSize)
    var read = 0L
    while (true) {
        val left = maxSize - read
        val length = if (buffer.size > left) {
            src.read(buffer)
        } else {
            src.read(buffer, 0, left.toInt())
        }
        if (length < 0) break
        write(buffer, 0, length)
        read += length
    }
    flush()
    return read
}
