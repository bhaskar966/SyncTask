package com.bhaskar.synctask.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<SyncTaskDatabase> {
    return Room.databaseBuilder<SyncTaskDatabase>(
        context = context.applicationContext,
        name = context.applicationContext.getDatabasePath("sync_task.db").absolutePath
    )
}