package com.bhaskar.synctask.platform

interface FCMManager {
    /**
     * Get the current FCM token
     */
    suspend fun getToken(): String?

    /**
     * Save FCM token to Firestore for this device
     */
    suspend fun saveTokenToFirestore(userId: String, token: String)

    /**
     * Setup listener for incoming FCM messages
     * @param onMessageReceived callback with (reminderId, action)
     */
    fun setupMessageListener(onMessageReceived: (reminderId: String, action: String) -> Unit)

    /**
     * Cancel/remove a notification from the notification tray
     */
    fun cancelNotification(reminderId: String)
}
