package com.bhaskar.synctask.presentation.list.ui_components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Text(
        text = "$title ($count)".uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = color,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
    )
}
