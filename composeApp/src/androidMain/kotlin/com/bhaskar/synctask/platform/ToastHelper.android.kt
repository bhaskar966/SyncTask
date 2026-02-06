package com.bhaskar.synctask.platform

import android.app.Application
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object ToastContext : KoinComponent {
    val application: Application by inject()
}

actual fun showToast(message: String) {
    try {
        Toast.makeText(ToastContext.application, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        println("❌ Failed to show toast: ${e.message}")
    }
}
