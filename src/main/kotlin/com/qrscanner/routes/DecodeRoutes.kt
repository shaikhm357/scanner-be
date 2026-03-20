package com.qrscanner.routes

import com.qrscanner.config.AppConfig
import com.qrscanner.exception.BatchLimitExceededException
import com.qrscanner.exception.ImageTooLargeException
import com.qrscanner.exception.NoBarcodeFoundException
import com.qrscanner.models.request.BatchDecodeRequest
import com.qrscanner.models.request.DecodeRequest
import com.qrscanner.models.response.BatchDecodeResponse
import com.qrscanner.models.response.BatchItemResult
import com.qrscanner.models.response.DecodeResponse
import com.qrscanner.service.DecodingService
import com.qrscanner.util.ImageUtils
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.Instant

fun Route.decodeRoutes(decodingService: DecodingService, appConfig: AppConfig) {

    post("/decode") {
        val startTime = System.currentTimeMillis()

        val contentType = call.request.contentType()
        val imageBytes = when {
            contentType.match(ContentType.MultiPart.FormData) -> {
                receiveMultipartImage(call, appConfig.maxImageSize)
            }
            contentType.match(ContentType.Application.Json) -> {
                val request = call.receive<DecodeRequest>()
                val bytes = ImageUtils.decodeBase64ToBytes(request.image)
                validateImageSize(bytes, appConfig.maxImageSize)
                bytes
            }
            else -> {
                // Try reading raw bytes
                val bytes = call.receive<ByteArray>()
                validateImageSize(bytes, appConfig.maxImageSize)
                bytes
            }
        }

        // Validate image format by magic bytes
        val detectedMime = ImageUtils.detectMimeType(imageBytes)
        if (detectedMime != null) {
            ImageUtils.validateContentType(detectedMime)
        }

        val image = withContext(Dispatchers.IO) {
            ImageUtils.bytesToBufferedImage(imageBytes)
        }

        val result = withContext(Dispatchers.IO) {
            decodingService.decode(image)
        }

        val processingTime = System.currentTimeMillis() - startTime

        call.respond(
            DecodeResponse(
                success = true,
                barcodesFound = result.results.size,
                results = result.results,
                processingTimeMs = processingTime,
                preprocessingApplied = result.preprocessingApplied,
                timestamp = Instant.now().toString()
            )
        )
    }

    post("/decode/batch") {
        val startTime = System.currentTimeMillis()

        val contentType = call.request.contentType()
        val imageBytesList: List<ByteArray> = when {
            contentType.match(ContentType.MultiPart.FormData) -> {
                receiveMultipartImages(call, appConfig.maxImageSize, appConfig.maxBatchSize)
            }
            contentType.match(ContentType.Application.Json) -> {
                val request = call.receive<BatchDecodeRequest>()
                if (request.images.size > appConfig.maxBatchSize) {
                    throw BatchLimitExceededException(appConfig.maxBatchSize)
                }
                request.images.map { base64 ->
                    val bytes = ImageUtils.decodeBase64ToBytes(base64)
                    validateImageSize(bytes, appConfig.maxImageSize)
                    bytes
                }
            }
            else -> {
                throw BatchLimitExceededException()
            }
        }

        val batchResults = withContext(Dispatchers.IO) {
            imageBytesList.mapIndexed { index, bytes ->
                async {
                    val itemStart = System.currentTimeMillis()
                    try {
                        val image = ImageUtils.bytesToBufferedImage(bytes)
                        val result = decodingService.decode(image)
                        BatchItemResult(
                            index = index,
                            success = true,
                            barcodesFound = result.results.size,
                            results = result.results,
                            processingTimeMs = System.currentTimeMillis() - itemStart,
                            preprocessingApplied = result.preprocessingApplied
                        )
                    } catch (e: NoBarcodeFoundException) {
                        BatchItemResult(
                            index = index,
                            success = false,
                            error = e.message,
                            processingTimeMs = System.currentTimeMillis() - itemStart
                        )
                    } catch (e: Exception) {
                        BatchItemResult(
                            index = index,
                            success = false,
                            error = e.message ?: "Unknown error",
                            processingTimeMs = System.currentTimeMillis() - itemStart
                        )
                    }
                }
            }.awaitAll()
        }

        val totalProcessingTime = System.currentTimeMillis() - startTime

        call.respond(
            BatchDecodeResponse(
                success = true,
                totalImages = imageBytesList.size,
                successfulDecodes = batchResults.count { it.success },
                results = batchResults,
                totalProcessingTimeMs = totalProcessingTime,
                timestamp = Instant.now().toString()
            )
        )
    }
}

private suspend fun receiveMultipartImage(call: ApplicationCall, maxSize: Long): ByteArray {
    val multipart = call.receiveMultipart()
    var imageBytes: ByteArray? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                if (imageBytes == null) {
                    ImageUtils.validateContentType(part.contentType?.toString())
                    ImageUtils.validateFileExtension(part.originalFileName)
                    val bytes = part.streamProvider().readBytes()
                    validateImageSize(bytes, maxSize)
                    imageBytes = bytes
                }
            }
            else -> {}
        }
        part.dispose()
    }

    return imageBytes ?: throw io.ktor.server.plugins.BadRequestException("No image file found in multipart request")
}

private suspend fun receiveMultipartImages(call: ApplicationCall, maxSize: Long, maxBatch: Int): List<ByteArray> {
    val multipart = call.receiveMultipart()
    val images = mutableListOf<ByteArray>()

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                if (images.size >= maxBatch) {
                    part.dispose()
                    throw BatchLimitExceededException(maxBatch)
                }
                ImageUtils.validateContentType(part.contentType?.toString())
                ImageUtils.validateFileExtension(part.originalFileName)
                val bytes = part.streamProvider().readBytes()
                validateImageSize(bytes, maxSize)
                images.add(bytes)
            }
            else -> {}
        }
        part.dispose()
    }

    if (images.isEmpty()) {
        throw io.ktor.server.plugins.BadRequestException("No image files found in multipart request")
    }

    return images
}

private fun validateImageSize(bytes: ByteArray, maxSize: Long) {
    if (bytes.size > maxSize) {
        throw ImageTooLargeException(maxSize)
    }
}
