package com.bhaskar.synctask.data.auth

import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class AuthManager(
    private val googleAuthenticator: GoogleAuthenticator
) {
    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    // Lazy injection to break circular dependency - SubscriptionRepository is resolved only when needed
    private val subscriptionRepository: SubscriptionRepository? by lazy {
        try {
            KoinPlatform.getKoin().getOrNull()
        } catch (e: Exception) {
            println("⚠️ SubscriptionRepository not available: ${e.message}")
            null
        }
    }

    init {
        // Listen to Firebase auth state changes
        CoroutineScope(Dispatchers.Default).launch {
            auth.authStateChanged.collect { firebaseUser ->
                _authState.value = if (firebaseUser != null) {
                    AuthState.Authenticated(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName,
                        email = firebaseUser.email
                    )
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    // Google Sign-In
    suspend fun signInWithGoogle(): Result<String> {
        return try {
            val googleUserResult = googleAuthenticator.signIn()

            googleUserResult.fold(
                onSuccess = { googleUser ->
                    // Firebase accepts null for accessToken when using idToken
                    val credential = GoogleAuthProvider.credential(
                        googleUser.idToken,
                        googleUser.accessToken.ifEmpty { null }
                    )
                    val result = auth.signInWithCredential(credential)
                    val userId = result.user?.uid ?: throw Exception("No user ID")
                    println("✅ Firebase: Google Sign-In successful - $userId")
                    
                    // Sync RevenueCat with Firebase user ID for cross-device subscription sync
                    try {
                        subscriptionRepository?.loginUser(userId)
                    } catch (e: Exception) {
                        println("⚠️ RevenueCat login sync failed: ${e.message}")
                        // Don't fail the sign-in if RevenueCat sync fails
                    }
                    
                    Result.success(userId)
                },
                onFailure = { error ->
                    println("❌ Google Sign-In failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ Firebase auth failed: ${e.message}")
            Result.failure(e)
        }
    }


    // Anonymous Sign-In (fallback)
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously()
            val userId = result.user?.uid ?: throw Exception("No user ID")
            println("✅ Firebase: Anonymous sign in - $userId")
            
            // Sync RevenueCat with anonymous Firebase user ID
            try {
                subscriptionRepository?.loginUser(userId)
            } catch (e: Exception) {
                println("⚠️ RevenueCat login sync failed: ${e.message}")
            }
            
            Result.success(userId)
        } catch (e: Exception) {
            println("❌ Anonymous sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            // Logout from RevenueCat first to reset subscription state
            try {
                subscriptionRepository?.logoutUser()
            } catch (e: Exception) {
                println("⚠️ RevenueCat logout sync failed: ${e.message}")
            }
            
            auth.signOut()
            println("✅ Signed out")
        } catch (e: Exception) {
            println("❌ Sign out failed: ${e.message}")
        }
    }
}