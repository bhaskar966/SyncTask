package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RecurrenceRule {
    abstract val interval: Int
    abstract val endDate: Long?
    abstract val occurrenceCount: Int?
    abstract val fromCompletion: Boolean

    @Serializable
    data class Daily(
        override val interval: Int = 1,
        override val endDate: Long? = null,
        override val occurrenceCount: Int? = null,
        override val fromCompletion: Boolean = false
    ) : RecurrenceRule()

    @Serializable
    data class Weekly(
        override val interval: Int = 1,
        val daysOfWeek: List<Int>,
        override val endDate: Long? = null,
        override val occurrenceCount: Int? = null,
        override val fromCompletion: Boolean = false
    ) : RecurrenceRule()

    @Serializable
    data class Monthly(
        override val interval: Int = 1,
        val dayOfMonth: Int,
        override val endDate: Long? = null,
        override val occurrenceCount: Int? = null,
        override val fromCompletion: Boolean = false
    ) : RecurrenceRule()

    @Serializable
    data class Yearly(
        override val interval: Int = 1,
        val month: Int,
        val dayOfMonth: Int,
        override val endDate: Long? = null,
        override val occurrenceCount: Int? = null,
        override val fromCompletion: Boolean = false
    ) : RecurrenceRule()

    @Serializable
    data class CustomDays(
        override val interval: Int,
        override val endDate: Long? = null,
        override val occurrenceCount: Int? = null,
        override val fromCompletion: Boolean = false
    ) : RecurrenceRule()
}