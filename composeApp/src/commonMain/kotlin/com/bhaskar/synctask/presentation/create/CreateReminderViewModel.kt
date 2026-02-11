@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.bhaskar.synctask.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.domain.RecurrenceUtils
import com.bhaskar.synctask.domain.generateUUID
import com.bhaskar.synctask.domain.model.*
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.domain.repository.GroupRepository
import com.bhaskar.synctask.domain.repository.TagRepository
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.bhaskar.synctask.presentation.create.components.*
import com.bhaskar.synctask.presentation.create.utils.ReminderUtils
import com.bhaskar.synctask.presentation.utils.atStartOfDay
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import com.bhaskar.synctask.presentation.utils.formatRecurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateReminderViewModel(
    private val reminderRepository: ReminderRepository,
    private val groupRepository: GroupRepository,
    private val tagRepository: TagRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReminderState())
    val state = _state.asStateFlow()

    private var editingReminderId: String? = null

    // Expose groups from repository
    val groups: StateFlow<List<ReminderGroup>> = authManager.authState
        .flatMapLatest { state ->
            val userId = if (state is AuthState.Authenticated) state.uid else "anonymous"
            groupRepository.getGroups(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose tags from repository
    val tags: StateFlow<List<Tag>> = authManager.authState
        .flatMapLatest { state ->
            val userId = if (state is AuthState.Authenticated) state.uid else "anonymous"
            tagRepository.getTags(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        resetState()
        
        viewModelScope.launch {
            subscriptionRepository.isPremiumSubscribed.collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    fun resetState() {
        val now = Clock.System.now()
        val futureTime = now.plus(kotlin.time.Duration.parse("12h"))
        val datetime = futureTime.toLocalDateTime(TimeZone.currentSystemDefault())
        editingReminderId = null
        _state.value = CreateReminderState(
            selectedDate = datetime.date,
            selectedTime = datetime.time,
            isPremium = subscriptionRepository.isPremiumSubscribed.value
        )
    }

    fun onEvent(event: CreateReminderEvent) {
        when (event) {
            // In OnTitleChanged
            is CreateReminderEvent.OnTitleChanged -> {
                _state.update { it.copy(title = event.title, titleError = null) }
            }

            // In OnDateSelected
            is CreateReminderEvent.OnDateSelected -> {
                _state.update {
                    val newDate = event.date
                    val oldDate = it.selectedDate
                    var rule = it.recurrence

                    if (rule != null) {
                        rule = when (rule) {
                            is RecurrenceRule.Weekly -> {
                                if (rule.interval == 1 && rule.daysOfWeek.size == 1 &&
                                    rule.daysOfWeek.first() == oldDate.dayOfWeek.isoDayNumber) {
                                    rule.copy(daysOfWeek = listOf(newDate.dayOfWeek.isoDayNumber))
                                } else rule
                            }
                            is RecurrenceRule.Monthly -> {
                                if (rule.interval == 1 && rule.dayOfMonth == oldDate.day) {
                                    rule.copy(dayOfMonth = newDate.day)
                                } else rule
                            }
                            is RecurrenceRule.Yearly -> {
                                if (rule.interval == 1 && rule.month == oldDate.month.number &&
                                    rule.dayOfMonth == oldDate.day) {
                                    rule.copy(
                                        month = newDate.month.number,
                                        dayOfMonth = newDate.day
                                    )
                                } else rule
                            }
                            else -> rule
                        }
                    }

                    it.copy(
                        selectedDate = newDate,
                        showDatePicker = false,
                        recurrence = rule,
                        recurrenceText = RecurrenceUtils.formatRecurrenceRule(rule),
                        dueDateTimeError = null
                    )
                }
            }

            // In OnTimeSelected
            is CreateReminderEvent.OnTimeSelected -> {
                _state.update { it.copy(selectedTime = event.time, showTimePicker = false, dueDateTimeError = null) }
            }

            // In OnDeadlineDateSelected
            is CreateReminderEvent.OnDeadlineDateSelected -> {
                _state.update { it.copy(deadlineDate = event.date, showDeadlineDatePicker = false, deadlineError = null) }
            }

            // In OnDeadlineTimeSelected
            is CreateReminderEvent.OnDeadlineTimeSelected -> {
                _state.update { it.copy(deadlineTime = event.time, showDeadlineTimePicker = false, deadlineError = null) }
            }

            // In OnReminderTimeModeChanged
            is CreateReminderEvent.OnReminderTimeModeChanged -> {
                _state.update { it.copy(reminderTimeMode = event.mode, reminderTimeError = null) }
            }

            // In OnBeforeDueOffsetChanged
            is CreateReminderEvent.OnBeforeDueOffsetChanged -> {
                _state.update { it.copy(beforeDueOffset = event.offsetMs, reminderTimeError = null) }
            }

            // In OnCustomReminderDateSelected
            is CreateReminderEvent.OnCustomReminderDateSelected -> {
                _state.update {
                    it.copy(
                        customReminderDate = event.date,
                        showCustomReminderDatePicker = false,
                        reminderTimeError = null
                    )
                }
            }

            // In OnCustomReminderTimeSelected
            is CreateReminderEvent.OnCustomReminderTimeSelected -> {
                _state.update {
                    it.copy(
                        customReminderTime = event.time,
                        showCustomReminderTimePicker = false,
                        reminderTimeError = null
                    )
                }
            }

            is CreateReminderEvent.OnDescriptionChanged -> {
                _state.update { it.copy(description = event.description) }
            }

            // Visual & Organization
            is CreateReminderEvent.OnIconSelected -> {
                _state.update { it.copy(icon = event.icon) }
            }

            is CreateReminderEvent.OnColorSelected -> {
                _state.update { it.copy(colorHex = event.color) }
            }

            is CreateReminderEvent.OnPinToggled -> {
                if (event.pinned) {
                    // Check limit before pinning
                    viewModelScope.launch {
                        val userId = authManager.currentUserId ?: "anonymous"
                        val currentPinnedCount = reminderRepository.getPinnedReminderCount(userId)
                        val isPremium = subscriptionRepository.isPremiumSubscribed.value // ✅ Get state
                        
                        if (SubscriptionConfig.canPinReminder(currentPinnedCount, isPremium)) {
                            _state.update { it.copy(isPinned = true) }
                        } else {
                            val message = if (isPremium) {
                                "You can only pin up to ${SubscriptionConfig.Limits.PREMIUM_MAX_PINNED_REMINDERS} reminders."
                            } else {
                                SubscriptionConfig.UpgradeMessages.PINNED
                            }
                            showPremiumDialog(message, isMaxLimit = isPremium)
                        }
                    }
                } else {
                    _state.update { it.copy(isPinned = false) }
                }
            }

            // Group autocomplete
            is CreateReminderEvent.OnGroupSearchQueryChanged -> {
                _state.update { it.copy(groupSearchQuery = event.query) }
            }

            is CreateReminderEvent.OnGroupSelected -> {
                _state.update {
                    it.copy(
                        selectedGroupId = event.groupId,
                        groupSearchQuery = ""
                    )
                }
            }

            is CreateReminderEvent.OnCreateGroup -> {
                if (!checkGroupLimit()) return

                val groupName = event.name.trim()
                if (groupName.isBlank()) return

                // Check if group already exists
                if (groups.value.any { it.name.equals(groupName, ignoreCase = true) }) {
                    return
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val newGroup = ReminderGroup(
                            id = Uuid.random().toString(),
                            userId = "", // Set by repository
                            name = groupName,
                            icon = "📁",
                            colorHex = "#6366F1",
                            order = groups.value.size,
                            createdAt = Clock.System.now().toEpochMilliseconds(),
                            lastModified = Clock.System.now().toEpochMilliseconds(),
                            isSynced = false
                        )
                        groupRepository.createGroup(newGroup)

                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    selectedGroupId = newGroup.id,
                                    groupSearchQuery = ""
                                )
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Failed to create group: ${e.message}")
                    }
                }
            }

            // Tag autocomplete
            is CreateReminderEvent.OnTagSearchQueryChanged -> {
                _state.update { it.copy(tagSearchQuery = event.query) }
            }

            is CreateReminderEvent.OnTagToggled -> {
                println("🏷️ ViewModel - OnTagToggled called with tagId: ${event.tagId}")
                println("🏷️ ViewModel - Current selectedTags: ${_state.value.selectedTags}")

                val newSelectedTags = if (_state.value.selectedTags.contains(event.tagId)) {
                    println("🏷️ ViewModel - Removing tag: ${event.tagId}")
                    _state.value.selectedTags - event.tagId
                } else {
                    println("🏷️ ViewModel - Adding tag: ${event.tagId}")
                    _state.value.selectedTags + event.tagId
                }

                println("🏷️ ViewModel - New selectedTags: $newSelectedTags")
                _state.update { it.copy(selectedTags = newSelectedTags) }
            }

            is CreateReminderEvent.OnCreateTag -> {
                if (!checkTagLimit()) return

                val tagName = event.name.trim()
                if (tagName.isBlank()) return

                // Check if tag already exists
                if (tags.value.any { it.name.equals(tagName, ignoreCase = true) }) {
                    return
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val newTag = Tag(
                            id = Uuid.random().toString(),
                            userId = "", // Set by repository
                            name = tagName,
                            colorHex = "#6366F1",
                            createdAt = Clock.System.now().toEpochMilliseconds(),
                            isSynced = false
                        )
                        tagRepository.createTag(newTag)

                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    selectedTags = it.selectedTags + newTag.id,
                                    tagSearchQuery = ""
                                )
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Failed to create tag: ${e.message}")
                    }
                }
            }

            // Subtasks
            is CreateReminderEvent.OnSubtaskInputChanged -> {
                _state.update { it.copy(subtaskInput = event.input) }
            }

            CreateReminderEvent.OnAddSubtask -> {
                if (!checkSubtaskLimit()) return

                val input = _state.value.subtaskInput.trim()
                if (input.isBlank()) return

                val newSubtask = SubTask(
                    id = Uuid.random().toString(),
                    title = input,
                    isCompleted = false,
                    order = _state.value.subtasks.size
                )
                _state.update {
                    it.copy(
                        subtasks = it.subtasks + newSubtask,
                        subtaskInput = ""
                    )
                }
            }

            is CreateReminderEvent.OnSubtaskToggled -> {
                _state.update {
                    it.copy(
                        subtasks = it.subtasks.map { subtask ->
                            if (subtask.id == event.subtaskId) {
                                subtask.copy(isCompleted = !subtask.isCompleted)
                            } else {
                                subtask
                            }
                        }
                    )
                }
            }

            is CreateReminderEvent.OnSubtaskDeleted -> {
                _state.update {
                    it.copy(subtasks = it.subtasks.filter { st -> st.id != event.subtaskId })
                }
            }

            // Priority & Recurrence
            is CreateReminderEvent.OnPrioritySelected -> {
                _state.update { it.copy(priority = event.priority) }
            }

            is CreateReminderEvent.OnRecurrenceTypeSelected -> {
                _state.update {
                    it.copy(
                        recurrenceType = event.type,
                        recurrence = if (event.type == RecurrenceType.NONE) null else it.recurrence
                    )
                }
            }

            is CreateReminderEvent.OnRecurrenceSelected -> {
                _state.update {
                    it.copy(
                        recurrence = event.recurrence,
                        recurrenceType = if (event.recurrence != null) RecurrenceType.CUSTOM else RecurrenceType.NONE,
                        showRecurrencePicker = false,
                        recurrenceText = RecurrenceUtils.formatRecurrenceRule(event.recurrence)
                    )
                }
                populateCustomRecurrenceFromRule(event.recurrence)
            }

            is CreateReminderEvent.OnDeadlineToggled -> {
                _state.update {
                    it.copy(
                        isDeadlineEnabled = event.enabled,
                        deadlineDate = if (event.enabled && it.deadlineDate == null)
                            Clock.System.todayIn(TimeZone.currentSystemDefault()) else it.deadlineDate,
                        deadlineTime = if (event.enabled && it.deadlineTime == null)
                            LocalTime(0, 0) else it.deadlineTime
                    )
                }
            }

            is CreateReminderEvent.OnHasSpecificTimeToggled -> {
                _state.update { it.copy(hasSpecificTime = event.enabled) }
            }

            CreateReminderEvent.OnToggleDatePicker -> {
                _state.update { it.copy(showDatePicker = !it.showDatePicker) }
            }

            CreateReminderEvent.OnToggleTimePicker -> {
                _state.update { it.copy(showTimePicker = !it.showTimePicker) }
            }

            CreateReminderEvent.OnToggleDeadlineDatePicker -> {
                _state.update { it.copy(showDeadlineDatePicker = !it.showDeadlineDatePicker) }
            }

            CreateReminderEvent.OnToggleDeadlineTimePicker -> {
                _state.update { it.copy(showDeadlineTimePicker = !it.showDeadlineTimePicker) }
            }

            CreateReminderEvent.OnToggleCustomReminderDatePicker -> {
                _state.update { it.copy(showCustomReminderDatePicker = !it.showCustomReminderDatePicker) }
            }

            CreateReminderEvent.OnToggleCustomReminderTimePicker -> {
                _state.update { it.copy(showCustomReminderTimePicker = !it.showCustomReminderTimePicker) }
            }

            CreateReminderEvent.OnToggleRecurrencePicker -> {
                _state.update { it.copy(showRecurrencePicker = !it.showRecurrencePicker) }
            }

            CreateReminderEvent.OnSave -> {
                saveReminder()
            }

            CreateReminderEvent.OnCustomRecurrenceToggled -> {
                _state.update {
                    it.copy(
                        customRecurrenceMode = !it.customRecurrenceMode,
                        recurrenceDayOfMonth = if (!it.customRecurrenceMode && it.recurrence == null) {
                            it.selectedDate.day
                        } else {
                            it.recurrenceDayOfMonth
                        },
                        recurrenceMonth = if (!it.customRecurrenceMode && it.recurrence == null) {
                            it.selectedDate.month.number
                        } else {
                            it.recurrenceMonth
                        }
                    )
                }
            }

            is CreateReminderEvent.OnRecurrenceFrequencyChanged -> {
                _state.update { it.copy(recurrenceFrequency = event.frequency) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceIntervalChanged -> {
                val newInterval = event.interval.coerceAtLeast(1)
                _state.update { it.copy(recurrenceInterval = newInterval) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceDayToggled -> {
                val newDays = if (event.day in _state.value.recurrenceSelectedDays) {
                    _state.value.recurrenceSelectedDays - event.day
                } else {
                    _state.value.recurrenceSelectedDays + event.day
                }
                _state.update { it.copy(recurrenceSelectedDays = newDays) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceDayOfMonthChanged -> {
                _state.update { it.copy(recurrenceDayOfMonth = event.day.coerceIn(1, 31)) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceMonthChanged -> {
                _state.update { it.copy(recurrenceMonth = event.month.coerceIn(1, 12)) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceEndModeChanged -> {
                _state.update { it.copy(recurrenceEndMode = event.mode) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceEndDateSelected -> {
                _state.update {
                    it.copy(
                        recurrenceEndDate = event.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                        showRecurrenceEndDatePicker = false
                    )
                }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceOccurrenceCountChanged -> {
                val newCount = event.count.coerceAtLeast(1)
                _state.update { it.copy(recurrenceOccurrenceCount = newCount) }
                buildCustomRecurrenceRule()
            }

            is CreateReminderEvent.OnRecurrenceFromCompletionToggled -> {
                _state.update { it.copy(recurrenceFromCompletion = event.enabled) }
                buildCustomRecurrenceRule()
            }

            CreateReminderEvent.OnToggleRecurrenceEndDatePicker -> {
                _state.update { it.copy(showRecurrenceEndDatePicker = !it.showRecurrenceEndDatePicker) }
            }

            CreateReminderEvent.OnToggleIconPicker -> {
                _state.update { it.copy(showIconPicker = !it.showIconPicker) }
            }

            CreateReminderEvent.OnToggleColorPicker -> {
                _state.update { it.copy(showColorPicker = !it.showColorPicker) }
            }
            
            // Premium Dialog Events
            CreateReminderEvent.OnDismissPremiumDialog -> {
                _state.update { it.copy(showPremiumDialog = false, premiumDialogMessage = "", isMaxLimitReached = false) }
            }
            
            CreateReminderEvent.OnNavigateToSubscription -> {
                _state.update { 
                    it.copy(
                        showPremiumDialog = false, 
                        premiumDialogMessage = "", 
                        isMaxLimitReached = false,
                        navigateToSubscription = true
                    ) 
                }
            }
            
            CreateReminderEvent.OnConsumeNavigateToSubscription -> {
                _state.update { it.copy(navigateToSubscription = false) }
            }
        }
    }
    
    // Helper function to show premium dialog
    private fun showPremiumDialog(message: String, isMaxLimit: Boolean = false) {
        _state.update { 
            it.copy(
                showPremiumDialog = true, 
                premiumDialogMessage = message,
                isMaxLimitReached = isMaxLimit
            ) 
        }
    }
    
    // Premium check functions
    fun canAddTag(): Boolean {
        val currentCount = tags.value.size
        val isPremium = subscriptionRepository.isPremiumSubscribed.value
        return SubscriptionConfig.canAddTag(currentCount, isPremium)
    }

    fun canAddGroup(): Boolean {
        val currentCount = groups.value.size
        val isPremium = subscriptionRepository.isPremiumSubscribed.value
        return SubscriptionConfig.canAddGroup(currentCount, isPremium)
    }
    
    fun canAddSubtask(): Boolean {
        val currentCount = _state.value.subtasks.size
        val isPremium = subscriptionRepository.isPremiumSubscribed.value
        return SubscriptionConfig.canAddSubtask(currentCount, isPremium)
    }
    
    // Called before adding a tag
    fun checkTagLimit(): Boolean {
        if (!canAddTag()) {
            val isPremium = subscriptionRepository.isPremiumSubscribed.value
            val message = if (isPremium) {
                "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_TAGS} tags."
            } else {
                SubscriptionConfig.UpgradeMessages.TAGS
            }
            showPremiumDialog(message, isMaxLimit = isPremium)
            return false
        }
        return true
    }

    // Called before adding a group
    fun checkGroupLimit(): Boolean {
         if (!canAddGroup()) {
            val isPremium = subscriptionRepository.isPremiumSubscribed.value
            val message = if (isPremium) {
                "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_GROUPS} groups."
            } else {
                SubscriptionConfig.UpgradeMessages.GROUPS
            }
            showPremiumDialog(message, isMaxLimit = isPremium)
            return false
        }
        return true
    }
    
    // Called before adding a subtask
    fun checkSubtaskLimit(): Boolean {
        if (!canAddSubtask()) {
            val isPremium = subscriptionRepository.isPremiumSubscribed.value
            val message = if (isPremium) {
                "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_SUBTASKS_PER_REMINDER} subtasks per reminder."
            } else {
                SubscriptionConfig.UpgradeMessages.SUBTASKS
            }
            showPremiumDialog(message, isMaxLimit = isPremium)
            return false
        }
        return true
    }


    private fun saveReminder() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!validateState(state.value)) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false) }
                }
                return@launch
            }

            val currentState = state.value

            // Check limits for NEW reminders
            if (editingReminderId == null) {
                val userId = authManager.currentUserId ?: "anonymous"
                val activeCount = reminderRepository.getActiveReminderCount(userId)
                val isPremium = subscriptionRepository.isPremiumSubscribed.value

                if (!SubscriptionConfig.canAddReminder(activeCount, isPremium)) {
                    val message = if (isPremium) {
                        "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_ACTIVE_REMINDERS} active reminders."
                    } else {
                        SubscriptionConfig.UpgradeMessages.REMINDERS
                    }
                    withContext(Dispatchers.Main) {
                        showPremiumDialog(message, isMaxLimit = isPremium)
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                _state.update { it.copy(isSaving = true) }
            }

            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val reminder = Reminder(
                    id = editingReminderId ?: generateUUID(),
                    userId = authManager.currentUserId ?: "anonymous",
                    title = currentState.title,
                    description = currentState.description.takeIf { it.isNotBlank() },
                    dueTime = currentState.getDueTime(),
                    reminderTime = currentState.getReminderTime(),
                    deadline = currentState.getDeadline(),
                    status = ReminderStatus.ACTIVE,
                    priority = currentState.priority,
                    recurrence = currentState.recurrence,
                    icon = currentState.icon,
                    colorHex = currentState.colorHex,
                    isPinned = currentState.isPinned,
                    groupId = currentState.selectedGroupId,
                    tagIds = currentState.selectedTags,
                    subtasks = currentState.subtasks,
                    targetRemindCount = currentState.recurrence?.occurrenceCount,
                    currentReminderCount = if (currentState.recurrence != null) 1 else null,
                    createdAt = now,
                    lastModified = now,
                    isSynced = false
                )

                println("💾 Reminder object created:")
                println("💾 - tagIds: ${reminder.tagIds}")
                println("💾 - groupId: ${reminder.groupId}")
                println("💾 - subtasks: ${reminder.subtasks}")

                if (editingReminderId != null) {
                     reminderRepository.updateReminder(reminder)
                } else {
                    reminderRepository.createReminder(reminder)
                }

                println("✅ Reminder saved!")
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false, isSaveSuccess = true) }
                }
            } catch (e: Exception) {
                println("❌ Error: ${e.message}")
                // Generic error fallback if needed, but we have specific fields now.
                 withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false) } // Just stop saving
                }
            }
        }
    }

    fun loadReminder(id: String) {
        // Reset state immediately to prevent flicker
        _state.update { it.copy(isSaveSuccess = false, isSaving = false) }
        
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = reminderRepository.getReminderById(id).firstOrNull() ?: return@launch
            editingReminderId = reminder.id

            val dueDateTime = Instant.fromEpochMilliseconds(reminder.dueTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        isEditing = true,
                        isSaving = false,
                        isSaveSuccess = false,
                        title = reminder.title,
                        description = reminder.description ?: "",
                        priority = reminder.priority,
                        selectedDate = dueDateTime.date,
                        selectedTime = dueDateTime.time,
                        hasSpecificTime = true,
                        recurrence = reminder.recurrence,
                        recurrenceType = if (reminder.recurrence != null) RecurrenceType.CUSTOM else RecurrenceType.NONE,
                        recurrenceText = RecurrenceUtils.formatRecurrenceRule(reminder.recurrence),
                        isDeadlineEnabled = reminder.deadline != null,
                        deadlineDate = reminder.deadline?.let { dl ->
                            Instant.fromEpochMilliseconds(dl)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        },
                        deadlineTime = reminder.deadline?.let { dl ->
                            Instant.fromEpochMilliseconds(dl)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).time
                        },
                        icon = reminder.icon,
                        colorHex = reminder.colorHex,
                        isPinned = reminder.isPinned,
                        selectedGroupId = reminder.groupId,
                        selectedTags = reminder.tagIds,
                        subtasks = reminder.subtasks,
                        
                        // Calculate Reminder Mode
                        reminderTimeMode = when {
                            reminder.reminderTime == null -> ReminderTimeMode.AT_DUE_TIME
                            reminder.reminderTime == reminder.dueTime -> ReminderTimeMode.AT_DUE_TIME
                            reminder.reminderTime < reminder.dueTime -> {
                                val offset = reminder.dueTime - reminder.reminderTime
                                if (ReminderUtils.COMMON_OFFSETS.contains(offset)) ReminderTimeMode.BEFORE_DUE_TIME else ReminderTimeMode.CUSTOM_TIME
                            }
                            else -> ReminderTimeMode.CUSTOM_TIME
                        },
                        beforeDueOffset = if (reminder.reminderTime != null && reminder.reminderTime < reminder.dueTime) {
                            val offset = reminder.dueTime - reminder.reminderTime
                            if (ReminderUtils.COMMON_OFFSETS.contains(offset)) offset else 3600000L // Default 1hr if custom or invalid
                        } else 3600000L,
                        customReminderDate = if (reminder.reminderTime != null) {
                            Instant.fromEpochMilliseconds(reminder.reminderTime).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        } else null,
                        customReminderTime = if (reminder.reminderTime != null) {
                            Instant.fromEpochMilliseconds(reminder.reminderTime).toLocalDateTime(TimeZone.currentSystemDefault()).time
                        } else null
                    )
                }
                populateCustomRecurrenceFromRule(reminder.recurrence)
            }
        }
    }

    private fun validateState(state: CreateReminderState): Boolean {
        var isValid = true
        val now = Clock.System.now().toEpochMilliseconds()
        var titleError: String? = null
        var dueDateTimeError: String? = null
        var deadlineError: String? = null
        var reminderTimeError: String? = null

        // 1. Title
        if (state.title.isBlank()) {
            titleError = "Title is required"
            isValid = false
        }

        val dueTime = state.getDueTime()
        // 2. Due Date/Time
        // If creating new, due time must be future.
        // If updating, allow past due time (can't validate against createdAt as we don't have it easily here, so we allow past).
        if (!state.isEditing && dueTime < now) {
            dueDateTimeError = "Due time must be in the future"
            isValid = false
        }

        // 3. Deadline
        val deadline = state.getDeadline()
        if (deadline != null && deadline < dueTime) {
            deadlineError = "Deadline cannot be before due time"
            isValid = false
        }

        // 4. Reminder Time
        // "remind me time/date should not exced duetime or deadline whichever is maximum"
        val reminderTime = state.getReminderTime()
        val maxTime = if (deadline != null && deadline > dueTime) deadline else dueTime
        
        if (reminderTime != null) {
             if (reminderTime > maxTime) {
                 reminderTimeError = "Reminder cannot be after due time/deadline"
                 isValid = false
             }
        }

        _state.update {
            it.copy(
                titleError = titleError,
                dueDateTimeError = dueDateTimeError,
                deadlineError = deadlineError,
                reminderTimeError = reminderTimeError
            )
        }
        return isValid
    }

    private fun buildCustomRecurrenceRule() {
        val s = _state.value
        val endDate = if (s.recurrenceEndMode == RecurrenceEndMode.DATE) s.recurrenceEndDate else null
        val count = if (s.recurrenceEndMode == RecurrenceEndMode.COUNT) s.recurrenceOccurrenceCount else null

        val rule = when (s.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> RecurrenceRule.Daily(
                interval = s.recurrenceInterval,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.WEEKLY -> RecurrenceRule.Weekly(
                interval = s.recurrenceInterval,
                daysOfWeek = s.recurrenceSelectedDays.toList().sorted(),
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.MONTHLY -> RecurrenceRule.Monthly(
                interval = s.recurrenceInterval,
                dayOfMonth = s.recurrenceDayOfMonth,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
            RecurrenceFrequency.YEARLY -> RecurrenceRule.Yearly(
                interval = s.recurrenceInterval,
                month = s.recurrenceMonth,
                dayOfMonth = s.recurrenceDayOfMonth,
                endDate = endDate,
                occurrenceCount = count,
                afterCompletion = s.recurrenceFromCompletion
            )
        }

        _state.update {
            it.copy(
                recurrence = rule,
                recurrenceText = formatRecurrence(rule)
            )
        }
    }

    private fun populateCustomRecurrenceFromRule(rule: RecurrenceRule?) {
        if (rule == null) return

        when (rule) {
            is RecurrenceRule.Daily -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.DAILY,
                        recurrenceInterval = rule.interval,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Weekly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.WEEKLY,
                        recurrenceInterval = rule.interval,
                        recurrenceSelectedDays = rule.daysOfWeek.toSet(),
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Monthly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.MONTHLY,
                        recurrenceInterval = rule.interval,
                        recurrenceDayOfMonth = rule.dayOfMonth,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.Yearly -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.YEARLY,
                        recurrenceInterval = rule.interval,
                        recurrenceMonth = rule.month,
                        recurrenceDayOfMonth = rule.dayOfMonth,
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
            is RecurrenceRule.CustomDays -> {
                _state.update {
                    it.copy(
                        recurrenceFrequency = RecurrenceFrequency.WEEKLY,
                        recurrenceInterval = rule.interval,
                        recurrenceSelectedDays = rule.daysOfWeek.toSet(),
                        recurrenceEndMode = when {
                            rule.endDate != null -> RecurrenceEndMode.DATE
                            rule.occurrenceCount != null -> RecurrenceEndMode.COUNT
                            else -> RecurrenceEndMode.NEVER
                        },
                        recurrenceEndDate = rule.endDate,
                        recurrenceOccurrenceCount = rule.occurrenceCount,
                        recurrenceFromCompletion = rule.afterCompletion
                    )
                }
            }
        }
    }
}
