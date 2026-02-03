package com.bhaskar.synctask.data.platform

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.bhaskar.synctask.platform.FCMManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformFCMManager(
    private val context: Context
) : FCMManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val fcmMessaging = FirebaseMessaging.getInstance()

    companion object {
        private const val TAG = "PlatformFCMManager"
        var messageCallback: ((String, String) -> Unit)? = null
    }

    actual override suspend fun getToken(): String? = suspendCoroutine { continuation ->
        fcmMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "✅ FCM token retrieved: $token")
                continuation.resume(token)
            } else {
                Log.e(TAG, "❌ Failed to get FCM token", task.exception)
                continuation.resume(null)
            }
        }
    }

    actual override suspend fun saveTokenToFirestore(userId: String, token: String) {
        try {
            println("📝 saveTokenToFirestore() called")
            println("   User ID: $userId")
            println("   Token: ${token.take(20)}...")

            // Get Installation ID
            val installationId = FirebaseInstallations.getInstance().id.await()
            println("   Installation ID: ${installationId.take(20)}...")

            val deviceModel = android.os.Build.MODEL

            val tokenData = hashMapOf(
                "token" to token,
                "platform" to "android",
                "installationId" to installationId,
                "deviceModel" to deviceModel,
                "lastUpdated" to System.currentTimeMillis()
            )

            println("   Firestore path: users/$userId/fcmTokens/$installationId")

            // Save to Firestore
            firestore.collection("users")
                .document(userId)
                .collection("fcmTokens")
                .document(installationId)
                .set(tokenData)
                .await()

            println("✅ Android: FCM token saved to Firestore for user: $userId")

        } catch (e: Exception) {
            println("❌ Failed to save FCM token to Firestore: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


    actual override fun setupMessageListener(onMessageReceived: (reminderId: String, action: String) -> Unit) {
        messageCallback = onMessageReceived
        Log.d(TAG, "✅ FCM message listener setup")
    }

    actual override fun cancelNotification(reminderId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(reminderId.hashCode())
            Log.d(TAG, "✅ Notification cancelled: $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel notification", e)
        }
    }
}