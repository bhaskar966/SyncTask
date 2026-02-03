package com.bhaskar.synctask.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.data.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authManager: AuthManager,
    private val syncService: SyncService
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Start sync when authenticated
        viewModelScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        println("✅ User authenticated: ${state.uid}")
                        syncService.startRealtimeSync()
                    }
                    is AuthState.Unauthenticated -> {
                        println("❌ User not authenticated")
                        syncService.stopRealtimeSync()
                    }
                    else -> {}
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authManager.signInWithGoogle()

            result.fold(
                onSuccess = { userId ->
                    println("✅ Sign in successful: $userId")
                    // Auth state will change automatically
                },
                onFailure = { error ->
                    println("❌ Sign in failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Sign-in failed"
                }
            )

            _isLoading.value = false
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authManager.signInAnonymously()

            result.fold(
                onSuccess = { userId ->
                    println("✅ Anonymous sign in successful: $userId")
                },
                onFailure = { error ->
                    println("❌ Sign in failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Sign-in failed"
                }
            )

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
}