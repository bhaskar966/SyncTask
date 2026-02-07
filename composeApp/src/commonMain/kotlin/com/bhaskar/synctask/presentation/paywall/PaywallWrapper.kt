package com.bhaskar.synctask.presentation.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.kmp.models.Offerings
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
    var offerings by remember { mutableStateOf<Offerings?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        com.revenuecat.purchases.kmp.Purchases.sharedInstance.getOfferings(
            onError = { e ->
                error = e.message
                isLoading = false
            },
            onSuccess = { o ->
                offerings = o
                isLoading = false
                o.all.forEach { entry ->
                    println("offerings: ${entry.value}")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null || offerings?.current == null) {
        // Fallback UI for empty offerings (common in iOS Test Store without products)
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Store Not Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                Text(
                    text = "Could not load subscription products. This usually happens in Test builds without App Store configuration.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                
                if (error != null) {

                    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))

                Button(onClick = onDismiss) {
                    Text("Close")
                }

            }
        }
    } else {
        Paywall(PaywallOptions(dismissRequest = onDismiss))
    }
}
