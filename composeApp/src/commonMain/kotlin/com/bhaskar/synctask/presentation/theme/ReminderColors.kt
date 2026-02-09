package com.bhaskar.synctask.presentation.theme

import androidx.compose.ui.graphics.Color

data class ColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

object ReminderColors {
    val colors = listOf(
        ColorOption("Red", "#EF4444", Color(0xFFEF4444)),
        ColorOption("Rose", "#F43F5E", Color(0xFFF43F5E)),
        ColorOption("Orange", "#F97316", Color(0xFFF97316)),
        ColorOption("Amber", "#F59E0B", Color(0xFFF59E0B)),
        ColorOption("Yellow", "#EAB308", Color(0xFFEAB308)),
        ColorOption("Lime", "#84CC16", Color(0xFF84CC16)),
        ColorOption("Green", "#10B981", Color(0xFF10B981)),
        ColorOption("Emerald", "#059669", Color(0xFF059669)),
        ColorOption("Teal", "#14B8A6", Color(0xFF14B8A6)),
        ColorOption("Cyan", "#06B6D4", Color(0xFF06B6D4)),
        ColorOption("Sky", "#0EA5E9", Color(0xFF0EA5E9)),
        ColorOption("Blue", "#3B82F6", Color(0xFF3B82F6)),
        ColorOption("Indigo", "#6366F1", Color(0xFF6366F1)),
        ColorOption("Violet", "#8B5CF6", Color(0xFF8B5CF6)),
        ColorOption("Purple", "#A855F7", Color(0xFFA855F7)),
        ColorOption("Fuchsia", "#D946EF", Color(0xFFD946EF)),
        ColorOption("Pink", "#EC4899", Color(0xFFEC4899)),
        ColorOption("Gray", "#6B7280", Color(0xFF6B7280))
    )

    fun getColorByHex(hex: String): Color? {
        return colors.find { it.hex == hex }?.color
    }

    fun getDefaultColor(): ColorOption = colors[17] // Gray
}