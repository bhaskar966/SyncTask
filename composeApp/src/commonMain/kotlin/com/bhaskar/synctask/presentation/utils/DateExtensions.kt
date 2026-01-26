package com.bhaskar.synctask.presentation.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime {
    return kotlinx.datetime.Instant.fromEpochMilliseconds(this.toEpochMilliseconds()).toLocalDateTime(timeZone)
}
