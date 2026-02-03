package com.bhaskar.synctask.platform

import com.bhaskar.synctask.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface FirestoreDataSource {
    suspend fun saveReminder(reminder: Reminder)
    suspend fun deleteReminder(userId: String, reminderId: String)
    fun getReminders(userId: String): Flow<List<Reminder>>
}