package com.bhaskar.synctask.di

import androidx.room.RoomDatabase
import com.bhaskar.synctask.data.auth.GoogleAuthenticator
import com.bhaskar.synctask.data.platform.PlatformFCMManager
import com.bhaskar.synctask.data.platform.PlatformFirestoreDataSource
import com.bhaskar.synctask.data.platform.PlatformNotificationScheduler
import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.db.getDatabaseBuilder
import com.bhaskar.synctask.platform.FCMManager
import com.bhaskar.synctask.platform.FirestoreDataSource
import com.bhaskar.synctask.platform.NotificationScheduler
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<SyncTaskDatabase>> { getDatabaseBuilder(androidContext()) }

    single { PlatformNotificationScheduler(androidContext()) } bind NotificationScheduler::class
    single { PlatformFirestoreDataSource() } bind FirestoreDataSource::class
    single { GoogleAuthenticator(androidContext()) }
    single { PlatformFCMManager(androidContext()) } bind FCMManager::class
}
