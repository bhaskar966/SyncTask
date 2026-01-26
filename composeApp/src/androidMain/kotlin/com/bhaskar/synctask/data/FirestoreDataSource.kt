package com.bhaskar.synctask.data

import com.bhaskar.synctask.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class FirestoreDataSource {
    actual suspend fun saveReminder(reminder: Reminder) {
        // TODO: Implement Firebase Firestore logic
    }

    actual suspend fun deleteReminder(id: String) {
        // TODO: Implement Firebase Firestore logic
    }

    actual fun getReminders(userId: String): Flow<List<Reminder>> {
        // TODO: Implement Firebase Firestore logic
        return flowOf(emptyList())
    }
}
