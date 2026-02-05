package com.bhaskar.synctask.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM ReminderEntity WHERE userId = :userId ORDER BY dueTime ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM ReminderEntity WHERE id = :id")
    fun getReminderById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM ReminderEntity WHERE userId = :userId AND status = 'ACTIVE' ORDER BY dueTime ASC")
    fun getActiveReminders(userId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("DELETE FROM ReminderEntity WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("UPDATE ReminderEntity SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean)

    @Query("SELECT * FROM ReminderEntity WHERE isSynced = 0")
    suspend fun getUnsyncedReminders(): List<ReminderEntity>

    // SyncQueue
    @Insert
    suspend fun addToQueue(item: SyncQueueEntity)

    @Query("SELECT * FROM SyncQueueEntity ORDER BY timestamp ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Query("DELETE FROM SyncQueueEntity WHERE id = :id")
    suspend fun removeFromQueue(id: Long)


    // Get reminders by group
    @Query("SELECT * FROM ReminderEntity WHERE userId = :userId AND groupId = :groupId AND status = 'ACTIVE' ORDER BY isPinned DESC, dueTime ASC")
    fun getRemindersByGroup(userId: String, groupId: String): Flow<List<ReminderEntity>>

    // Get pinned reminders
    @Query("SELECT * FROM ReminderEntity WHERE userId = :userId AND isPinned = 1 AND status = 'ACTIVE' ORDER BY dueTime ASC")
    fun getPinnedReminders(userId: String): Flow<List<ReminderEntity>>

    // Count pinned reminders (for premium gate)
    @Query("SELECT COUNT(*) FROM ReminderEntity WHERE userId = :userId AND isPinned = 1 AND status = 'ACTIVE'")
    suspend fun getPinnedCount(userId: String): Int

    // Count active reminders (for premium gate)
    @Query("SELECT COUNT(*) FROM ReminderEntity WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActiveReminderCount(userId: String): Int

    //  Get ungrouped reminders
    @Query("SELECT * FROM ReminderEntity WHERE userId = :userId AND (groupId IS NULL OR groupId = '') AND status != 'COMPLETED' AND status != 'DISMISSED' ORDER BY dueTime ASC")
    fun getUngroupedReminders(userId: String): Flow<List<ReminderEntity>>

    // Get reminder count by group
    @Query("SELECT COUNT(*) FROM ReminderEntity WHERE groupId = :groupId AND status != 'COMPLETED' AND status != 'DISMISSED'")
    suspend fun getReminderCountByGroup(groupId: String): Int

    // Get pinned reminder count
    @Query("SELECT COUNT(*) FROM ReminderEntity WHERE userId = :userId AND isPinned = 1 AND status != 'COMPLETED' AND status != 'DISMISSED'")
    suspend fun getPinnedReminderCount(userId: String): Int

    // Unassign reminders from deleted group
    @Query("UPDATE ReminderEntity SET groupId = NULL, isSynced = 0 WHERE groupId = :groupId")
    suspend fun unassignRemindersFromGroup(groupId: String)
}