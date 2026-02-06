package com.bhaskar.synctask.domain.model

/**
 * Data class holding active subscription details for UI display.
 */
data class ActiveSubscriptionInfo(
    val expirationDateFormatted: String?,
    val willRenew: Boolean,
    val productId: String?
)
