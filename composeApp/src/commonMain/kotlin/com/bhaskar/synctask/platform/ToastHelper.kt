package com.bhaskar.synctask.platform

/**
 * Shows a short toast/message to the user.
 * Android: Uses Toast
 * iOS: Currently no-op (can be extended with UIAlertController or similar)
 */
expect fun showToast(message: String)
