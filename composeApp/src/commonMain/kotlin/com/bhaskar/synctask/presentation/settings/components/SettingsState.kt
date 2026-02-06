package com.bhaskar.synctask.presentation.settings.components

import androidx.compose.ui.graphics.ImageBitmap
import com.bhaskar.synctask.domain.model.ActiveSubscriptionInfo

data class SettingsState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String? = null,
    val userProfileImage: ImageBitmap? = null,
    val isLoadingImage: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isPushEnabled: Boolean = false,
    val isEmailEnabled: Boolean = false,
    val isBadgeEnabled: Boolean = true,
    // Subscription State
    val isPremium: Boolean = false,
    val managementUrl: String? = null,
    val activeSubscriptionInfo: ActiveSubscriptionInfo? = null,
    val isRestoring: Boolean = false
)