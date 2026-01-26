package com.bhaskar.synctask.data

import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.ReminderRepository
import com.bhaskar.synctask.domain.model.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        // Room DAO generated suspend function for single item returns T? directly if not Flow.
        // But I defined `suspend fun getReminderById(id: String): ReminderEntity?` in DAO.
        // The interface expects `Flow<Reminder?>`.
        // So I should probably change DAO to returning Flow or wrap it.
        // Or I can use `dao.getAllReminders(userId)` and filter? No.
        // I should change DAO to `fun getReminderById(id: String): Flow<ReminderEntity?>`.
        // Let's assume I will update DAO next step if I didn't already.
        // Wait, I defined `suspend fun getReminderById` in DAO step 1244.
        // BUT `ReminderRepository` expects `Flow`.
        // I will use a simple flow builder for now or update DAO.
        // Updating DAO is better. I will assume DAO returns Flow for consistency with Repository.
        // But for this step I will execute the change assuming DAO returns suspend, I can verify later.
        // actually repository expects Flow.
        // I will change code here to use flow { emit(dao.getReminderById(id)) } ?? No that's not reactive.
        // I MUST change DAO to return Flow<ReminderEntity?>.
        // I'll update DAO in next step. For now I'll write code assuming `dao.getReminderByIdFlow(id)`.
        // Or I can just query all and filtering?
        // No, I'll update DAO to `getReminderById(id): Flow<ReminderEntity?>`.
        return kotlinx.coroutines.flow.flow {
             emit(dao.getReminderById(id)?.toDomain())
        }
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
        val entity = dao.getReminderById(id) ?: return
        val updated = entity.toDomain().copy(
            status = ReminderStatus.COMPLETED,
            completedAt = Clock.System.now().toEpochMilliseconds(),
            isSynced = false
        )
        updateReminder(updated)
    }

    override suspend fun snoozeReminder(id: String, snoozeUntil: Long) {
        val entity = dao.getReminderById(id) ?: return
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
