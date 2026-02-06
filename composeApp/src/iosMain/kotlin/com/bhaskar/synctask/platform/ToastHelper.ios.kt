package com.bhaskar.synctask.platform

actual fun showToast(message: String) {
    // No-op on iOS for now
    // Could be extended to use UIAlertController or similar
    println("📱 Toast (iOS): $message")
}
