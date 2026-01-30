package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    SNOOZED,
    DISMISSED,
    MISSED
}