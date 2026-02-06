package com.bhaskar.synctask.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: run {
        println("❌ Invalid URL: $url")
        return
    }
    
    if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
        println("✅ Opened URL: $url")
    } else {
        println("❌ Cannot open URL: $url")
    }
}
