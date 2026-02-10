package com.bhaskar.synctask.presentation.settings.components

import com.bhaskar.synctask.domain.model.ThemeMode

sealed class SettingsEvent {
    data class OnThemeChanged(val mode: ThemeMode) : SettingsEvent()
    data class OnPushToggled(val enabled: Boolean) : SettingsEvent()
    data class OnEmailToggled(val enabled: Boolean) : SettingsEvent()
    data class OnBadgeToggled(val enabled: Boolean) : SettingsEvent()
    data object OnLogout : SettingsEvent()
    data object OnRestorePurchases : SettingsEvent()
    
    // Tag Management Events
    data object OnShowTagsDialog : SettingsEvent()
    data object OnHideTagsDialog : SettingsEvent()
    data class OnNewTagNameChanged(val name: String) : SettingsEvent()
    data class OnNewTagColorChanged(val color: String) : SettingsEvent()
    data object OnCreateTag : SettingsEvent()
    data class OnDeleteTag(val tagId: String) : SettingsEvent()
    data object OnDismissPremiumDialog : SettingsEvent()
}