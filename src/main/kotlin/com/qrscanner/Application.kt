package com.qrscanner

import com.qrscanner.plugins.configureMonitoring
import com.qrscanner.plugins.configureRouting
import com.qrscanner.plugins.configureSerialization
import com.qrscanner.plugins.configureWebSockets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureWebSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting()
}
