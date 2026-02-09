package com.bhaskar.synctask.presentation.create.utils

object ReminderUtils {
    val REMINDER_OFFSETS = listOf(
        300000L to "5 min",
        600000L to "10 min",
        1800000L to "30 min",
        3600000L to "1 hr",
        7200000L to "2 hrs",
        86400000L to "1 day"
    )

    val COMMON_OFFSETS = REMINDER_OFFSETS.map { it.first }
}
