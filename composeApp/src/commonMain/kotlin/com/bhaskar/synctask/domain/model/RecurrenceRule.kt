package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RecurrenceRule {
    @Serializable
    data class Daily(val interval: Int = 1, val endDate: Long? = null) : RecurrenceRule()

    @Serializable
    data class Weekly(val interval: Int = 1, val daysOfWeek: List<Int>, val endDate: Long? = null) : RecurrenceRule()

    @Serializable
    data class Monthly(val interval: Int = 1, val dayOfMonth: Int, val endDate: Long? = null) : RecurrenceRule()

    @Serializable
    data class CustomDays(val interval: Int, val endDate: Long? = null) : RecurrenceRule()
}