package com.bhaskar.synctask.data.services

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Pure wrapper service for RevenueCat SDK interactions.
 * Handles initialization, delegate callbacks, and user session management.
 */
class RevenueCatService : PurchasesDelegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isInitialized = false

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    /**
     * Set up the Purchases delegate. specific configuration (API key) is handled in Application/App classes.
     */
    fun initialize() {
        if (isInitialized) {
            println("🎫 RevenueCatService already initialized")
            return
        }

        try {
            Purchases.sharedInstance.delegate = this
            isInitialized = true
            println("✅ RevenueCatService initialized with delegate")

            // Fetch initial info
            scope.launch {
                fetchCustomerInfo()
            }
        } catch (e: Exception) {
            println("⚠️ RevenueCatService initialization failed: ${e.message}")
        }
    }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        println("🎫 RevenueCatService: onCustomerInfoUpdated")
        _customerInfo.value = customerInfo
    }

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            (PurchasesError, Boolean) -> Unit,
            (StoreTransaction, CustomerInfo) -> Unit
        ) -> Unit
    ) {
        println("🎫 RevenueCatService: onPurchasePromoProduct ${product.id}")
    }

    suspend fun fetchCustomerInfo(): Result<CustomerInfo> {
        return suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getCustomerInfo(
                onError = { error ->
                    println("❌ Failed to get customer info: ${error.message}")
                    cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { info ->
                    println("✅ Got customer info")
                    _customerInfo.value = info
                    cont.resume(Result.success(info))
                }
            )
        }
    }

    suspend fun restorePurchases(): Result<CustomerInfo> {
        return suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    println("❌ Restore failed: ${error.message}")
                    cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { info ->
                    println("✅ Restore successful")
                    _customerInfo.value = info
                    cont.resume(Result.success(info))
                }
            )
        }
    }

    suspend fun login(userId: String): Result<CustomerInfo> {
        return suspendCancellableCoroutine { cont ->
            println("🔑 RevenueCat: Logging in user $userId")
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { error ->
                    println("❌ Login failed: ${error.message}")
                    cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { info, created ->
                    println("✅ Login successful (created: $created)")
                    _customerInfo.value = info
                    cont.resume(Result.success(info))
                }
            )
        }
    }

    suspend fun logout(): Result<CustomerInfo> {
        return suspendCancellableCoroutine { cont ->
            println("🚪 RevenueCat: Logging out")
            Purchases.sharedInstance.logOut(
                onError = { error ->
                    println("❌ Logout failed: ${error.message}")
                    cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { info ->
                    println("✅ Logout successful")
                    _customerInfo.value = info
                    cont.resume(Result.success(info))
                }
            )
        }
    }
}
