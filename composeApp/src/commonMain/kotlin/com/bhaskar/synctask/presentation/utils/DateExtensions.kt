package com.bhaskar.synctask.presentation.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime {
    return Instant.fromEpochMilliseconds(this.toEpochMilliseconds()).toLocalDateTime(timeZone)
}

fun LocalDate.atTime(time: LocalTime, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.atTime(time).toInstant(timeZone).toEpochMilliseconds()
}

fun LocalDate.atStartOfDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
}