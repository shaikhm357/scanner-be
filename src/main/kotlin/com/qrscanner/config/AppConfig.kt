package com.qrscanner.config

import io.ktor.server.application.*

data class AppConfig(
    val maxImageSize: Long,
    val maxBatchSize: Int,
    val supportedFormats: List<String>
) {
    companion object {
        fun load(environment: ApplicationEnvironment): AppConfig {
            val config = environment.config
            return AppConfig(
                maxImageSize = config.propertyOrNull("app.maxImageSize")?.getString()?.toLong() ?: 10_485_760,
                maxBatchSize = config.propertyOrNull("app.maxBatchSize")?.getString()?.toInt() ?: 10,
                supportedFormats = config.propertyOrNull("app.supportedFormats")?.getList()
                    ?: listOf("image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp", "image/tiff")
            )
        }
    }
}
