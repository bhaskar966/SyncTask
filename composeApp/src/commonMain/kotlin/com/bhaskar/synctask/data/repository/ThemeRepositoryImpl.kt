package com.bhaskar.synctask.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bhaskar.synctask.domain.model.ThemeMode
import com.bhaskar.synctask.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_24_HOUR_FORMAT = androidx.datastore.preferences.core.booleanPreferencesKey("is_24_hour_format")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    override val is24HourFormat: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_24_HOUR_FORMAT] ?: false // Default to 12h (false)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun set24HourFormat(is24Hour: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_24_HOUR_FORMAT] = is24Hour
        }
    }
}
