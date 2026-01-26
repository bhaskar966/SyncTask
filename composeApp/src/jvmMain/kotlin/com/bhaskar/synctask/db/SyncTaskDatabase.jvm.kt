package com.bhaskar.synctask.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<SyncTaskDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "sync_task.db")
    return Room.databaseBuilder(
        name = dbFile.absolutePath
    )
}