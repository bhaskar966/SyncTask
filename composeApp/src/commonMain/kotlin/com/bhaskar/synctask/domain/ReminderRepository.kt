package com.bhaskar.synctask.domain

import com.bhaskar.synctask.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    fun getReminderById(id: String): Flow<Reminder?>
    suspend fun createReminder(reminder: Reminder)
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(id: String)
    suspend fun completeReminder(id: String)
    suspend fun snoozeReminder(id: String, snoozeUntil: Long)
    suspend fun sync()
}
