package com.bhaskar.synctask.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun SystemAppearance(isDark: Boolean) {
        // iOS status bar logic if needed, usually handled by UIViewController
        // But we can force update if required.
        // For now, leaving as no-op or simple style update if accessible.
        // Compose usually handles this if UIViewController based status bar is used.
}
