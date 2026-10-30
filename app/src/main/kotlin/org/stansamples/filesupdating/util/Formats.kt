package org.stansamples.filesupdating.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration

internal fun Duration.toDate(): Date {
    return Date(inWholeMilliseconds)
}

internal fun DateFormat.fmt(duration: Duration): String {
    return format(duration.toDate())
}

internal fun dateFormat(pattern: String, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): DateFormat {
    val dateFormat = SimpleDateFormat(pattern, locale)
    dateFormat.timeZone = timeZone
    return dateFormat
}
