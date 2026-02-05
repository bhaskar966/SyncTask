package com.bhaskar.synctask.presentation.groups.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bhaskar.synctask.presentation.create.ui_components.ColorPickerDialog
import com.bhaskar.synctask.presentation.create.ui_components.IconPickerDialog
import com.bhaskar.synctask.presentation.groups.components.GroupsEvent
import com.bhaskar.synctask.presentation.groups.components.GroupsState
import com.bhaskar.synctask.presentation.utils.parseHexColor

@Composable
fun CreateGroupDialog(
    state: GroupsState,
    onEvent: (GroupsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    // Show icon picker
    if (state.showIconPicker) {
        IconPickerDialog(
            selectedIcon = state.dialogIcon,
            onIconSelected = { icon ->
                icon?.let { onEvent(GroupsEvent.UpdateDialogIcon(it)) }
                onEvent(GroupsEvent.ToggleIconPicker)
            },
            onDismiss = { onEvent(GroupsEvent.ToggleIconPicker) }
        )
    }

    // Show color picker
    if (state.showColorPicker) {
        ColorPickerDialog(
            selectedColor = state.dialogColor,
            onColorSelected = { color ->
                color?.let { onEvent(GroupsEvent.UpdateDialogColor(it)) }
                onEvent(GroupsEvent.ToggleColorPicker)
            },
            onDismiss = { onEvent(GroupsEvent.ToggleColorPicker) }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.editingGroup != null) "Edit Group" else "Create Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Preview Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = parseHexColor(state.dialogColor).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon preview
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(state.dialogColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.dialogIcon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        // Name preview
                        Text(
                            text = state.dialogName.ifBlank { "Group Name" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.dialogName.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Name Input
                OutlinedTextField(
                    value = state.dialogName,
                    onValueChange = { onEvent(GroupsEvent.UpdateDialogName(it)) },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g., Work, Personal, Shopping") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.error != null
                )

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Icon and Color Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Icon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedButton(
                            onClick = { onEvent(GroupsEvent.ToggleIconPicker) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.dialogIcon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }

                    // Color Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Color",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedButton(
                            onClick = { onEvent(GroupsEvent.ToggleColorPicker) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(state.dialogColor))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onEvent(GroupsEvent.SaveGroup) },
                        modifier = Modifier.weight(1f),
                        enabled = state.dialogName.isNotBlank() && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (state.editingGroup != null) "Update" else "Create")
                        }
                    }
                }
            }
        }
    }
}
