package com.bhaskar.synctask.data

import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.ReminderRepository
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock

class ReminderRepositoryImpl(
    private val database: SyncTaskDatabase,
    private val firestoreDataSource: FirestoreDataSource,
    private val scope: CoroutineScope // Application scope
) : ReminderRepository {

    private val dao = database.reminderDao()

    override fun getReminders(): Flow<List<Reminder>> {
        val userId = "user_1" 
        return dao.getAllReminders(userId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActiveReminders(): Flow<List<Reminder>> {
        val userId = "user_1"
        return dao.getActiveReminders(userId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getReminderById(id: String): Flow<Reminder?> {
        return dao.getReminderById(id).map { it?.toDomain() }
    }

    override suspend fun createReminder(reminder: Reminder) {
        dao.insertReminder(reminder.toEntity())
        sync()
    }

    override suspend fun updateReminder(reminder: Reminder) {
        dao.insertReminder(reminder.toEntity()) // OnConflictStrategy.REPLACE
    }

    override suspend fun deleteReminder(id: String) {
        dao.deleteReminder(id)
        firestoreDataSource.deleteReminder(id)
    }

    override suspend fun completeReminder(id: String) {
        val entity = dao.getReminderById(id).firstOrNull() ?: return
        val updated = entity.toDomain().copy(
            status = ReminderStatus.COMPLETED,
            completedAt = Clock.System.now().toEpochMilliseconds(),
            isSynced = false
        )
        updateReminder(updated)
    }

    override suspend fun snoozeReminder(id: String, snoozeUntil: Long) {
        val entity = dao.getReminderById(id).firstOrNull() ?: return
        val updated = entity.toDomain().copy(
            status = ReminderStatus.SNOOZED,
            snoozeUntil = snoozeUntil,
            isSynced = false
        )
        updateReminder(updated)
    }

    override suspend fun sync() {
        val unsynced = dao.getUnsyncedReminders()
        unsynced.forEach { entity ->
            try {
                firestoreDataSource.saveReminder(entity.toDomain())
                dao.updateSyncStatus(entity.id, true)
            } catch (e: Exception) {
                println("Sync failed for ${entity.id}: $e")
            }
        }
    }
}
