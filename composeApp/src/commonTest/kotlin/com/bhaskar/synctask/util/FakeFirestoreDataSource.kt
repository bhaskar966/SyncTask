package com.bhaskar.synctask.util

import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFirestoreDataSource : FirestoreDataSource {

    private val reminders = MutableStateFlow<Map<String, Reminder>>(emptyMap())
    var shouldThrowError = false

    override suspend fun saveReminder(reminder: Reminder) {
        if (shouldThrowError) {
            throw Exception("Network error")
        }
        val current = reminders.value.toMutableMap()
        current[reminder.id] = reminder
        reminders.value = current
        println("☁️ FakeFirestore: Saved ${reminder.id}")
    }

    override suspend fun deleteReminder(userId: String, reminderId: String) {
        if (shouldThrowError) {
            throw Exception("Network error")
        }
        val current = reminders.value.toMutableMap()
        current.remove(reminderId)
        reminders.value = current
        println("☁️ FakeFirestore: Deleted $reminderId")
    }

    override fun getReminders(userId: String): Flow<List<Reminder>> {
        return reminders.map { map ->
            if (shouldThrowError) {
                throw Exception("Network error")
            }
            map.values.filter { it.userId == userId }
        }
    }

    // Test helpers
    fun clear() {
        reminders.value = emptyMap()
        shouldThrowError = false
    }

    fun getAll() = reminders.value.values.toList()

    fun contains(id: String) = reminders.value.containsKey(id)
}