package com.qrscanner.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.time.Duration

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = 5 * 1024 * 1024  // 5MB protocol limit; app-level validation at 2MB
        masking = false
    }
}
