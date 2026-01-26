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
    suspend fun getReminderById(id: String): ReminderEntity?

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
}
