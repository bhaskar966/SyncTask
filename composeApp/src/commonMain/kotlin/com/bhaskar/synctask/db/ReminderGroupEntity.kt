package com.bhaskar.synctask.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ReminderGroup",
    indices = [
        Index("userId"),
        Index("order")
    ]
)
data class ReminderGroupEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val order: Int,
    val createdAt: Long,
    val lastModified: Long,
    val isSynced: Boolean
)