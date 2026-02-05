package com.bhaskar.synctask.presentation.utils

import androidx.compose.ui.graphics.Color

/**
 * Parses hex color string to Compose Color (KMP compatible)
 * Supports formats: #RGB, #RRGGBB, #AARRGGBB
 */
fun String.toComposeColor(): Color {
    val hex = this.removePrefix("#")

    return when (hex.length) {
        3 -> {
            // #RGB -> #RRGGBB
            val r = hex[0].toString().repeat(2).toInt(16)
            val g = hex[1].toString().repeat(2).toInt(16)
            val b = hex[2].toString().repeat(2).toInt(16)
            Color(r, g, b)
        }
        6 -> {
            // #RRGGBB
            val r = hex.take(2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            Color(r, g, b)
        }
        8 -> {
            // #AARRGGBB
            val a = hex.take(2).toInt(16)
            val r = hex.substring(2, 4).toInt(16)
            val g = hex.substring(4, 6).toInt(16)
            val b = hex.substring(6, 8).toInt(16)
            Color(r, g, b, a)
        }
        else -> Color.Gray // Fallback
    }
}

/**
 * Safe color parser with fallback
 */
fun parseHexColor(hex: String?, fallback: Color = Color.Gray): Color {
    return try {
        hex?.toComposeColor() ?: fallback
    } catch (e: Exception) {
        fallback
    }
}