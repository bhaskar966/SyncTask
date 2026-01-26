package com.bhaskar.synctask.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SyncQueueEntity")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: String,
    val operation: String,
    val timestamp: Long,
    val retryCount: Int = 0,
    val jsonPayload: String
)
