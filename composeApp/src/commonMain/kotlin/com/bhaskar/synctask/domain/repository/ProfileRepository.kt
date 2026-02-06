package com.bhaskar.synctask.domain.repository

interface ProfileRepository {
    suspend fun fetchProfileImage(url: String): Result<ByteArray>
}
