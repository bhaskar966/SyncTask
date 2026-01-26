package com.bhaskar.synctask.domain

import platform.Foundation.NSUUID

actual fun generateUUID(): String = NSUUID().UUIDString()
