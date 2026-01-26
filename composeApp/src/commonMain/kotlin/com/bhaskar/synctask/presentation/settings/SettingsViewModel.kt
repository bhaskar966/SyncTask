package com.bhaskar.synctask.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChanged -> {
                _state.update { it.copy(themeMode = event.mode) }
            }
            is SettingsEvent.OnPushToggled -> {
                _state.update { it.copy(isPushEnabled = event.enabled) }
            }
            is SettingsEvent.OnEmailToggled -> {
                _state.update { it.copy(isEmailEnabled = event.enabled) }
            }
            is SettingsEvent.OnBadgeToggled -> {
                _state.update { it.copy(isBadgeEnabled = event.enabled) }
            }
            SettingsEvent.OnLogout -> {
                // handle logout
            }
        }
    }
}
