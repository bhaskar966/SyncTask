package com.bhaskar.synctask.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted()
}
