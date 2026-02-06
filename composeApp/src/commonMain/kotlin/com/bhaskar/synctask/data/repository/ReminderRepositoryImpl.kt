package com.bhaskar.synctask.data.repository

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.data.services.RecurrenceService
import com.bhaskar.synctask.data.toDomain
import com.bhaskar.synctask.data.toEntity
import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.platform.FirestoreDataSource
import com.bhaskar.synctask.platform.NotificationScheduler
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

class ReminderRepositoryImpl(
    private val database: SyncTaskDatabase,
    private val firestoreDataSource: FirestoreDataSource,
    private val recurrenceService: RecurrenceService,
    private val notificationScheduler: NotificationScheduler,
    private val authManager: AuthManager,
    private val scope: CoroutineScope
) : ReminderRepository {

    private val dao = database.reminderDao()

    private var syncJob: Job? = null

    private val userId: String
        get() = authManager.currentUserId ?: "anonymous"

    init {
        // Start real-time sync when repository is created
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        println("🔄 Starting Firestore sync for user: ${state.uid}")
                        syncFromFirestore(state.uid)
                    }
                    is AuthState.Unauthenticated -> {
                        println("🔄 User logged out - stopping sync")
                    }
                    is AuthState.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private suspend fun syncFromFirestore(userId: String) {
        try {
            firestoreDataSource.getReminders(userId).collect { cloudReminders ->
                println("☁️ Received ${cloudReminders.size} reminders from Firestore")

                // 🔥 STEP 1: Create a set of cloud reminder IDs for quick lookup
                val cloudReminderIds = cloudReminders.map { it.id }.toSet()
                println("📋 Cloud reminder IDs: $cloudReminderIds")

                // 🔥 STEP 2: Get all local reminders and delete ones not in cloud
                val localReminders = dao.getAllReminders(userId).first()
                println("📱 Found ${localReminders.size} local reminders")

                localReminders.forEach { localEntity ->
                    if (localEntity.id !in cloudReminderIds) {
                        println("🗑️ Deleting reminder from local DB (deleted from cloud): ${localEntity.title}")
                        dao.deleteReminder(localEntity.id)
                        withContext(Dispatchers.Main) {
                            notificationScheduler.cancelNotification(localEntity.id)
                        }
                    }
                }

                // 🔥 STEP 3: Process cloud reminders (add/update)
                cloudReminders.forEach { cloudReminder ->
                    val localReminder = dao.getReminderById(cloudReminder.id).firstOrNull()
                    if (localReminder == null) {
                        // New reminder from cloud
                        println("📥 New from cloud: ${cloudReminder.title}")
                        dao.insertReminder(cloudReminder.copy(isSynced = true).toEntity())
                    } else {
                        // Resolve conflicts (last-write-wins based on lastModified)
                        val local = localReminder.toDomain()
                        if (cloudReminder.lastModified > local.lastModified) {
                            println("📥 Update from cloud: ${cloudReminder.title}")
                            dao.insertReminder(cloudReminder.copy(isSynced = true).toEntity())

                            // Cancel notification if status changed to completed/dismissed
                            if (cloudReminder.status == ReminderStatus.COMPLETED ||
                                cloudReminder.status == ReminderStatus.DISMISSED) {
                                println("🗑️ Cancelling notification for ${cloudReminder.id}")
                                notificationScheduler.cancelNotification(cloudReminder.id)
                            }
                        } else if (local.lastModified > cloudReminder.lastModified && !local.isSynced) {
                            // Local is newer and not synced - push to cloud
                            println("📤 Pushing newer local version to cloud: ${local.title}")
                            firestoreDataSource.saveReminder(local)
                            dao.updateSyncStatus(local.id, true)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationScheduler.scheduleNext()
                }
            }
        } catch (e: Exception) {
            println("❌ Firestore sync error: ${e.message}")
            e.printStackTrace()
        }
    }


    override fun getReminders(): Flow<List<Reminder>> {
        return dao.getAllReminders(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getActiveReminders(): Flow<List<Reminder>> {
        return dao.getActiveReminders(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getReminderById(id: String): Flow<Reminder?> {
        return dao.getReminderById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun createReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        println("💾 Repository: Creating reminder - ${reminder.title}")
        val now = Clock.System.now().toEpochMilliseconds()
        val withTimestamp = reminder.copy(
            userId = userId,
            lastModified = now,
            isSynced = false
        )

        dao.insertReminder(withTimestamp.toEntity())
        withContext(Dispatchers.Main) {
            notificationScheduler.scheduleNext()
        }

        syncToCloud(withTimestamp)
    }

    override suspend fun updateReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        println("💾 Repository: Updating reminder - ${reminder.title}")
        val now = Clock.System.now().toEpochMilliseconds()
        val withTimestamp = reminder.copy(
            userId = userId,
            lastModified = now,
            isSynced = false
        )

        dao.insertReminder(withTimestamp.toEntity())
        withContext(Dispatchers.Main) {
            notificationScheduler.scheduleNext()
        }

        syncToCloud(withTimestamp)
    }

    override suspend fun completeReminder(id: String) = withContext(Dispatchers.IO) {
        println("✅ Repository: Completing reminder $id")
        val reminder = getReminderById(id).firstOrNull() ?: return@withContext
        val now = Clock.System.now().toEpochMilliseconds()
        val completed = reminder.copy(
            status = ReminderStatus.COMPLETED,
            completedAt = now,
            lastModified = now,
            isSynced = false
        )

        dao.insertReminder(completed.toEntity())
        if (reminder.recurrence != null && reminder.recurrence.afterCompletion) {
            println("🔁 Creating next instance (afterCompletion = true)")
            val nextReminder = recurrenceService.createNextInstance(reminder, now)
            if (nextReminder != null) {
                dao.insertReminder(nextReminder.toEntity())
                syncToCloud(nextReminder)
            }
        }

        withContext(Dispatchers.Main) {
            notificationScheduler.scheduleNext()
        }

        syncToCloud(completed)
    }

    override suspend fun snoozeReminder(id: String, snoozeMinutes: Int) =
        withContext(Dispatchers.IO) {
            println("⏰ Repository: Snoozing reminder $id for $snoozeMinutes minutes")
            val reminder = getReminderById(id).firstOrNull() ?: return@withContext
            val now = Clock.System.now().toEpochMilliseconds()
            val snoozeUntil = now + (snoozeMinutes * 60 * 1000L)
            val snoozed = reminder.copy(
                status = ReminderStatus.SNOOZED,
                snoozeUntil = snoozeUntil,
                lastModified = now,
                isSynced = false
            )

            dao.insertReminder(snoozed.toEntity())
            withContext(Dispatchers.Main) {
                notificationScheduler.scheduleNext()
            }

            syncToCloud(snoozed)
        }

    override suspend fun deleteReminder(id: String) = withContext(Dispatchers.IO) {
        println("🗑️ Repository: Deleting reminder $id")
        dao.deleteReminder(id)

        firestoreDataSource.deleteReminder(userId, id)

        withContext(Dispatchers.Main) {
            notificationScheduler.scheduleNext()
        }
    }

    override suspend fun sync() = withContext(Dispatchers.IO) {
        val unsynced = dao.getUnsyncedReminders()
        unsynced.forEach { entity ->
            try {
                firestoreDataSource.saveReminder(entity.toDomain())
                dao.updateSyncStatus(entity.id, true)
            } catch (e: Exception) {
                println("❌ Sync failed for ${entity.id}: $e")
            }
        }
    }

    override suspend fun dismissReminder(id: String) {
        withContext(Dispatchers.IO) {
            println("🚫 Repository: Dismissing reminder $id")
            val reminder = getReminderById(id).firstOrNull() ?: return@withContext
            val now = Clock.System.now().toEpochMilliseconds()
            val dismissed = reminder.copy(
                status = ReminderStatus.DISMISSED,
                lastModified = now,
                isSynced = false
            )

            dao.insertReminder(dismissed.toEntity())
            withContext(Dispatchers.Main) {
                notificationScheduler.scheduleNext()
            }

            syncToCloud(dismissed)
        }
    }

    override suspend fun rescheduleReminder(
        id: String,
        newDueTime: Long,
        newReminderTime: Long?
    ) {
        withContext(Dispatchers.IO) {
            println("📅 Repository: Rescheduling reminder $id")
            val reminder = getReminderById(id).firstOrNull() ?: return@withContext
            val now = Clock.System.now().toEpochMilliseconds()
            val rescheduled = reminder.copy(
                dueTime = newDueTime,
                reminderTime = newReminderTime,
                lastModified = now,
                status = ReminderStatus.ACTIVE,
                snoozeUntil = null,
                isSynced = false
            )

            dao.insertReminder(rescheduled.toEntity())
            withContext(Dispatchers.Main) {
                notificationScheduler.scheduleNext()
            }

            syncToCloud(rescheduled)
        }
    }

    private suspend fun syncToCloud(reminder: Reminder) {
        try {
            firestoreDataSource.saveReminder(reminder)
            dao.updateSyncStatus(reminder.id, true)
        } catch (e: Exception) {
            println("⚠️ Cloud sync failed: ${e.message}")
        }
    }

    suspend fun checkMissedReminders() = withContext(Dispatchers.IO) {
        println("🔍━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔍 checkMissedReminders() START")
        val now = Clock.System.now().toEpochMilliseconds()
        val timeZone = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date

        val allReminders = getReminders().first()
        val activeReminders = allReminders.filter { it.status == ReminderStatus.ACTIVE }
        println("🔍 Checking ${activeReminders.size} active reminders")

        var missedCount = 0
        activeReminders.forEach { reminder ->
            if (reminder.deadline != null) {
                if (now > reminder.deadline) {
                    println("⚠️ DEADLINE MISSED: ${reminder.title}")
                    markAsMissed(reminder, now)
                    missedCount++
                }
                return@forEach
            }

            val dueDate = Instant.fromEpochMilliseconds(reminder.dueTime)
                .toLocalDateTime(timeZone).date

            if (dueDate < today) {
                println("⚠️ DUE DATE MISSED: ${reminder.title}")
                markAsMissed(reminder, now)
                missedCount++
            }
        }

        println("🔍 Check complete: $missedCount reminders marked as MISSED")
        println("🔍━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        withContext(Dispatchers.Main) {
            notificationScheduler.scheduleNext()
        }
    }

    private suspend fun markAsMissed(reminder: Reminder, now: Long) {
        val missed = reminder.copy(
            status = ReminderStatus.MISSED,
            completedAt = now,
            isSynced = false
        )

        dao.insertReminder(missed.toEntity())
        syncToCloud(missed)
        println("❌ Marked as MISSED: ${reminder.title}")
    }

    override fun getRemindersByGroup(userId: String, groupId: String): Flow<List<Reminder>> {
        return dao.getRemindersByGroup(userId, groupId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getUngroupedReminders(userId: String): Flow<List<Reminder>> {
        return dao.getUngroupedReminders(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getReminderCountByGroup(groupId: String): Int =
        withContext(Dispatchers.IO) {
            dao.getReminderCountByGroup(groupId)
        }

    override suspend fun getActiveReminderCount(userId: String): Int =
        withContext(Dispatchers.IO) {
            dao.getActiveReminderCount(userId)
        }

    override suspend fun getPinnedReminderCount(userId: String): Int =
        withContext(Dispatchers.IO) {
            dao.getPinnedReminderCount(userId)
        }

    internal suspend fun handleNotificationDelivered(reminderId: String, isPreReminder: Boolean) {
        withContext(Dispatchers.IO) {
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("📱 handleNotificationDelivered() START")
            println("   reminderId: $reminderId")
            println("   isPreReminder: $isPreReminder")

            checkMissedReminders()

            if (isPreReminder) {
                println("📢 Pre-reminder delivered - no action needed")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return@withContext
            }

            val reminder = getReminderById(reminderId).firstOrNull()
            if (reminder == null) {
                println("❌ Reminder not found: $reminderId")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return@withContext
            }

            println("📋 Reminder found: ${reminder.title}")
            println("   Status: ${reminder.status}")
            println("   Recurrence: ${reminder.recurrence}")
            println("   Deadline: ${reminder.deadline}")

            if (reminder.status == ReminderStatus.SNOOZED) {
                println("⏰ Snoozed reminder fired - resetting to ACTIVE")
                val active = reminder.copy(
                    status = ReminderStatus.ACTIVE,
                    snoozeUntil = null,
                    isSynced = false
                )

                dao.insertReminder(active.toEntity())
                syncToCloud(active)
                println("✅ Snooze cleared")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return@withContext
            }

            if (reminder.deadline != null) {
                println("🔔 Deadline reminder notification delivered - no instance creation")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return@withContext
            }

            if (reminder.recurrence != null
                && !reminder.recurrence.afterCompletion
                && reminder.status == ReminderStatus.ACTIVE
            ) {
                println("🔁 Creating next instance (afterCompletion = false)")
                val now = Clock.System.now().toEpochMilliseconds()
                val nextReminder = recurrenceService.createNextInstance(reminder, now)

                if (nextReminder != null) {
                    dao.insertReminder(nextReminder.toEntity())
                    syncToCloud(nextReminder)
                    println("✅ Next instance created: ${nextReminder.id}")
                } else {
                    println("⚠️ No next instance created (end date/count reached)")
                }
            } else {
                println("ℹ️ No next instance creation needed")
            }

            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    override suspend fun deleteAllLocalReminders() = withContext(Dispatchers.IO) {
        dao.deleteAllReminders()
    }
}
