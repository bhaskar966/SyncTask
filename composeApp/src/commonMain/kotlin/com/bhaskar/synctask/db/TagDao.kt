package com.bhaskar.synctask.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag WHERE userId = :userId ORDER BY name ASC")
    fun getAllTags(userId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM Tag WHERE id = :id")
    fun getTagById(id: String): Flow<TagEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM Tag WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Query("UPDATE Tag SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean)

    @Query("SELECT * FROM Tag WHERE isSynced = 0")
    suspend fun getUnsyncedTags(): List<TagEntity>

    @Query("SELECT COUNT(*) FROM Tag WHERE userId = :userId")
    suspend fun getTagCount(userId: String): Int

    @Query("DELETE FROM Tag")
    suspend fun deleteAllTags()
}