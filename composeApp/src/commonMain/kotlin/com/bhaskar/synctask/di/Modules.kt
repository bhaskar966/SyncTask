package com.bhaskar.synctask.di

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.fcm.FCMInitializer
import com.bhaskar.synctask.data.services.RecurrenceService
import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.data.repository.GroupRepositoryImpl
import com.bhaskar.synctask.data.repository.ProfileRepositoryImpl
import com.bhaskar.synctask.data.repository.TagRepositoryImpl
import com.bhaskar.synctask.data.sync.SyncService
import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.data.repository.SubscriptionRepositoryImpl
import com.bhaskar.synctask.data.services.RevenueCatService
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.detail.ReminderDetailViewModel
import com.bhaskar.synctask.presentation.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.KoinApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.bhaskar.synctask.db.getSyncDatabase
import com.bhaskar.synctask.domain.NotificationCalculator
import com.bhaskar.synctask.domain.repository.GroupRepository
import com.bhaskar.synctask.domain.repository.ProfileRepository
import com.bhaskar.synctask.domain.repository.TagRepository
import com.bhaskar.synctask.presentation.auth.AuthViewModel
import com.bhaskar.synctask.presentation.groups.GroupsViewModel

expect val platformModule: Module

val appModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    single {
        AuthManager(
            googleAuthenticator = get()
        )
    }
}

val dataModule = module {
    // Database
    single { getSyncDatabase(get()) }

    // DAOs - get from database instance
    single { get<SyncTaskDatabase>().reminderDao() }
    single { get<SyncTaskDatabase>().groupDao() }
    single { get<SyncTaskDatabase>().tagDao() }

    // Services
    single { RecurrenceService() }
    single { RevenueCatService() }
    single { NotificationCalculator(get()) }

    // Repositories
    single<ReminderRepository> {
        ReminderRepositoryImpl(
            database = get(),
            firestoreDataSource = get(),
            recurrenceService = get(),
            notificationScheduler = get(),
            authManager = get(),
            scope = get()
        )
    }

    single<GroupRepository> {
        GroupRepositoryImpl(
            groupDao = get(),
            firestoreDataSource = get(),
            authManager = get(),
            coroutineScope = get(),
            reminderDao = get()
        )
    }

    single<TagRepository> {
        TagRepositoryImpl(
            tagDao = get(),
            firestoreDataSource = get(),
            authManager = get(),
            coroutineScope = get()
        )
    }

    single {
        SyncService(
            firestoreDataSource = get(),
            dao = get(),
            notificationScheduler = get(),
            authManager = get()
        )
    }

    single {
        FCMInitializer(
            authManager = get(),
            fcmManager = get(),
            scope = get()
        )
    }
    
    // Subscription Repository
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get()) }

    // Profile Repository
    single<ProfileRepository> { ProfileRepositoryImpl() }
}

val domainModule = module {
    viewModelOf(::ReminderListViewModel)
    viewModelOf(::CreateReminderViewModel)
    viewModelOf(::ReminderDetailViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::GroupsViewModel)
}

fun initKoin(config: (KoinApplication.() -> Unit)? = null) {
    org.koin.core.context.startKoin {
        config?.invoke(this)
        modules(appModule, dataModule, domainModule, platformModule)
    }
}