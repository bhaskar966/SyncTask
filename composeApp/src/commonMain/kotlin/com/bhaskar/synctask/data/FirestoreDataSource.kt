package com.bhaskar.synctask.data

import com.bhaskar.synctask.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

expect class FirestoreDataSource() {
    suspend fun saveReminder(reminder: Reminder)
    suspend fun deleteReminder(id: String)
    fun getReminders(userId: String): Flow<List<Reminder>>
}
