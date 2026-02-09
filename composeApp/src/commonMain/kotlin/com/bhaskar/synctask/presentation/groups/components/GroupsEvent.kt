package com.bhaskar.synctask.presentation.groups.components

import com.bhaskar.synctask.domain.model.ReminderGroup

sealed class GroupsEvent {
    // Dialog events
    data object ShowCreateDialog : GroupsEvent()
    data class ShowEditDialog(val group: ReminderGroup) : GroupsEvent()
    data object HideDialog : GroupsEvent()

    // Dialog input events
    data class UpdateDialogName(val name: String) : GroupsEvent()
    data class UpdateDialogIcon(val icon: String) : GroupsEvent()
    data class UpdateDialogColor(val color: String) : GroupsEvent()

    // Dialog picker toggles
    data object ToggleIconPicker : GroupsEvent()
    data object ToggleColorPicker : GroupsEvent()

    // CRUD events
    data object SaveGroup : GroupsEvent()
    data class DeleteGroup(val groupId: String) : GroupsEvent()
    data class DeleteReminder(val reminderId: String) : GroupsEvent()
    data class TogglePin(val reminder: com.bhaskar.synctask.domain.model.Reminder) : GroupsEvent()
    data class UpdateReminderStatus(val reminderId: String, val isCompleted: Boolean) : GroupsEvent()
    data class UpdateSubtaskStatus(val reminder: com.bhaskar.synctask.domain.model.Reminder, val subtask: com.bhaskar.synctask.domain.model.SubTask, val isCompleted: Boolean) : GroupsEvent()

    // Expand/collapse groups
    data class ToggleGroupExpanded(val groupId: String) : GroupsEvent()

    // Premium dialog events
    data object DismissPremiumDialog : GroupsEvent()
    data object NavigateToSubscription : GroupsEvent()

    // Navigate to reminder detail
    data class NavigateToReminder(val reminderId: String) : GroupsEvent()
    
    // Search
    data class OnSearchQueryChanged(val query: String) : GroupsEvent()
}