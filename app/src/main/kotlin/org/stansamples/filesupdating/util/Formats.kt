package org.stansamples.filesupdating.util

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun DateTimeFormatter.string(duration: Duration): String {
    val instant = Instant.ofEpochMilli(duration.inWholeMilliseconds)
    return format(instant)
}

internal fun DateTimeFormatter.duration(text: String): Duration {
    return LocalDate.parse(text, this)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
        .milliseconds
}

internal fun formats(pattern: String, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): DateTimeFormatter {
    return DateTimeFormatter.ofPattern(pattern, locale).withZone(timeZone.toZoneId()).withResolverStyle(ResolverStyle.STRICT)
}
