package com.bhaskar.synctask.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    val permissionsController: PermissionsController // Inject this from compose
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        checkPermission()
    }

    fun onScreenResume() {
        checkPermission()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChanged -> {
                _state.update { it.copy(themeMode = event.mode) }
            }
            is SettingsEvent.OnPushToggled -> {
                if (event.enabled) {
                    requestNotificationPermission()
                } else {
                    _state.update { it.copy(isPushEnabled = false) }
                }
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

    private fun checkPermission() {
        viewModelScope.launch {
            try {
                val isGranted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                _state.update { it.copy(isPushEnabled = isGranted) }
            } catch (e: Exception) {
                println("Error checking permission: ${e.message}")
            }
        }
    }

    private fun requestNotificationPermission() {
        viewModelScope.launch {
            try {
                permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                _state.update { it.copy(isPushEnabled = true) }
            } catch (denied: DeniedAlwaysException) {
                // User denied with "Don't ask again" or needs to enable in settings
                _state.update { it.copy(isPushEnabled = false) }
                openAppSettings()
            } catch (denied: DeniedException) {
                // User denied once
                _state.update { it.copy(isPushEnabled = false) }
            }
        }
    }

    fun openAppSettings() {
        viewModelScope.launch {
            permissionsController.openAppSettings()
        }
    }
}
