package com.bhaskar.synctask.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun byteArrayToImageBitmap(bytes: ByteArray): ImageBitmap?
