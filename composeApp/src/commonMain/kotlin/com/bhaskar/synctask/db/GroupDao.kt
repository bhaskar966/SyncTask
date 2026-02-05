package com.bhaskar.synctask.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM ReminderGroup WHERE userId = :userId ORDER BY `order` ASC")
    fun getAllGroups(userId: String): Flow<List<ReminderGroupEntity>>

    @Query("SELECT * FROM ReminderGroup WHERE id = :id")
    fun getGroupById(id: String): Flow<ReminderGroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ReminderGroupEntity)

    @Query("DELETE FROM ReminderGroup WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("UPDATE ReminderGroup SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean)

    @Query("SELECT * FROM ReminderGroup WHERE isSynced = 0")
    suspend fun getUnsyncedGroups(): List<ReminderGroupEntity>

    @Query("SELECT COUNT(*) FROM ReminderGroup WHERE userId = :userId")
    suspend fun getGroupCount(userId: String): Int
}