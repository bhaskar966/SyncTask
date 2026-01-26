package com.bhaskar.synctask.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask


fun getDatabaseBuilder(): RoomDatabase.Builder<SyncTaskDatabase> {
    val dbFilePath = documentDir() + "/sync_task.db"
    return Room.databaseBuilder<SyncTaskDatabase>(
        name = dbFilePath
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDir(): String {
    val docDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    return requireNotNull(docDir?.path)
}