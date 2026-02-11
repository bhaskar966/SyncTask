package com.bhaskar.synctask.domain.repository

import com.bhaskar.synctask.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    val is24HourFormat: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun set24HourFormat(is24Hour: Boolean)
}
