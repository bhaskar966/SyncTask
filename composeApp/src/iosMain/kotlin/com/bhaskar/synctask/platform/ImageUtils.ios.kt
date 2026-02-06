package com.bhaskar.synctask.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
