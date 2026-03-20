package com.qrscanner.service

import com.qrscanner.exception.NoBarcodeFoundException
import com.qrscanner.models.response.BarcodeResult
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class DecodingService(
    private val barcodeDecoderService: BarcodeDecoderService,
    private val imagePreprocessor: ImagePreprocessor
) {

    private val logger = LoggerFactory.getLogger(DecodingService::class.java)

    data class DecodeResult(
        val results: List<BarcodeResult>,
        val preprocessingApplied: Boolean
    )

    fun decode(image: BufferedImage): DecodeResult {
        // Attempt 1: Original image (no preprocessing)
        logger.debug("Decode attempt 1: original image")
        val directResults = barcodeDecoderService.decode(image)
        if (directResults.isNotEmpty()) {
            return DecodeResult(directResults, preprocessingApplied = false)
        }

        // Attempt 2-5: Preprocessing strategies
        val strategies = listOf(
            ImagePreprocessor.Strategy.GRAYSCALE,
            ImagePreprocessor.Strategy.GRAYSCALE_CLAHE,
            ImagePreprocessor.Strategy.GRAYSCALE_BLUR_THRESHOLD,
            ImagePreprocessor.Strategy.GRAYSCALE_CLAHE_BILATERAL_THRESHOLD
        )

        for ((index, strategy) in strategies.withIndex()) {
            logger.debug("Decode attempt {}: {}", index + 2, strategy)
            try {
                val preprocessed = imagePreprocessor.preprocess(image, strategy)
                val results = barcodeDecoderService.decode(preprocessed)
                if (results.isNotEmpty()) {
                    return DecodeResult(results, preprocessingApplied = true)
                }
            } catch (e: Exception) {
                logger.warn("Preprocessing strategy {} failed: {}", strategy, e.message)
            }
        }

        throw NoBarcodeFoundException()
    }
}
