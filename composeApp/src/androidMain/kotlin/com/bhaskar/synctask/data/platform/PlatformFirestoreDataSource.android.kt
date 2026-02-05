package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.data.mappers.toFirestoreGroup
import com.bhaskar.synctask.data.mappers.toFirestoreMap
import com.bhaskar.synctask.data.mappers.toFirestoreReminder
import com.bhaskar.synctask.data.mappers.toFirestoreTag
import com.bhaskar.synctask.data.mappers.toGroup
import com.bhaskar.synctask.data.mappers.toReminder
import com.bhaskar.synctask.data.mappers.toTag
import com.bhaskar.synctask.domain.model.FirestoreGroup
import com.bhaskar.synctask.domain.model.FirestoreReminder
import com.bhaskar.synctask.domain.model.FirestoreTag
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.model.Tag
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

    // REMINDERS
    actual override suspend fun saveReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        try {
            val path = "users/${reminder.userId}/reminders/${reminder.id}"
            println("🔥 Android Firestore: Saving reminder to path: $path")

            firestore
                .collection("users")
                .document(reminder.userId)
                .collection("reminders")
                .document(reminder.id)
                .set(reminder.toFirestoreReminder())

            println("✅ Android Firestore: Reminder saved successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Save reminder failed - ${e.message}")
            e.printStackTrace()
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

            println("✅ Android Firestore: Reminder deleted successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Delete reminder failed - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual override fun getReminders(userId: String): Flow<List<Reminder>> = callbackFlow {
        println("🔥 Android Firestore: Starting reminders listener for user $userId")

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
                        println("❌ Android Firestore: Reminders listener error - ${e.message}")
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            println("❌ Android Firestore: Failed to start reminders listener - ${e.message}")
            e.printStackTrace()
        }

        awaitClose {
            println("🔥 Android Firestore: Reminders listener closed")
        }
    }.flowOn(Dispatchers.IO)


    // GROUPS
    actual override suspend fun saveGroup(group: ReminderGroup) = withContext(Dispatchers.IO) {
        try {
            val path = "users/${group.userId}/groups/${group.id}"
            println("🔥 Android Firestore: Saving group to path: $path")

            firestore
                .collection("users")
                .document(group.userId)
                .collection("groups")
                .document(group.id)
                .set(group.toFirestoreGroup())

            println("✅ Android Firestore: Group saved successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Save group failed - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual override suspend fun deleteGroup(userId: String, groupId: String) = withContext(Dispatchers.IO) {
        try {
            println("🔥 Android Firestore: Deleting group $groupId for user $userId")

            firestore
                .collection("users")
                .document(userId)
                .collection("groups")
                .document(groupId)
                .delete()

            println("✅ Android Firestore: Group deleted successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Delete group failed - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual override fun getGroups(userId: String): Flow<List<ReminderGroup>> = callbackFlow {
        println("🔥 Android Firestore: Starting groups listener for user $userId")

        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("groups")
                .snapshots
                .collect { snapshot ->
                    try {
                        val groups = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<FirestoreGroup>().toGroup()
                            } catch (e: Exception) {
                                println("❌ Failed to parse group ${doc.id}: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        }
                        println("🔥 Android Firestore: Received ${groups.size} groups")
                        trySend(groups)
                    } catch (e: Exception) {
                        println("❌ Android Firestore: Groups listener error - ${e.message}")
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            println("❌ Android Firestore: Failed to start groups listener - ${e.message}")
            e.printStackTrace()
        }

        awaitClose {
            println("🔥 Android Firestore: Groups listener closed")
        }
    }.flowOn(Dispatchers.IO)


    // TAGS
    actual override suspend fun saveTag(tag: Tag) = withContext(Dispatchers.IO) {
        try {
            val path = "users/${tag.userId}/tags/${tag.id}"
            println("🔥 Android Firestore: Saving tag to path: $path")

            firestore
                .collection("users")
                .document(tag.userId)
                .collection("tags")
                .document(tag.id)
                .set(tag.toFirestoreTag())

            println("✅ Android Firestore: Tag saved successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Save tag failed - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual override suspend fun deleteTag(userId: String, tagId: String) = withContext(Dispatchers.IO) {
        try {
            println("🔥 Android Firestore: Deleting tag $tagId for user $userId")

            firestore
                .collection("users")
                .document(userId)
                .collection("tags")
                .document(tagId)
                .delete()

            println("✅ Android Firestore: Tag deleted successfully")
        } catch (e: Exception) {
            println("❌ Android Firestore: Delete tag failed - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual override fun getTags(userId: String): Flow<List<Tag>> = callbackFlow {
        println("🔥 Android Firestore: Starting tags listener for user $userId")

        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("tags")
                .snapshots
                .collect { snapshot ->
                    try {
                        val tags = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<FirestoreTag>().toTag()
                            } catch (e: Exception) {
                                println("❌ Failed to parse tag ${doc.id}: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        }
                        println("🔥 Android Firestore: Received ${tags.size} tags")
                        trySend(tags)
                    } catch (e: Exception) {
                        println("❌ Android Firestore: Tags listener error - ${e.message}")
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            println("❌ Android Firestore: Failed to start tags listener - ${e.message}")
            e.printStackTrace()
        }

        awaitClose {
            println("🔥 Android Firestore: Tags listener closed")
        }
    }.flowOn(Dispatchers.IO)
}