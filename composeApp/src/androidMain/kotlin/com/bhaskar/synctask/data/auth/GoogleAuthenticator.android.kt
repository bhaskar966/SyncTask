package com.bhaskar.synctask.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.bhaskar.synctask.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

actual class GoogleAuthenticator(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    actual suspend fun signIn(): Result<GoogleUser> {
        return try {
            // Build Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()

            // Build credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credential from Credential Manager
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Parse Google ID token credential
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val googleUser = GoogleUser(
                idToken = googleIdTokenCredential.idToken,
                accessToken = "",
                displayName = googleIdTokenCredential.displayName,
                profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString(),
            )

            println("✅ Android: Google Sign-In successful - ${googleUser.displayName}")
            Result.success(googleUser)

        } catch (e: GetCredentialException) {
            println("❌ Android: Credential Manager error - ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("❌ Android: Google Sign-In failed - ${e.message}")
            Result.failure(e)
        }
    }
}
