package com.bhaskar.synctask.platform

/**
 * Opens a URL in the system browser.
 * Platform-specific implementations handle Android/iOS differences.
 */
expect fun openUrl(url: String)
