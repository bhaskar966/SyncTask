package com.bhaskar.synctask.platform

import android.content.Intent
import android.net.Uri
import com.bhaskar.synctask.SyncTaskApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.android.ext.android.get
import android.app.Application

private object UrlOpenerContext : KoinComponent {
    val application: Application by inject()
}

actual fun openUrl(url: String) {
    try {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        UrlOpenerContext.application.startActivity(intent)
        println("✅ Opened URL: $url")
    } catch (e: Exception) {
        println("❌ Failed to open URL: ${e.message}")
    }
}
