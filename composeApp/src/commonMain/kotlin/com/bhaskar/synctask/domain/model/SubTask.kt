package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SubTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val order: Int
)