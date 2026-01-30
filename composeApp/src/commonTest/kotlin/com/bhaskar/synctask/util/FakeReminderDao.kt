package com.bhaskar.synctask.util

import com.bhaskar.synctask.db.ReminderDao
import com.bhaskar.synctask.db.ReminderEntity
import com.bhaskar.synctask.db.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeReminderDao : ReminderDao {

    private val reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())

    override fun getAllReminders(userId: String): Flow<List<ReminderEntity>> {
        return reminders.map { list ->
            list.filter { it.userId == userId }.sortedBy { it.dueTime }
        }
    }

    override fun getActiveReminders(userId: String): Flow<List<ReminderEntity>> {
        return reminders.map { list ->
            list.filter { it.userId == userId && it.status == "ACTIVE" }
                .sortedBy { it.dueTime }
        }
    }

    override fun getReminderById(id: String): Flow<ReminderEntity?> {
        return reminders.map { list -> list.find { it.id == id } }
    }

    override suspend fun insertReminder(reminder: ReminderEntity) {
        val current = reminders.value.toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            current[index] = reminder // Update existing
        } else {
            current.add(reminder) // Insert new
        }
        reminders.value = current
    }

    override suspend fun deleteReminder(id: String) {
        reminders.value = reminders.value.filter { it.id != id }
    }

    override suspend fun updateSyncStatus(id: String, isSynced: Boolean) {
        val current = reminders.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(isSynced = isSynced)
            reminders.value = current
        }
    }

    override suspend fun getUnsyncedReminders(): List<ReminderEntity> {
        return reminders.value.filter { !it.isSynced }
    }

    override suspend fun addToQueue(item: SyncQueueEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun getPendingOperations(): List<SyncQueueEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun removeFromQueue(id: Long) {
        TODO("Not yet implemented")
    }

    // Test helper
    fun clear() {
        reminders.value = emptyList()
    }

    fun getAll(): List<ReminderEntity> = reminders.value
}