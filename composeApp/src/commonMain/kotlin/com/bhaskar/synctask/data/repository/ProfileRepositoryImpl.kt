package com.bhaskar.synctask.data.repository

import com.bhaskar.synctask.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

class ProfileRepositoryImpl() : ProfileRepository {

    private val httpClient: HttpClient = HttpClient()

    override suspend fun fetchProfileImage(url: String): Result<ByteArray> {
        return try {
            val response: HttpResponse = httpClient.get(url)
            val bytes: ByteArray = response.body()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
