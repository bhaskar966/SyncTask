package com.bhaskar.synctask.di

import androidx.room.RoomDatabase
import com.bhaskar.synctask.data.FirestoreDataSource
import com.bhaskar.synctask.data.ReminderRepositoryImpl
import com.bhaskar.synctask.db.SyncTaskDatabase
import com.bhaskar.synctask.domain.CreateReminderUseCase
import com.bhaskar.synctask.domain.ReminderRepository
import com.bhaskar.synctask.presentation.list.ReminderListViewModel
import com.bhaskar.synctask.presentation.create.CreateReminderViewModel
import com.bhaskar.synctask.presentation.detail.ReminderDetailViewModel
import com.bhaskar.synctask.presentation.recurrence.CustomRecurrenceViewModel
import com.bhaskar.synctask.presentation.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.core.KoinApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bhaskar.synctask.db.getSyncDatabase

expect val platformModule: Module

val appModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
}


val dataModule = module {
    single<SyncTaskDatabase> { getSyncDatabase(get()) }
    
    single { get<SyncTaskDatabase>().reminderDao() }
    
    single { FirestoreDataSource() }
    singleOf(::ReminderRepositoryImpl).bind<ReminderRepository>()
}

val domainModule = module {
    factoryOf(::CreateReminderUseCase)
    viewModelOf(::ReminderListViewModel)
    viewModelOf(::CreateReminderViewModel)
    viewModelOf(::ReminderDetailViewModel)
    viewModelOf(::CustomRecurrenceViewModel)
    viewModelOf(::SettingsViewModel)
}

fun initKoin(config: (KoinApplication.() -> Unit)? = null) {
    org.koin.core.context.startKoin {
        config?.invoke(this)
        modules(appModule, dataModule, domainModule, platformModule)
    }
}
