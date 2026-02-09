package com.bhaskar.synctask.presentation.create.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bhaskar.synctask.presentation.theme.PredefinedIcons

@Composable
fun IconPickerDialog(
    selectedIcon: String?,
    onIconSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter icons based on search (you can implement more sophisticated search if needed)
    val filteredIcons = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            PredefinedIcons.icons
        } else {
            // For now, just return all icons. You can add icon names/tags for better search
            PredefinedIcons.icons
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Choose Icon",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val actionIcon = if(selectedIcon != null) Icons.Default.Check else Icons.Default.Close

                    IconButton(onClick = onDismiss) {
                        Icon(actionIcon, "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Search bar
//                OutlinedTextField(
//                    value = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    placeholder = { Text("Search icons...") },
//                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
//                    modifier = Modifier.fillMaxWidth(),
//                    singleLine = true,
//                    shape = RoundedCornerShape(12.dp)
//                )

                Spacer(Modifier.height(16.dp))

                // Clear Selection Button
                if (selectedIcon != null) {
                    TextButton(
                        onClick = {
                            onIconSelected(null)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Icon Selection")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Icons Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredIcons) { icon ->
                        IconItem(
                            icon = icon,
                            isSelected = icon == selectedIcon,
                            onClick = {
                                onIconSelected(icon)
                                // We don't dismiss immediately so the user can see the selection highlight
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconItem(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
