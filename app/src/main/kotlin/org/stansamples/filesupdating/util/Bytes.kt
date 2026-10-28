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
