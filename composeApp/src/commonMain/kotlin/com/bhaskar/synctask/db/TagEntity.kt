package com.bhaskar.synctask.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Tag",
    indices = [Index("userId")]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val colorHex: String?,
    val createdAt: Long,
    val isSynced: Boolean
)