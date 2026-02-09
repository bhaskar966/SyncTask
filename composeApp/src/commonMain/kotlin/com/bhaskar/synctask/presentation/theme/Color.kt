package com.bhaskar.synctask.presentation.theme

import androidx.compose.ui.graphics.Color

// --- Base Palette ---
val MonoBlack = Color(0xFF000000)
val MonoWhite = Color(0xFFFFFFFF)
val MonoDarkGray = Color(0xFF252528) // Deep Gray
val MonoMediumGray = Color(0xFF8E8E93)
val MonoLightGray = Color(0xFFEEEEEE) // Very Light Gray (Brightened)
val MonoSurfaceGray = Color(0xFFF9F9F9) // Slightly off-white

// Accent
val AccentOrange = Color(0xFFFF6D00)
val AccentOrangeContainer = Color(0xFFFFD180)

// Semantic
val ErrorRed = Color(0xFFB00020)
val DarkErrorRed = Color(0xFFCF6679)

// --- Light Theme Colors ---
val LightPrimary = MonoBlack
val LightOnPrimary = MonoWhite
val LightPrimaryContainer = MonoLightGray
val LightOnPrimaryContainer = MonoBlack

val LightSecondary = MonoDarkGray
val LightOnSecondary = MonoWhite
val LightSecondaryContainer = Color(0xFFE5E5EA)
val LightOnSecondaryContainer = MonoBlack

val LightTertiary = AccentOrange
val LightOnTertiary = MonoWhite
val LightTertiaryContainer = AccentOrangeContainer
val LightOnTertiaryContainer = Color(0xFFE65100)

val LightBackground = MonoWhite
val LightOnBackground = MonoBlack
val LightSurface = MonoWhite
val LightOnSurface = MonoBlack
val LightSurfaceVariant = MonoLightGray
val LightOnSurfaceVariant = MonoDarkGray
val LightOutline = MonoMediumGray

val LightError = ErrorRed
val LightOnError = MonoWhite

// --- Dark Theme Colors ---
val DarkPrimary = MonoWhite
val DarkOnPrimary = MonoBlack
val DarkPrimaryContainer = MonoDarkGray
val DarkOnPrimaryContainer = MonoWhite

val DarkSecondary = MonoLightGray
val DarkOnSecondary = MonoBlack
val DarkSecondaryContainer = Color(0xFF3A3A3C)
val DarkOnSecondaryContainer = MonoWhite

val DarkTertiary = AccentOrange
val DarkOnTertiary = MonoWhite
val DarkTertiaryContainer = Color(0xFFCC5500)
val DarkOnTertiaryContainer = MonoWhite

val DarkBackground = MonoBlack
val DarkOnBackground = MonoWhite
val DarkSurface = MonoBlack
val DarkOnSurface = MonoWhite
val DarkSurfaceVariant = MonoDarkGray
val DarkOnSurfaceVariant = MonoLightGray
val DarkOutline = MonoMediumGray

val DarkError = DarkErrorRed
val DarkOnError = MonoBlack


