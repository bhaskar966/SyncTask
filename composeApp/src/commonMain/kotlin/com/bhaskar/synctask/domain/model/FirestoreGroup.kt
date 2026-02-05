package com.bhaskar.synctask.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FirestoreGroup(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val icon: String = "",
    val colorHex: String = "",
    val order: Int = 0,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L
)