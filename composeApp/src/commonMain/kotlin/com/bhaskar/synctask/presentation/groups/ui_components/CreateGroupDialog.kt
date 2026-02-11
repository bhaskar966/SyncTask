package com.bhaskar.synctask.presentation.groups.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bhaskar.synctask.presentation.create.ui_components.IconPickerDialog
import com.bhaskar.synctask.presentation.groups.components.GroupsEvent
import com.bhaskar.synctask.presentation.groups.components.GroupsState

@Composable
fun CreateGroupDialog(
    state: GroupsState,
    onEvent: (GroupsEvent) -> Unit,
    onDismiss: () -> Unit
) {
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                Text(
                    text = if (state.editingGroup != null) "Edit Group" else "Create Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name Input
                TextField(
                    value = state.dialogName,
                    onValueChange = { onEvent(GroupsEvent.UpdateDialogName(it)) },
                    placeholder = { Text("Group Name", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                Spacer(Modifier.height(12.dp))
                
                // Decorations Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Emoji
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { onEvent(GroupsEvent.ToggleIconPicker) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.dialogIcon.ifBlank { "📁" }, style = MaterialTheme.typography.titleMedium)
                    }
                    
                    // Color Picker
                    com.bhaskar.synctask.presentation.create.ui_components.ColorPickerAnchor(
                        selectedColorHex = state.dialogColor,
                        onColorSelected = { onEvent(GroupsEvent.UpdateDialogColor(it)) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onEvent(GroupsEvent.SaveGroup) },
                        enabled = state.dialogName.isNotBlank() && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
