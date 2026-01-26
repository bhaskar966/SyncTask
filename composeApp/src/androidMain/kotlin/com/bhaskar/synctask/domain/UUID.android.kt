package com.bhaskar.synctask.domain

import java.util.UUID

actual fun generateUUID(): String = UUID.randomUUID().toString()
