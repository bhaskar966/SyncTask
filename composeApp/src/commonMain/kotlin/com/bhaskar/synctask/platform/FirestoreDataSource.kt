package com.bhaskar.synctask.platform

import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface FirestoreDataSource {
    // Reminders
    suspend fun saveReminder(reminder: Reminder)
    suspend fun deleteReminder(userId: String, reminderId: String)
    fun getReminders(userId: String): Flow<List<Reminder>>

    // Groups
    suspend fun saveGroup(group: ReminderGroup)
    suspend fun deleteGroup(userId: String, groupId: String)
    fun getGroups(userId: String): Flow<List<ReminderGroup>>

    // Tags
    suspend fun saveTag(tag: Tag)
    suspend fun deleteTag(userId: String, tagId: String)
    fun getTags(userId: String): Flow<List<Tag>>
}