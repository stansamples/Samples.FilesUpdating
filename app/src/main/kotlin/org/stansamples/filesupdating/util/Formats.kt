package org.stansamples.filesupdating.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun dateFormat(pattern: String, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): DateFormat {
    val dateFormat = SimpleDateFormat(pattern, locale)
    dateFormat.timeZone = timeZone
    return dateFormat
}
