package com.qrscanner.routes

import com.qrscanner.models.response.FormatsResponse
import com.qrscanner.models.response.HealthResponse
import com.qrscanner.service.ImagePreprocessor
import com.qrscanner.util.BarcodeFormatMapper
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant

fun Route.infoRoutes() {
    val preprocessor = ImagePreprocessor()

    get("/health") {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        val duration = Duration.ofMillis(uptime)
        val uptimeStr = String.format(
            "%dd %dh %dm %ds",
            duration.toDays(),
            duration.toHoursPart(),
            duration.toMinutesPart(),
            duration.toSecondsPart()
        )

        call.respond(
            HealthResponse(
                status = "UP",
                uptime = uptimeStr,
                opencvAvailable = preprocessor.isAvailable(),
                timestamp = Instant.now().toString()
            )
        )
    }

    get("/formats") {
        val formats = BarcodeFormatMapper.getAllFormatInfo()
        call.respond(
            FormatsResponse(
                formats = formats,
                totalFormats = formats.size
            )
        )
    }
}
