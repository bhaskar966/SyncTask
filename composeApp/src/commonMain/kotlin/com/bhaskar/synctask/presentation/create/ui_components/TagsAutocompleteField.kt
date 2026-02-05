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
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.presentation.theme.Indigo500
import com.bhaskar.synctask.presentation.utils.parseHexColor

@Composable
fun TagsAutocompleteField(
    selectedTags: List<String>,
    availableTags: List<Tag>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onTagToggled: (String) -> Unit,
    onCreateTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    println("🏷️ TagsAutocompleteField - selectedTags: $selectedTags")
    println("🏷️ TagsAutocompleteField - availableTags: ${availableTags.map { it.name }}")
    println("🏷️ TagsAutocompleteField - searchQuery: '$searchQuery'")
    println("🏷️ TagsAutocompleteField - isExpanded: $isExpanded")

    val selectedTagObjects = availableTags.filter { it.id in selectedTags }
    println("🏷️ TagsAutocompleteField - selectedTagObjects: ${selectedTagObjects.map { it.name }}")

    val filteredTags = if (searchQuery.isBlank()) {
        availableTags
    } else {
        availableTags.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val showCreateOption = searchQuery.isNotBlank() &&
            !availableTags.any { it.name.equals(searchQuery, ignoreCase = true) }

    Column(modifier = modifier) {
        // Always show selected tags chips
        if (selectedTagObjects.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedTagObjects.forEach { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = {
                            println("🗑️ Removing tag: ${tag.name} (${tag.id})")
                            onTagToggled(tag.id)
                        }
                    )
                }
            }
        }

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                println("🔍 Search query changed: '$it'")
                onSearchQueryChanged(it)
                isExpanded = true
            },
            label = { Text("Tags") },
            leadingIcon = {
                Icon(Icons.Default.Tag, "Tags")
            },
            trailingIcon = {
                IconButton(onClick = {
                    val newExpandedState = !isExpanded
                    println("🔽 Toggle dropdown: $newExpandedState")
                    isExpanded = newExpandedState
                    // Clear search when closing dropdown
                    if (!newExpandedState) {
                        println("🧹 Clearing search query")
                        onSearchQueryChanged("")
                    }
                }) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        "Toggle dropdown"
                    )
                }
            },
            placeholder = { Text("Add tags...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Dropdown
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(filteredTags) { tag ->
                        TagDropdownItem(
                            tag = tag,
                            isSelected = tag.id in selectedTags,
                            onClick = {
                                println("✅ Tag clicked: ${tag.name} (${tag.id})")
                                println("✅ Was selected: ${tag.id in selectedTags}")

                                if (tag.id !in selectedTags) {
                                    println("✅ Adding tag to selection")
                                    onTagToggled(tag.id)
                                } else {
                                    println("⚠️ Tag already selected, skipping")
                                }

                                println("✅ Clearing search and closing dropdown")
                                // Clear search query after selection
                                onSearchQueryChanged("")
                                // Close the dropdown
                                isExpanded = false
                            }
                        )
                    }

                    // Create new tag option
                    if (showCreateOption) {
                        item {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        println("➕ Creating new tag: '$searchQuery'")
                                        onCreateTag(searchQuery)
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
                                    "Create tag \"$searchQuery\"",
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
private fun TagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = parseHexColor(tag.colorHex, fallback = Indigo500),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                tag.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TagDropdownItem(
    tag: Tag,
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
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = parseHexColor(tag.colorHex, fallback = Indigo500),
                    shape = RoundedCornerShape(6.dp)
                )
        )

        Spacer(Modifier.width(12.dp))

        Text(
            tag.name,
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
