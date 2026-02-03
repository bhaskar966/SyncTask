package com.bhaskar.synctask.data.sync

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.data.toDomain
import com.bhaskar.synctask.data.toEntity
import com.bhaskar.synctask.db.ReminderDao
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.platform.FirestoreDataSource
import com.bhaskar.synctask.platform.NotificationScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SyncService(
    private val firestoreDataSource: FirestoreDataSource,
    private val dao: ReminderDao,
    private val notificationScheduler: NotificationScheduler,
    private val authManager: AuthManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var realtimeSyncJob: Job? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        // Auto-start sync when authenticated
        scope.launch {
            authManager.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        println("🔄 SyncService: User authenticated, starting sync")
                        startRealtimeSync()
                    }
                    is AuthState.Unauthenticated -> {
                        println("⏸️ SyncService: User logged out, stopping sync")
                        stopRealtimeSync()
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Start real-time sync listener
     */
    fun startRealtimeSync() {
        if (realtimeSyncJob?.isActive == true) {
            println("⚠️ SyncService: Realtime sync already running")
            return
        }

        val userId = authManager.currentUserId
        if (userId == null) {
            println("❌ SyncService: Cannot start sync - no user ID")
            return
        }

        println("🚀 SyncService: Starting realtime sync for user: $userId")
        _syncState.value = SyncState.Syncing

        realtimeSyncJob = scope.launch {
            try {
                // Listen to Firestore changes in real-time
                firestoreDataSource.getReminders(userId).collect { firestoreReminders ->
                    println("📥 SyncService: Received ${firestoreReminders.size} reminders from Firestore")
                    syncRemindersFromFirestore(firestoreReminders)
                    _syncState.value = SyncState.Synced
                }
            } catch (e: Exception) {
                println("❌ SyncService: Realtime sync error - ${e.message}")
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Stop real-time sync listener
     */
    fun stopRealtimeSync() {
        println("⏹️ SyncService: Stopping realtime sync")
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
        _syncState.value = SyncState.Idle
    }

    /**
     * Sync reminders from Firestore to local database
     */
    private suspend fun syncRemindersFromFirestore(firestoreReminders: List<Reminder>) {
        try {
            val userId = authManager.currentUserId ?: return

            // Get local reminders for this user
            val localReminders = dao.getAllReminders(userId).first()
                .map { it.toDomain() }

            val localMap = localReminders.associateBy { it.id }

            for (firestoreReminder in firestoreReminders) {
                val localReminder = localMap[firestoreReminder.id]

                when {
                    // New reminder from Firestore
                    localReminder == null -> {
                        println("➕ SyncService: Adding new reminder: ${firestoreReminder.title}")
                        dao.insertReminder(firestoreReminder.toEntity())
                    }

                    // Firestore version is newer
                    firestoreReminder.lastModified > localReminder.lastModified -> {
                        println("🔄 SyncService: Updating reminder: ${firestoreReminder.title}")
                        dao.insertReminder(firestoreReminder.toEntity())
                    }

                    // Local version is newer - push to Firestore
                    localReminder.lastModified > firestoreReminder.lastModified -> {
                        println("⬆️ SyncService: Local version newer, pushing to Firestore: ${localReminder.title}")
                        pushReminderToFirestore(localReminder)
                    }

                    // Same version - do nothing
                    else -> {
                        // Already in sync
                    }
                }
            }

            // Check for reminders that exist locally but not in Firestore
            val firestoreIds = firestoreReminders.map { it.id }.toSet()
            val localOnlyReminders = localReminders.filter { it.id !in firestoreIds }

            for (localReminder in localOnlyReminders) {
                println("⬆️ SyncService: Pushing local-only reminder to Firestore: ${localReminder.title}")
                pushReminderToFirestore(localReminder)
            }

            // Reschedule notifications after sync
            withContext(Dispatchers.Main) {
                notificationScheduler.scheduleNext()
            }

        } catch (e: Exception) {
            println("❌ SyncService: Error syncing reminders - ${e.message}")
            throw e
        }
    }

    /**
     * Push a reminder to Firestore
     */
    suspend fun pushReminderToFirestore(reminder: Reminder) {
        try {
            println("⬆️ SyncService: Pushing reminder to Firestore: ${reminder.title}")
            firestoreDataSource.saveReminder(reminder)

            // Mark as synced in local DB
            dao.updateSyncStatus(reminder.id, true)
        } catch (e: Exception) {
            println("❌ SyncService: Failed to push reminder - ${e.message}")
            throw e
        }
    }

    /**
     * Delete a reminder from Firestore
     */
    suspend fun deleteReminderFromFirestore(reminderId: String) {
        val userId = authManager.currentUserId
        if (userId == null) {
            println("❌ SyncService: Cannot delete - no user ID")
            return
        }

        try {
            println("🗑️ SyncService: Deleting reminder from Firestore: $reminderId")
            firestoreDataSource.deleteReminder(userId, reminderId)
        } catch (e: Exception) {
            println("❌ SyncService: Failed to delete reminder - ${e.message}")
            throw e
        }
    }

    /**
     * Force a full sync - push all unsynced local reminders
     */
    suspend fun forceSync() {
        val userId = authManager.currentUserId
        if (userId == null) {
            println("❌ SyncService: Cannot force sync - no user ID")
            return
        }

        println("🔄 SyncService: Forcing full sync")
        _syncState.value = SyncState.Syncing

        try {
            // Get all unsynced reminders
            val unsyncedEntities = dao.getUnsyncedReminders()
            println("⬆️ SyncService: Found ${unsyncedEntities.size} unsynced reminders")

            for (entity in unsyncedEntities) {
                val reminder = entity.toDomain()
                pushReminderToFirestore(reminder)
            }

            _syncState.value = SyncState.Synced
            println("✅ SyncService: Force sync completed")
        } catch (e: Exception) {
            println("❌ SyncService: Force sync failed - ${e.message}")
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    /**
     * Clean up when service is destroyed
     */
    fun cleanup() {
        stopRealtimeSync()
        scope.cancel()
    }
}

/**
 * Sync state
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}
