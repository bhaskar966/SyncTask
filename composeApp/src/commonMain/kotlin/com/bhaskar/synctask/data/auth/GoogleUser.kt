package com.bhaskar.synctask.data.auth

data class GoogleUser(
    val idToken: String,
    val accessToken: String,
    val displayName: String?,
    val profilePicUrl: String?
)