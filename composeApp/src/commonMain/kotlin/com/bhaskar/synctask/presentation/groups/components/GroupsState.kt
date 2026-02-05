package com.bhaskar.synctask.presentation.groups.components

import com.bhaskar.synctask.domain.model.ReminderGroup

data class GroupsState(
    val isDialogVisible: Boolean = false,
    val editingGroup: ReminderGroup? = null,
    val dialogName: String = "",
    val dialogIcon: String = "📁",
    val dialogColor: String = "#6366F1",
    val error: String? = null,

    // Icon & Color picker states for dialog
    val showIconPicker: Boolean = false,
    val showColorPicker: Boolean = false,

    // Premium dialog
    val showPremiumDialog: Boolean = false,
    val premiumDialogMessage: String = "",

    // Expanded groups tracking (for expand/collapse)
    val expandedGroupIds: Set<String> = emptySet(),

    // Loading states
    val isLoading: Boolean = false,
)