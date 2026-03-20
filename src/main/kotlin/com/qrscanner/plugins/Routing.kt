package com.qrscanner.plugins

import com.qrscanner.config.AppConfig
import com.qrscanner.routes.decodeRoutes
import com.qrscanner.routes.infoRoutes
import com.qrscanner.routes.scanRoutes
import com.qrscanner.service.BarcodeDecoderService
import com.qrscanner.service.ContentTypeDetector
import com.qrscanner.service.DecodingService
import com.qrscanner.service.ImagePreprocessor
import com.qrscanner.service.StreamDecodingService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val appConfig = AppConfig.load(environment)
    val contentTypeDetector = ContentTypeDetector()
    val barcodeDecoderService = BarcodeDecoderService(contentTypeDetector)
    val imagePreprocessor = ImagePreprocessor()
    val decodingService = DecodingService(barcodeDecoderService, imagePreprocessor)
    val streamDecodingService = StreamDecodingService(barcodeDecoderService, imagePreprocessor)

    routing {
        route("/api/v1") {
            infoRoutes()
            decodeRoutes(decodingService, appConfig)
            scanRoutes(streamDecodingService)
        }
    }
}
