package com.bhaskar.synctask.data.auth

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume

actual class GoogleAuthenticator {

    private val clientId: String by lazy {
        NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") as? String
            ?: error("GIDClientID not found in Info.plist")
    }

    actual suspend fun signIn(): Result<GoogleUser> = suspendCancellableCoroutine { continuation ->
        GoogleSignInState.continuation = continuation
        GoogleSignInState.clientId = clientId
        GoogleSignInState.shouldTrigger = true
    }
}

object GoogleSignInState {
    internal var continuation: CancellableContinuation<Result<GoogleUser>>? = null

    @Volatile
    var clientId: String? = null

    @Volatile
    var shouldTrigger: Boolean = false

    @Suppress("unused")
    fun onSuccess(idToken: String, accessToken: String, displayName: String?, profilePicUrl: String?) {
        val user = GoogleUser(
            idToken = idToken,
            accessToken = accessToken,  // ← ADD THIS
            displayName = displayName,
            profilePicUrl = profilePicUrl
        )
        println("✅ iOS: Google Sign-In successful - $displayName")
        continuation?.resume(Result.success(user))
        continuation = null
        shouldTrigger = false
    }

    @Suppress("unused")
    fun onFailure(error: String) {
        println("❌ iOS: Google Sign-In failed - $error")
        continuation?.resume(Result.failure(Exception(error)))
        continuation = null
        shouldTrigger = false
    }
}