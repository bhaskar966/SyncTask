package com.bhaskar.synctask.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.bhaskar.synctask.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {
    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }
}
