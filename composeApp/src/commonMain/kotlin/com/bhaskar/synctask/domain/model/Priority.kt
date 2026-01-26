package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    LOW, MEDIUM, HIGH
}