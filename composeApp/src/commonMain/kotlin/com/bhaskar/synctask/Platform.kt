package com.bhaskar.synctask

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform