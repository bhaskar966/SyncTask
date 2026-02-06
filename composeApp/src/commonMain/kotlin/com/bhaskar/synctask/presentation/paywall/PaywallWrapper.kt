package com.bhaskar.synctask.presentation.paywall

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

/**
 * Wrapper around RevenueCat's built-in Paywall composable.
 * This provides a thin integration layer for navigation.
 * 
 * Note: Purchase completion is handled automatically by RevenueCat SDK,
 * which will update the SubscriptionRepositoryImpl via the PurchasesDelegate.
 */
@Composable
fun PaywallWrapper(
    onDismiss: () -> Unit
) {
    Paywall(PaywallOptions(dismissRequest = onDismiss))
}
