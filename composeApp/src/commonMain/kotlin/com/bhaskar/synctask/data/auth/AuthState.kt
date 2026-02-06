package com.bhaskar.synctask.data.auth

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?
    ) : AuthState()
}