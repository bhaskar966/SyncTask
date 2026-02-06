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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GroupsViewModel(
    private val groupRepository: GroupRepository,
    private val reminderRepository: ReminderRepository,
    private val subscriptionRepository: SubscriptionRepository, // ✅ Inject this
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.asStateFlow()

    private val userId: String
        get() = authManager.currentUserId ?: "anonymous"

    // ✅ Get groups from repository
    val groups: StateFlow<List<ReminderGroup>> = authManager.authState
        .map { state ->
            if (state is AuthState.Authenticated) state.uid else "anonymous"
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "anonymous")
        .let { userIdFlow ->
            groupRepository.getGroups(userIdFlow.value)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
        }

    // ✅ NEW: Load reminders for each group
    fun getRemindersForGroup(groupId: String): StateFlow<List<Reminder>> {
        return reminderRepository.getRemindersByGroup(userId, groupId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    // ✅ Expose premium state
    val isPremium: StateFlow<Boolean> = subscriptionRepository.isPremiumSubscribed

    // ✅ NEW: Load ungrouped reminders
    val ungroupedReminders: StateFlow<List<Reminder>> = reminderRepository.getUngroupedReminders(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onEvent(event: GroupsEvent) {
        when (event) {
            is GroupsEvent.ShowCreateDialog -> {
                // ✅ Check premium limit before showing dialog
                viewModelScope.launch {
                    val currentCount = groupRepository.getGroupCount(userId)
                    // ✅ Observe premium state directly from repo
                    val isPremium = subscriptionRepository.isPremiumSubscribed.value
                    
                    if (SubscriptionConfig.canAddGroup(currentCount, isPremium)) {
                        _state.update {
                            it.copy(
                                isDialogVisible = true,
                                editingGroup = null,
                                dialogName = "",
                                dialogIcon = "📁",
                                dialogColor = "#6366F1"
                            )
                        }
                    } else {
                        // Show premium dialog
                        _state.update {
                            it.copy(
                                showPremiumDialog = true,
                                premiumDialogMessage = SubscriptionConfig.UpgradeMessages.GROUPS
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
                        dialogColor = "#6366F1",
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
                _state.update { it.copy(showPremiumDialog = false, premiumDialogMessage = "") }
            }

            // Navigate to subscription (handled in UI)
            GroupsEvent.NavigateToSubscription -> {
                // Will be handled in the composable
            }

            // Navigate to reminder detail (handled in UI)
            is GroupsEvent.NavigateToReminder -> {
                // Will be handled in the composable
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
                        order = groups.value.size,
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
                        dialogColor = "#6366F1",
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

    // Get reminder count for a specific group (for badge)
    suspend fun getReminderCount(groupId: String): Int {
        return reminderRepository.getReminderCountByGroup(groupId)
    }
}
