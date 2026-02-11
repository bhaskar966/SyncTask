package com.bhaskar.synctask.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class OnboardingState(
    val isCompleted: Boolean? = null
)

sealed class OnboardingEvent {
    data object CompleteOnboarding : OnboardingEvent()
}

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            onboardingRepository.isOnboardingCompleted.collectLatest { completed ->
                _state.value = _state.value.copy(isCompleted = completed)
            }
        }
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.CompleteOnboarding -> {
                viewModelScope.launch {
                    onboardingRepository.setOnboardingCompleted()
                }
            }
        }
    }
}
