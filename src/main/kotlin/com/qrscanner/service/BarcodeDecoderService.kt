package com.qrscanner.service

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import com.qrscanner.models.response.BarcodeResult
import com.qrscanner.models.response.BoundingBox
import com.qrscanner.models.response.Point
import com.qrscanner.util.BarcodeFormatMapper
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class BarcodeDecoderService(private val contentTypeDetector: ContentTypeDetector) {

    private val logger = LoggerFactory.getLogger(BarcodeDecoderService::class.java)

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to BarcodeFormatMapper.ALL_FORMATS.toList(),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
        DecodeHintType.ALSO_INVERTED to true
    )

    fun decode(image: BufferedImage): List<BarcodeResult> {
        val source = BufferedImageLuminanceSource(image)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        val results = mutableListOf<Result>()

        // Try multi-barcode reader first
        try {
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val multiReader = GenericMultipleBarcodeReader(reader)
            val multiResults = multiReader.decodeMultiple(bitmap, hints)
            results.addAll(multiResults)
        } catch (e: NotFoundException) {
            // Fall back to single reader
            try {
                val reader = MultiFormatReader()
                val result = reader.decode(bitmap, hints)
                results.add(result)
            } catch (e: NotFoundException) {
                logger.debug("No barcode found in image")
                return emptyList()
            }
        }

        return results.map { result -> mapResult(result) }
            .distinctBy { it.rawValue + it.format }
    }

    private fun mapResult(result: Result): BarcodeResult {
        val detection = contentTypeDetector.detect(result.text)
        val boundingBox = extractBoundingBox(result.resultPoints)

        return BarcodeResult(
            format = result.barcodeFormat.name,
            rawValue = result.text,
            contentType = detection.contentType.name,
            metadata = detection.metadata,
            boundingBox = boundingBox,
            confidence = determineConfidence(result)
        )
    }

    private fun extractBoundingBox(points: Array<ResultPoint>?): BoundingBox? {
        if (points == null || points.isEmpty()) return null

        return when {
            points.size >= 4 -> BoundingBox(
                topLeft = Point(points[0].x, points[0].y),
                topRight = Point(points[1].x, points[1].y),
                bottomRight = Point(points[2].x, points[2].y),
                bottomLeft = Point(points[3].x, points[3].y)
            )
            points.size == 3 -> BoundingBox(
                topLeft = Point(points[0].x, points[0].y),
                topRight = Point(points[1].x, points[1].y),
                bottomLeft = Point(points[2].x, points[2].y),
                bottomRight = Point(points[2].x, points[1].y)
            )
            points.size == 2 -> BoundingBox(
                topLeft = Point(points[0].x, points[0].y),
                topRight = Point(points[1].x, points[0].y),
                bottomLeft = Point(points[0].x, points[1].y),
                bottomRight = Point(points[1].x, points[1].y)
            )
            else -> {
                val p = points[0]
                BoundingBox(
                    topLeft = Point(p.x, p.y),
                    topRight = Point(p.x, p.y),
                    bottomLeft = Point(p.x, p.y),
                    bottomRight = Point(p.x, p.y)
                )
            }
        }
    }

    private fun determineConfidence(result: Result): String {
        val metadata = result.resultMetadata
        if (metadata != null) {
            val errorsCorrected = metadata[ResultMetadataType.ERROR_CORRECTION_LEVEL]
            if (errorsCorrected != null) {
                return when (errorsCorrected.toString()) {
                    "H", "Q" -> "HIGH"
                    "M" -> "MEDIUM"
                    "L" -> "LOW"
                    else -> "HIGH"
                }
            }
        }
        return "HIGH"
    }
}
