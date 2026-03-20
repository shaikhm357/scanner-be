package com.qrscanner.service

import com.qrscanner.models.response.BarcodeResult
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

class StreamDecodingService(
    private val barcodeDecoderService: BarcodeDecoderService,
    private val imagePreprocessor: ImagePreprocessor
) {

    private val logger = LoggerFactory.getLogger(StreamDecodingService::class.java)

    data class StreamDecodeResult(
        val results: List<BarcodeResult>,
        val preprocessingApplied: Boolean
    )

    fun createSessionDecoder(escalationThreshold: Int = 10, enableEscalation: Boolean = true): SessionDecoder {
        return SessionDecoder(
            barcodeDecoderService = barcodeDecoderService,
            imagePreprocessor = imagePreprocessor,
            escalationThreshold = escalationThreshold,
            enableEscalation = enableEscalation
        )
    }

    class SessionDecoder(
        private val barcodeDecoderService: BarcodeDecoderService,
        private val imagePreprocessor: ImagePreprocessor,
        @Volatile var escalationThreshold: Int,
        @Volatile var enableEscalation: Boolean
    ) {
        private val logger = LoggerFactory.getLogger(SessionDecoder::class.java)
        private val consecutiveFailures = AtomicInteger(0)

        fun decode(image: BufferedImage): StreamDecodeResult {
            // Fast path: raw ZXing decode
            val fastResults = barcodeDecoderService.decode(image)
            if (fastResults.isNotEmpty()) {
                consecutiveFailures.set(0)
                return StreamDecodeResult(results = fastResults, preprocessingApplied = false)
            }

            // Increment failure counter
            val failures = consecutiveFailures.incrementAndGet()

            // Escalation: try CLAHE preprocessing after N consecutive failures
            if (enableEscalation && failures >= escalationThreshold) {
                try {
                    val preprocessed = imagePreprocessor.preprocess(image, ImagePreprocessor.Strategy.GRAYSCALE_CLAHE)
                    val escalatedResults = barcodeDecoderService.decode(preprocessed)
                    if (escalatedResults.isNotEmpty()) {
                        consecutiveFailures.set(0)
                        return StreamDecodeResult(results = escalatedResults, preprocessingApplied = true)
                    }
                } catch (e: Exception) {
                    logger.debug("Escalation preprocessing failed: {}", e.message)
                }
            }

            // Nothing found — normal for most frames
            return StreamDecodeResult(results = emptyList(), preprocessingApplied = false)
        }

        fun reconfigure(escalationThreshold: Int, enableEscalation: Boolean) {
            this.escalationThreshold = escalationThreshold
            this.enableEscalation = enableEscalation
            consecutiveFailures.set(0)
            logger.debug("Session reconfigured: threshold={}, escalation={}", escalationThreshold, enableEscalation)
        }
    }
}
