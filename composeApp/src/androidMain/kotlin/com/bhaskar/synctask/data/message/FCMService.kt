package com.bhaskar.synctask.data.message

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.platform.PlatformFCMManager
import com.bhaskar.synctask.platform.FCMManager
import com.bhaskar.synctask.platform.NotificationScheduler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class FCMService : FirebaseMessagingService() {

    private val fcmManager: FCMManager by inject()
    private val authManager: AuthManager by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📬 FCM message received: ${message.data}")

        val action = message.data["action"] ?: return
        val reminderId = message.data["reminderId"] ?: return

        Log.d(TAG, "Action: $action, ReminderId: $reminderId")

        serviceScope.launch {
            try {
                when (action) {
                    // NOTIFICATION ACTIONS
                    "dismissed", "completed" -> {
                        // Just cancel notification
                        fcmManager.cancelNotification(reminderId)
                        Log.d(TAG, "✅ $action after FCM")
                    }

                    "snoozed" -> {
                        // Cancel notification and reschedule
                        fcmManager.cancelNotification(reminderId)
                        notificationScheduler.scheduleNext()
                        Log.d(TAG, "⏰ Snoozed after FCM")
                    }

                    "rescheduled" -> {
                        // Cancel old notification and reschedule all
                        fcmManager.cancelNotification(reminderId)
                        notificationScheduler.scheduleNext()
                        Log.d(TAG, "📅 Rescheduled after FCM")
                    }

                    // CRUD OPERATIONS
                    "reminder_created" -> {
                        // New reminder from another device - reschedule
                        notificationScheduler.scheduleNext()
                        Log.d(TAG, "➕ New reminder created, rescheduling")
                    }

                    "reminder_updated" -> {
                        // Reminder updated from another device - reschedule
                        notificationScheduler.scheduleNext()
                        Log.d(TAG, "✏️ Reminder updated, rescheduling")
                    }

                    "reminder_deleted" -> {
                        // Reminder deleted from another device
                        fcmManager.cancelNotification(reminderId)
                        notificationScheduler.scheduleNext()
                        Log.d(TAG, "🗑️ Reminder deleted, canceling and rescheduling")
                    }

                    else -> {
                        Log.w(TAG, "⚠️ Unknown action: $action")
                    }
                }

                // Notify callback for UI updates
                PlatformFCMManager.messageCallback?.invoke(reminderId, action)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing FCM", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM token: $token")

        serviceScope.launch {
            authManager.currentUserId?.let { userId ->
                try {
                    fcmManager.saveTokenToFirestore(userId, token)
                    Log.d(TAG, "✅ Token saved for user: $userId")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to save token", e)
                }
            }
        }
    }
}
