package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: String,
    val userId: String,
    val name: String,
    val colorHex: String? = null,
    val createdAt: Long,
    val isSynced: Boolean = false
)