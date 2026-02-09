package com.bhaskar.synctask.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.history.components.HistoryEvent
import com.bhaskar.synctask.presentation.history.components.HistoryState
import com.bhaskar.synctask.presentation.history.components.HistoryTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val reminderRepository: ReminderRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val userIdFlow = authManager.authState.map {
        if (it is AuthState.Authenticated) it.uid else "anonymous"
    }

    init {
        viewModelScope.launch {
            val allRemindersFlow = userIdFlow.flatMapLatest { uid ->
                reminderRepository.getReminders()
            }

            combine(
                allRemindersFlow,
                _state.map { it.searchQuery }.distinctUntilChanged()
            ) { reminders, query ->
                
                // Categorize
                val completed = reminders
                    .filter { it.status == ReminderStatus.COMPLETED }
                    .sortedByDescending { it.completedAt ?: it.dueTime } // completedAt should be there, fallback dueTime
                
                val missed = reminders
                    .filter { it.status == ReminderStatus.MISSED }
                    .sortedByDescending { it.dueTime }
                
                val dismissed = reminders
                    .filter { it.status == ReminderStatus.DISMISSED }
                    .sortedByDescending { it.dueTime }
                
                // Global Search
                val searchResults = if (query.isNotBlank()) {
                    val q = query.lowercase()
                    reminders
                        .filter { 
                            (it.status == ReminderStatus.COMPLETED || 
                             it.status == ReminderStatus.MISSED || 
                             it.status == ReminderStatus.DISMISSED) && 
                            (it.title.lowercase().contains(q) || (it.description?.lowercase()?.contains(q) == true))
                        }
                        .sortedByDescending { it.completedAt ?: it.dueTime }
                } else {
                    emptyList()
                }

                HistoryStateData(completed, missed, dismissed, searchResults)
            }.collect { data ->
                _state.update {
                    it.copy(
                        completedReminders = data.completed,
                        missedReminders = data.missed,
                        dismissedReminders = data.dismissed,
                        searchResults = data.searchResults
                    )
                }
            }
        }
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.OnTabSelected -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }
            is HistoryEvent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is HistoryEvent.OnDeleteReminder -> {
                viewModelScope.launch {
                    reminderRepository.deleteReminder(event.reminderId)
                }
            }
        }
    }
}

// Helper to hold data in combine
private data class HistoryStateData(
    val completed: List<Reminder>,
    val missed: List<Reminder>,
    val dismissed: List<Reminder>,
    val searchResults: List<Reminder>
)
