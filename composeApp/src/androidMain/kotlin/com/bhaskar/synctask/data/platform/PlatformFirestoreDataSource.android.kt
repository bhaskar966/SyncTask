package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.data.mappers.toFirestoreMap
import com.bhaskar.synctask.data.mappers.toFirestoreReminder
import com.bhaskar.synctask.data.mappers.toReminder
import com.bhaskar.synctask.domain.model.FirestoreReminder
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

actual class PlatformFirestoreDataSource : FirestoreDataSource {

    private val firestore = Firebase.firestore

    actual override suspend fun saveReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        try {
            val path = "users/${reminder.userId}/reminders/${reminder.id}"
            println("🔥 Android Firestore: Saving to path: $path")
            println("🔥 Android Firestore: Saving reminder ${reminder.id}")
            firestore
                .collection("users")
                .document(reminder.userId)
                .collection("reminders")
                .document(reminder.id)
                .set(reminder.toFirestoreReminder())
            println("✅ Android Firestore: Saved successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Save failed - ${e.message}")
            throw e
        }
    }

    actual override suspend fun deleteReminder(userId: String, reminderId: String) = withContext(Dispatchers.IO) {
        try {
            println("🔥 Android Firestore: Deleting reminder $reminderId for user $userId")
            firestore
                .collection("users")
                .document(userId)
                .collection("reminders")
                .document(reminderId)
                .delete()
            println("✅ Android Firestore: Deleted successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Delete failed - ${e.message}")
            throw e
        }
    }

    actual override fun getReminders(userId: String): Flow<List<Reminder>> = callbackFlow {
        println("🔥 Android Firestore: Starting listener for user $userId")

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
                                e.printStackTrace()
                                null
                            }
                        }

                        println("🔥 Android Firestore: Received ${reminders.size} reminders")
                        trySend(reminders)
                    } catch (e: Exception) {
                        println("❌ Android Firestore: Listener error - ${e.message}")
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            println("❌ Android Firestore: Failed to start listener - ${e.message}")
            e.printStackTrace()
        }

        awaitClose {
            println("🔥 Android Firestore: Listener closed")
        }
    }.flowOn(Dispatchers.IO)
}