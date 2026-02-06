package com.bhaskar.synctask.domain.repository

import com.bhaskar.synctask.domain.model.ActiveSubscriptionInfo
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.revenuecat.purchases.kmp.models.CustomerInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for subscription management.
 * Acts as the domain layer interface for subscription operations.
 */
interface SubscriptionRepository {
    /** Current premium subscription state */
    val isPremiumSubscribed: StateFlow<Boolean>
    
    /** Current customer info from RevenueCat */
    val customerInfo: StateFlow<CustomerInfo?>
    
    /** Management URL for subscription (opens App Store/Play Store subscription settings) */
    val managementUrl: StateFlow<String?>
    
    /** Active subscription details for display */
    val activeSubscriptionInfo: StateFlow<ActiveSubscriptionInfo?>
    
    /** Force refresh customer info from RevenueCat */
    suspend fun refreshCustomerInfo(): Result<CustomerInfo>
    
    /** Restore previous purchases */
    suspend fun restorePurchases(): Result<CustomerInfo>
    
    /** 
     * Log in a user with their Firebase UID.
     * This syncs the user's subscription across devices.
     */
    suspend fun loginUser(userId: String): Result<CustomerInfo>
    
    /**
     * Log out the current user from RevenueCat.
     * Creates an anonymous user for future purchases until login.
     */
    suspend fun logoutUser(): Result<CustomerInfo>
}