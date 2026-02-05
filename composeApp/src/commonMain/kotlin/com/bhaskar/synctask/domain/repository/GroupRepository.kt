package com.bhaskar.synctask.domain.repository

import com.bhaskar.synctask.domain.model.ReminderGroup
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    // Local + Firestore combined
    fun getGroups(userId: String): Flow<List<ReminderGroup>>

    // Create/Update
    suspend fun createGroup(group: ReminderGroup)
    suspend fun updateGroup(group: ReminderGroup)

    // Delete
    suspend fun deleteGroup(userId: String, groupId: String)

    // Get single group
    fun getGroupById(groupId: String): Flow<ReminderGroup?>

    // Premium gate
    suspend fun getGroupCount(userId: String): Int

    // Sync
    suspend fun syncGroups(userId: String)

    suspend fun unassignRemindersFromGroup(groupId: String)
}