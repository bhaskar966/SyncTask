package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.platform.FCMManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import platform.Foundation.NSLog
import platform.UIKit.UIDevice
import kotlin.time.Clock

actual class PlatformFCMManager : FCMManager {

    companion object {
        var messageCallback: ((String, String) -> Unit)? = null
        var cachedToken: String? = null
    }

    actual override suspend fun getToken(): String? {
        NSLog("📱 iOS: Getting FCM token")
        return cachedToken
    }

    actual override suspend fun saveTokenToFirestore(userId: String, token: String) {
        NSLog("💾 iOS: Saving token to Firestore for user: $userId")

        try {
            val firestore = Firebase.firestore

            val installationId = token.take(20)
            val deviceModel = UIDevice.currentDevice.model

            val tokenData = mapOf(
                "token" to token,
                "platform" to "ios",
                "installationId" to installationId,
                "deviceModel" to deviceModel,
                "lastUpdated" to Clock.System.now().toEpochMilliseconds()
            )

            firestore
                .collection("users")
                .document(userId)
                .collection("fcmTokens")
                .document(installationId)
                .set(tokenData)

            NSLog("✅ iOS: FCM token saved to Firestore")
            cachedToken = token

        } catch (e: Exception) {
            NSLog("❌ iOS: Failed to save FCM token: ${e.message}")
            throw e
        }
    }


    actual override fun setupMessageListener(onMessageReceived: (reminderId: String, action: String) -> Unit) {
        NSLog("👂 iOS: Setting up FCM message listener")
        messageCallback = onMessageReceived
    }

    actual override fun cancelNotification(reminderId: String) {
        NSLog("🗑️ iOS: Cancelling notification: $reminderId")

        // Trigger Swift bridge via shared state
        FCMBridgeState.reminderIdToCancel = reminderId
        FCMBridgeState.shouldCancelNotification = true
    }
}

object FCMBridgeState {
    var userId: String? = null
    var token: String? = null
    var shouldSaveToken: Boolean = false
    var reminderIdToCancel: String? = null
    var shouldCancelNotification: Boolean = false

    // Called from Swift when token is received
    @Suppress("unused")
    fun onTokenReceived(token: String) {
        PlatformFCMManager.cachedToken = token
        NSLog("✅ iOS: FCM token cached: $token")
    }

    // Called from Swift when FCM message is received
    @Suppress("unused")
    fun onMessageReceived(reminderId: String, action: String) {
        NSLog("📬 iOS: Handling FCM message - Action: $action, ReminderId: $reminderId")
        PlatformFCMManager.messageCallback?.invoke(reminderId, action)
    }
}