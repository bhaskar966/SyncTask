package com.bhaskar.synctask.presentation.settings

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isPushEnabled: Boolean = true,
    val isEmailEnabled: Boolean = false,
    val isBadgeEnabled: Boolean = true,
    val userName: String = "Jane Doe",
    val userEmail: String = "jane.doe@example.com"
)

sealed class SettingsEvent {
    data class OnThemeChanged(val mode: ThemeMode) : SettingsEvent()
    data class OnPushToggled(val enabled: Boolean) : SettingsEvent()
    data class OnEmailToggled(val enabled: Boolean) : SettingsEvent()
    data class OnBadgeToggled(val enabled: Boolean) : SettingsEvent()
    data object OnLogout : SettingsEvent()
}
