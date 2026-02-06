package com.bhaskar.synctask.domain.repository

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
    suspend fun snoozeReminder(id: String, snoozeMinutes: Int)
    suspend fun sync()
    suspend fun dismissReminder(id: String)
    suspend fun rescheduleReminder(id: String, newDueTime: Long, newReminderTime: Long?)
    fun getRemindersByGroup(userId: String, groupId: String): Flow<List<Reminder>>
    fun getUngroupedReminders(userId: String): Flow<List<Reminder>>
    suspend fun getReminderCountByGroup(groupId: String): Int
    suspend fun getActiveReminderCount(userId: String): Int
    suspend fun getPinnedReminderCount(userId: String): Int
    suspend fun deleteAllLocalReminders()
}