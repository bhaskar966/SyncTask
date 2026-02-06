package com.bhaskar.synctask.domain.repository

import com.bhaskar.synctask.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    // Local + Firestore combined
    fun getTags(userId: String): Flow<List<Tag>>

    // Create/Update
    suspend fun createTag(tag: Tag)
    suspend fun updateTag(tag: Tag)

    // Delete
    suspend fun deleteTag(userId: String, tagId: String)

    // Get single tag
    fun getTagById(tagId: String): Flow<Tag?>

    // Premium gate
    suspend fun getTagCount(userId: String): Int

    // Sync
    suspend fun syncTags(userId: String)

    suspend fun deleteAllLocalTags()
}