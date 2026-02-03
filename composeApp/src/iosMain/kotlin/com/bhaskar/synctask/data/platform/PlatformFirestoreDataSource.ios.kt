package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.data.mappers.toFirestoreMap
import com.bhaskar.synctask.data.mappers.toFirestoreReminder
import com.bhaskar.synctask.data.mappers.toReminder
import com.bhaskar.synctask.domain.model.FirestoreReminder
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.collections.mapNotNull

actual class PlatformFirestoreDataSource : FirestoreDataSource {

    private val firestore = Firebase.firestore

    actual override suspend fun saveReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        try {
            println("🔥 iOS Firestore: Saving reminder ${reminder.id}")
            firestore
                .collection("users")
                .document(reminder.userId)
                .collection("reminders")
                .document(reminder.id)
                .set(reminder.toFirestoreReminder())
            println("✅ iOS Firestore: Saved successfully")
        } catch (e: Exception) {
            println("❌ iOS Firestore: Save failed - ${e.message}")
            throw e
        }
    }

    actual override suspend fun deleteReminder(userId: String, reminderId: String) = withContext(Dispatchers.IO) {
        try {
            println("🔥 iOS Firestore: Deleting reminder $reminderId for user $userId")
            firestore
                .collection("users")
                .document(userId)
                .collection("reminders")
                .document(reminderId)
                .delete()
            println("✅ iOS Firestore: Deleted successfully")
        } catch (e: Exception) {
            println("❌ iOS Firestore: Delete failed - ${e.message}")
            throw e
        }
    }

    actual override fun getReminders(userId: String): Flow<List<Reminder>> = callbackFlow {
        println("🔥 iOS Firestore: Starting listener for user $userId")

        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("reminders")
                .snapshots
                .collect { snapshot ->
                    try {
                        val reminders = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<FirestoreReminder>().toReminder()
                            } catch (e: Exception) {
                                println("❌ Failed to parse reminder ${doc.id}: ${e.message}")
                                null
                            }
                        }

                        println("🔥 iOS Firestore: Received ${reminders.size} reminders")
                        trySend(reminders)
                    } catch (e: Exception) {
                        println("❌ iOS Firestore: Listener error - ${e.message}")
                    }
                }
        } catch (e: Exception) {
            println("❌ iOS Firestore: Failed to start listener - ${e.message}")
        }

        awaitClose {
            println("🔥 iOS Firestore: Listener closed")
        }
    }.flowOn(Dispatchers.IO)
}
