package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import kotlinx.coroutines.flow.Flow

expect class PlatformFirestoreDataSource: FirestoreDataSource {
    override suspend fun saveReminder(reminder: Reminder)
    override suspend fun deleteReminder(id: String)
    override fun getReminders(userId: String): Flow<List<Reminder>>
}