package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.platform.FirestoreDataSource
import kotlinx.coroutines.flow.Flow

expect class PlatformFirestoreDataSource : FirestoreDataSource {
    // Reminders
    override suspend fun saveReminder(reminder: Reminder)
    override suspend fun deleteReminder(userId: String, reminderId: String)
    override fun getReminders(userId: String): Flow<List<Reminder>>

    // Groups
    override suspend fun saveGroup(group: ReminderGroup)
    override suspend fun deleteGroup(userId: String, groupId: String)
    override fun getGroups(userId: String): Flow<List<ReminderGroup>>

    // Tags
    override suspend fun saveTag(tag: Tag)
    override suspend fun deleteTag(userId: String, tagId: String)
    override fun getTags(userId: String): Flow<List<Tag>>
}