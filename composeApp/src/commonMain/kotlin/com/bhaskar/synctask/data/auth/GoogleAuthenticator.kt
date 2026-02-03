package com.bhaskar.synctask.data.auth

expect class GoogleAuthenticator {

    suspend fun signIn(): Result<GoogleUser>

}