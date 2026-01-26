package com.bhaskar.synctask.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [ReminderEntity::class, SyncQueueEntity::class], version = 1)
@ConstructedBy(SyncTaskDatabaseConstructor::class)
abstract class SyncTaskDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SyncTaskDatabaseConstructor : RoomDatabaseConstructor<SyncTaskDatabase> {
    override fun initialize(): SyncTaskDatabase
}

fun getSyncDatabase(builder: RoomDatabase.Builder<SyncTaskDatabase>): SyncTaskDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}