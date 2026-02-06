package com.bhaskar.synctask.data.repository

import com.bhaskar.synctask.data.services.RevenueCatService
import com.bhaskar.synctask.domain.repository.ActiveSubscriptionInfo
import com.bhaskar.synctask.domain.repository.SubscriptionRepository
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.revenuecat.purchases.kmp.models.CustomerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Implementation of SubscriptionRepository using RevenueCatService.
 * Holds domain logic for subscription state (e.g. checking entitlement ID).
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class SubscriptionRepositoryImpl(
    private val revenueCatService: RevenueCatService
) : SubscriptionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremiumSubscribed = MutableStateFlow(false)
    override val isPremiumSubscribed: StateFlow<Boolean> = _isPremiumSubscribed.asStateFlow()

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    override val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private val _managementUrl = MutableStateFlow<String?>(null)
    override val managementUrl: StateFlow<String?> = _managementUrl.asStateFlow()

    private val _activeSubscriptionInfo = MutableStateFlow<ActiveSubscriptionInfo?>(null)
    override val activeSubscriptionInfo: StateFlow<ActiveSubscriptionInfo?> = _activeSubscriptionInfo.asStateFlow()

    init {
        observeService()
    }

    private fun observeService() {
        scope.launch {
            revenueCatService.customerInfo.collectLatest { info ->
                if (info != null) {
                    processCustomerInfo(info)
                }
            }
        }
    }

    /**
     * Initialize the repository (and service).
     * Called from Application class.
     */
    fun initialize() {
        revenueCatService.initialize()
    }

    override suspend fun refreshCustomerInfo(): Result<CustomerInfo> {
        return revenueCatService.fetchCustomerInfo()
    }

    override suspend fun restorePurchases(): Result<CustomerInfo> {
        return revenueCatService.restorePurchases()
    }

    override suspend fun loginUser(userId: String): Result<CustomerInfo> {
        return revenueCatService.login(userId)
    }

    override suspend fun logoutUser(): Result<CustomerInfo> {
        return revenueCatService.logout()
    }

    private fun processCustomerInfo(info: CustomerInfo) {
        _customerInfo.value = info

        // Domain Logic: Check for premium entitlement
        val premiumEntitlement = info.entitlements[SubscriptionConfig.PREMIUM_ENTITLEMENT_ID]
        val hasPremium = premiumEntitlement?.isActive == true
        _isPremiumSubscribed.value = hasPremium

        // Update management URL - use safe access if possible or fallback
        // Note: KMP SDK might not expose managementURL directly nicely yet, handle with care
        try {
            _managementUrl.value = info.managementUrlString
        } catch (e: Exception) {
            // fast fail safely
        }

        // Update active subscription info
        if (hasPremium && premiumEntitlement != null) {
            val expirationFormatted = premiumEntitlement.expirationDate?.let { formatExpirationDate(it) }
            _activeSubscriptionInfo.value = ActiveSubscriptionInfo(
                expirationDateFormatted = expirationFormatted,
                willRenew = premiumEntitlement.willRenew,
                productId = premiumEntitlement.productIdentifier
            )
            println("🎫 Active subscription: product=${premiumEntitlement.productIdentifier}, expires=$expirationFormatted")
        } else {
            _activeSubscriptionInfo.value = null
        }
        
        println("🎫 Repository updated: isPremium=$hasPremium")
    }

    private fun formatExpirationDate(instant: Long?): String {
        if (instant == null) return "Unknown"
        return try {
            // Convert millis to Instant
            val inst = Instant.fromEpochMilliseconds(instant)
            val localDateTime = inst.toLocalDateTime(TimeZone.currentSystemDefault())
            val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            "$month ${localDateTime.day}, ${localDateTime.year}"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    // Overload for KMP Instant if needed, keeping compatibility
    private fun formatExpirationDate(instant: Any): String {
        return if (instant is Long) formatExpirationDate(instant as Long?) 
        else "Unknown"
    }
}
