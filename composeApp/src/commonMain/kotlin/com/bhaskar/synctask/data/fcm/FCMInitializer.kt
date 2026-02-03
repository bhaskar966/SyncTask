package com.bhaskar.synctask.data.fcm

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.platform.FCMManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FCMInitializer(
    private val authManager: AuthManager,
    private val fcmManager: FCMManager,
    private val scope: CoroutineScope
) {

    init {
        println("🎬 FCMInitializer created!")
        initialize()
    }

    private fun initialize() {
        println("🔧 FCMInitializer.initialize() called")

        // Setup FCM message listener
        fcmManager.setupMessageListener { reminderId, action ->
            println("📬 FCM message received in app: $action for $reminderId")
        }

        // Check if ALREADY authenticated
        scope.launch {
            val currentState = authManager.authState.value
            println("🔍 Current auth state: $currentState")

            if (currentState is AuthState.Authenticated) {
                println("🔑 Already authenticated, saving FCM token immediately")
                saveFCMToken(currentState.uid)
            } else {
                println("⏳ Not authenticated yet, waiting...")
            }
        }

        // Also listen for future auth changes
        scope.launch {
            authManager.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    println("🔑 User authenticated: ${state.uid}")
                    saveFCMToken(state.uid)
                }
            }
        }
    }

    private suspend fun saveFCMToken(userId: String) {
        try {
            println("🔄 Attempting to get FCM token...")
            val token = fcmManager.getToken()

            if (token != null) {
                println("✅ Got FCM token: ${token.take(20)}...")
                fcmManager.saveTokenToFirestore(userId, token)
                println("✅ FCM token saved for user: $userId")
            } else {
                println("⚠️ FCM token is null")
            }
        } catch (e: Exception) {
            println("❌ Failed to save FCM token: ${e.message}")
            e.printStackTrace()
        }
    }
}