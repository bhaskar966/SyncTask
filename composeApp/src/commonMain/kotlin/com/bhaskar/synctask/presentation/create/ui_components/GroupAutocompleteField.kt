package com.bhaskar.synctask.presentation.create.ui_components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.presentation.utils.parseHexColor

@Composable
fun GroupAutocompleteField(
    selectedGroupId: String?,
    availableGroups: List<ReminderGroup>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val selectedGroup = availableGroups.find { it.id == selectedGroupId }

    val filteredGroups = if (searchQuery.isBlank()) {
        availableGroups
    } else {
        availableGroups.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val showCreateOption = searchQuery.isNotBlank() &&
            !availableGroups.any { it.name.equals(searchQuery, ignoreCase = true) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = if (isExpanded) searchQuery else (selectedGroup?.name ?: ""),
            onValueChange = {
                onSearchQueryChanged(it)
                isExpanded = true
            },
            label = { Text("Group") },
            leadingIcon = {
                Text(
                    selectedGroup?.icon ?: "📁",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            trailingIcon = {
                Row {
                    if (selectedGroupId != null) {
                        IconButton(onClick = {
                            onGroupSelected(null)
                            onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            "Toggle dropdown"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    item {
                        DropdownItem(
                            icon = "🚫",
                            name = "None",
                            color = null,
                            isSelected = selectedGroupId == null,
                            onClick = {
                                onGroupSelected(null)
                                onSearchQueryChanged("")
                                isExpanded = false
                            }
                        )
                    }

                    items(filteredGroups) { group ->
                        DropdownItem(
                            icon = group.icon,
                            name = group.name,
                            color = group.colorHex,
                            isSelected = selectedGroupId == group.id,
                            onClick = {
                                onGroupSelected(group.id)
                                onSearchQueryChanged("")
                                isExpanded = false
                            }
                        )
                    }

                    if (showCreateOption) {
                        item {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCreateGroup(searchQuery)
                                        onSearchQueryChanged("")
                                        isExpanded = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Create \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownItem(
    icon: String,
    name: String,
    color: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (color != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = parseHexColor(color),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            Text(
                icon,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(Modifier.weight(1f))

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}