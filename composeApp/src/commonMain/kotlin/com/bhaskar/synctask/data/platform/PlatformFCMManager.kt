package com.bhaskar.synctask.data.platform

import com.bhaskar.synctask.platform.FCMManager

expect class PlatformFCMManager : FCMManager {
    override suspend fun getToken(): String?
    override suspend fun saveTokenToFirestore(userId: String, token: String)
    override fun setupMessageListener(onMessageReceived: (reminderId: String, action: String) -> Unit)
    override fun cancelNotification(reminderId: String)
}