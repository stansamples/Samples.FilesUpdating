package org.stansamples.filesupdating.util

import java.util.Locale

internal fun Byte.hex(locale: Locale = Locale.US): String {
    return String.format(locale, "%02x", toInt().and(0xff))
}

internal fun ByteArray.hex(locale: Locale = Locale.US): String {
    if (isEmpty()) return ""
    val builder = StringBuilder(size * 2)
    builder.append(get(0).hex(locale))
    for (i in 1 until size) {
        builder.append(get(i).hex(locale))
    }
    return builder.toString()
}

internal fun ByteArray.write(index: Int = 0, value: Int) {
    set(index, value.shr(24).toByte())
    set(index + 1, value.shr(16).toByte())
    set(index + 2, value.shr(8).toByte())
    set(index + 3, value.toByte())
}

internal fun Int.toBytes(): ByteArray {
    val bytes = ByteArray(4)
    bytes.write(value = this)
    return bytes
}
