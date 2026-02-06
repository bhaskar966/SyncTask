package com.bhaskar.synctask.presentation.settings.components

sealed class SettingsEvent {
    data class OnThemeChanged(val mode: ThemeMode) : SettingsEvent()
    data class OnPushToggled(val enabled: Boolean) : SettingsEvent()
    data class OnEmailToggled(val enabled: Boolean) : SettingsEvent()
    data class OnBadgeToggled(val enabled: Boolean) : SettingsEvent()
    data object OnLogout : SettingsEvent()
    data object OnRestorePurchases : SettingsEvent()
}