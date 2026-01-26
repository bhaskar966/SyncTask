package com.bhaskar.synctask.di

import androidx.room.RoomDatabase
import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.db.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<SyncTaskDatabase>> { getDatabaseBuilder() }
}
