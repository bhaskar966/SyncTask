@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bhaskar.synctask.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.repository.GroupRepository
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.bhaskar.synctask.presentation.groups.components.GroupsEvent
import com.bhaskar.synctask.presentation.groups.components.GroupsState
import com.bhaskar.synctask.presentation.groups.components.GroupWithReminders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GroupsViewModel(
    private val groupRepository: GroupRepository,
    private val reminderRepository: ReminderRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.asStateFlow()

    private val userId: String
        get() = authManager.currentUserId ?: "anonymous"

    init {
        viewModelScope.launch {
            val userIdFlow = authManager.authState.map {
                if (it is AuthState.Authenticated) it.uid else "anonymous"
            }

            val groupsWithRemindersFlow = userIdFlow.flatMapLatest { uid ->
                groupRepository.getGroups(uid).flatMapLatest { groups ->
                    if (groups.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
                    else {
                        val flows = groups.map { group ->
                            reminderRepository.getRemindersByGroup(uid, group.id)
                                .map { GroupWithReminders(group, it) }
                        }
                        combine(flows) { args -> args.toList() }
                    }
                }
            }

            val ungroupedFlow = userIdFlow.flatMapLatest { uid ->
                reminderRepository.getUngroupedReminders(uid)
            }

            combine(
                groupsWithRemindersFlow,
                ungroupedFlow,
                subscriptionRepository.isPremiumSubscribed,
                _state.map { it.searchQuery }.distinctUntilChanged()
            ) { groupsWithReminders, ungrouped, isPremium, query ->
                val filteredGroups = if (query.isBlank()) {
                    groupsWithReminders
                } else {
                    groupsWithReminders.mapNotNull { groupItem ->
                        val groupMatches = groupItem.group.name.contains(query, ignoreCase = true)
                        val matchingReminders = groupItem.reminders.filter {
                            it.title.contains(query, ignoreCase = true)
                        }

                        if (groupMatches) {
                            groupItem
                        } else if (matchingReminders.isNotEmpty()) {
                            groupItem.copy(reminders = matchingReminders)
                        } else {
                            null
                        }
                    }
                }

                val filteredUngrouped = if (query.isBlank()) {
                    ungrouped
                } else {
                    ungrouped.filter { it.title.contains(query, ignoreCase = true) }
                }

                Triple(filteredGroups, filteredUngrouped, isPremium)
            }.collect { (groups, ungrouped, isPremium) ->
                _state.update {
                    it.copy(
                        groupsWithReminders = groups,
                        ungroupedReminders = ungrouped,
                        isPremium = isPremium
                    )
                }
            }
        }
    }

    fun onEvent(event: GroupsEvent) {
        when (event) {
            is GroupsEvent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }

            is GroupsEvent.ShowCreateDialog -> {
                // Check premium limit before showing dialog
                viewModelScope.launch {
                    val currentCount = groupRepository.getGroupCount(userId)
                    // Observe premium state directly from repo
                    val isPremium = subscriptionRepository.isPremiumSubscribed.value
                    
                    if (SubscriptionConfig.canAddGroup(currentCount, isPremium)) {
                        _state.update {
                            it.copy(
                                isDialogVisible = true,
                                editingGroup = null,
                                dialogName = "",
                                dialogIcon = "📁",
                                dialogColor = "#6B7280"
                            )
                        }
                    } else {
                        // Show premium/limit dialog
                        val message = if (isPremium) {
                            "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_GROUPS} groups."
                        } else {
                            SubscriptionConfig.UpgradeMessages.GROUPS
                        }
                        
                        _state.update {
                            it.copy(
                                showPremiumDialog = true,
                                premiumDialogMessage = message,
                                isMaxLimitReached = isPremium
                            )
                        }
                    }
                }
            }

            is GroupsEvent.ShowEditDialog -> {
                _state.update {
                    it.copy(
                        isDialogVisible = true,
                        editingGroup = event.group,
                        dialogName = event.group.name,
                        dialogIcon = event.group.icon,
                        dialogColor = event.group.colorHex
                    )
                }
            }

            is GroupsEvent.HideDialog -> {
                _state.update {
                    it.copy(
                        isDialogVisible = false,
                        editingGroup = null,
                        dialogName = "",
                        dialogIcon = "📁",
                        dialogColor = "#6B7280",
                        showIconPicker = false,
                        showColorPicker = false
                    )
                }
            }

            is GroupsEvent.UpdateDialogName -> {
                _state.update { it.copy(dialogName = event.name) }
            }

            is GroupsEvent.UpdateDialogIcon -> {
                _state.update { it.copy(dialogIcon = event.icon) }
            }

            is GroupsEvent.UpdateDialogColor -> {
                _state.update { it.copy(dialogColor = event.color) }
            }

            // Toggle icon picker
            GroupsEvent.ToggleIconPicker -> {
                _state.update { it.copy(showIconPicker = !it.showIconPicker) }
            }

            //  Toggle color picker
            GroupsEvent.ToggleColorPicker -> {
                _state.update { it.copy(showColorPicker = !it.showColorPicker) }
            }

            is GroupsEvent.SaveGroup -> {
                saveGroup()
            }

            is GroupsEvent.DeleteGroup -> {
                deleteGroup(event.groupId)
            }

            is GroupsEvent.DeleteReminder -> {
                deleteReminder(event.reminderId)
            }

            is GroupsEvent.UpdateReminderStatus -> {
                updateReminderStatus(event.reminderId, event.isCompleted)
            }

            is GroupsEvent.UpdateSubtaskStatus -> {
                updateSubtaskStatus(event.reminder, event.subtask, event.isCompleted)
            }

            // Toggle group expanded state
            is GroupsEvent.ToggleGroupExpanded -> {
                _state.update { currentState ->
                    val newExpandedIds = if (event.groupId in currentState.expandedGroupIds) {
                        currentState.expandedGroupIds - event.groupId
                    } else {
                        currentState.expandedGroupIds + event.groupId
                    }
                    currentState.copy(expandedGroupIds = newExpandedIds)
                }
            }

            // Dismiss premium dialog
            GroupsEvent.DismissPremiumDialog -> {
                _state.update { it.copy(showPremiumDialog = false, premiumDialogMessage = "", isMaxLimitReached = false) }
            }

            // Navigate to subscription (handled in UI)
            GroupsEvent.NavigateToSubscription -> {
                // Will be handled in the composable
            }

            // Navigate to reminder detail (handled in UI)
            is GroupsEvent.NavigateToReminder -> {
                // Will be handled in the composable
            }

            is GroupsEvent.TogglePin -> {
                viewModelScope.launch {
                    val reminder = event.reminder
                    if (reminder.isPinned) {
                        // Unpinning is always allowed
                        reminderRepository.updateReminder(reminder.copy(isPinned = false))
                    } else {
                        // Pinning requires check
                        val currentPinnedCount = reminderRepository.getPinnedReminderCount(reminder.userId)
                        val isPremium = subscriptionRepository.isPremiumSubscribed.value
                        
                        if (SubscriptionConfig.canPinReminder(currentPinnedCount, isPremium)) {
                            reminderRepository.updateReminder(reminder.copy(isPinned = true))
                        } else {
                            val message = if (isPremium) {
                                "You can only pin up to ${SubscriptionConfig.Limits.PREMIUM_MAX_PINNED_REMINDERS} reminders."
                            } else {
                                SubscriptionConfig.UpgradeMessages.PINNED
                            }
                            
                            _state.update {
                                it.copy(
                                    showPremiumDialog = true,
                                    premiumDialogMessage = message,
                                    isMaxLimitReached = isPremium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateReminderStatus(reminderId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                if (isCompleted) {
                     reminderRepository.completeReminder(reminderId)
                } else {
                     // We need to un-complete. Since repository doesn't have explicit uncomplete,
                     // we fetch and update.
                     val reminder = reminderRepository.getReminderById(reminderId).firstOrNull()
                     if (reminder != null) {
                         reminderRepository.updateReminder(reminder.copy(status = com.bhaskar.synctask.domain.model.ReminderStatus.ACTIVE, completedAt = null))
                     }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateSubtaskStatus(reminder: Reminder, subtask: com.bhaskar.synctask.domain.model.SubTask, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                 val updatedSubtasks = reminder.subtasks.map {
                     if (it.id == subtask.id) it.copy(isCompleted = isCompleted) else it
                 }
                 reminderRepository.updateReminder(reminder.copy(subtasks = updatedSubtasks))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun saveGroup() {
        val currentState = _state.value

        if (currentState.dialogName.isBlank()) {
            _state.update { it.copy(error = "Group name cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val now = Clock.System.now().toEpochMilliseconds()

                if (currentState.editingGroup != null) {
                    // Update existing group
                    val updated = currentState.editingGroup.copy(
                        name = currentState.dialogName.trim(),
                        icon = currentState.dialogIcon,
                        colorHex = currentState.dialogColor,
                        lastModified = now
                    )
                    groupRepository.updateGroup(updated)
                } else {
                    // Create new group
                    val newGroup = ReminderGroup(
                        id = Uuid.random().toString(),
                        userId = "", // Will be set by repository
                        name = currentState.dialogName.trim(),
                        icon = currentState.dialogIcon,
                        colorHex = currentState.dialogColor,
                        order = currentState.groupsWithReminders.size,
                        createdAt = now,
                        lastModified = now,
                        isSynced = false
                    )
                    groupRepository.createGroup(newGroup)
                }

                // Hide dialog
                _state.update {
                    it.copy(
                        isDialogVisible = false,
                        editingGroup = null,
                        dialogName = "",
                        dialogIcon = "📁",
                        dialogColor = "#6B7280",
                        showIconPicker = false,
                        showColorPicker = false,
                        error = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to save group",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                groupRepository.deleteGroup(userId, groupId)

                // Remove from expanded list if it was expanded
                _state.update {
                    it.copy(
                        expandedGroupIds = it.expandedGroupIds - groupId,
                        error = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to delete group",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                reminderRepository.deleteReminder(reminderId)
            } catch (e: Exception) {
               // Handle error ideally
            }
        }
    }

    // Get reminder count for a specific group (for badge)
    suspend fun getReminderCount(groupId: String): Int {
        return reminderRepository.getReminderCountByGroup(groupId)
    }
}
