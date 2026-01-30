package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class PlatformFirestoreDataSource : FirestoreDataSource {
    actual override suspend fun saveReminder(reminder: Reminder) {
        // TODO: Implement Firebase Firestore logic
        println("🔥 Android Firestore: Would save reminder ${reminder.id}")
    }

    actual override suspend fun deleteReminder(id: String) {
        // TODO: Implement Firebase Firestore logic
        println("🔥 Android Firestore: Would delete reminder $id")
    }

    actual override fun getReminders(userId: String): Flow<List<Reminder>> {
        // TODO: Implement Firebase Firestore logic
        println("🔥 Android Firestore: Would fetch reminders for $userId")
        return flowOf(emptyList())
    }
}