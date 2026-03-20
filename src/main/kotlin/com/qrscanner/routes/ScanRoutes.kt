package com.qrscanner.routes

import com.qrscanner.models.request.StreamConfigRequest
import com.qrscanner.models.response.StreamDecodeResponse
import com.qrscanner.service.StreamDecodingService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

private val logger = LoggerFactory.getLogger("ScanRoutes")

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

private const val MAX_FRAME_SIZE = 2 * 1024 * 1024 // 2MB

fun Route.scanRoutes(streamDecodingService: StreamDecodingService) {
    webSocket("/decode/stream") {
        val processing = AtomicBoolean(false)
        val frameCounter = AtomicLong(0)
        val sessionDecoder = streamDecodingService.createSessionDecoder()

        logger.info("WebSocket session started")

        // Send session_started message
        sendResponse(StreamDecodeResponse(type = "session_started", success = true))

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val bytes = frame.readBytes()

                        // Frame size validation
                        if (bytes.size > MAX_FRAME_SIZE) {
                            sendResponse(StreamDecodeResponse(
                                type = "error",
                                error = "Frame size ${bytes.size} exceeds maximum of $MAX_FRAME_SIZE bytes"
                            ))
                            continue
                        }

                        // Backpressure: drop frame if still processing previous one
                        if (!processing.compareAndSet(false, true)) {
                            continue
                        }

                        val currentFrame = frameCounter.incrementAndGet()

                        launch(Dispatchers.IO) {
                            try {
                                val startTime = System.currentTimeMillis()

                                val image: BufferedImage? = try {
                                    ImageIO.read(ByteArrayInputStream(bytes))
                                } catch (e: Exception) {
                                    null
                                }

                                if (image == null) {
                                    sendResponse(StreamDecodeResponse(
                                        type = "error",
                                        frameNumber = currentFrame,
                                        error = "Failed to decode image from binary frame"
                                    ))
                                    return@launch
                                }

                                val result = sessionDecoder.decode(image)
                                val elapsed = System.currentTimeMillis() - startTime

                                sendResponse(StreamDecodeResponse(
                                    type = "result",
                                    success = result.results.isNotEmpty(),
                                    barcodesFound = result.results.size,
                                    results = result.results,
                                    processingTimeMs = elapsed,
                                    preprocessingApplied = result.preprocessingApplied,
                                    frameNumber = currentFrame
                                ))
                            } catch (e: Exception) {
                                logger.error("Error processing frame {}: {}", currentFrame, e.message)
                                sendResponse(StreamDecodeResponse(
                                    type = "error",
                                    frameNumber = currentFrame,
                                    error = "Processing error: ${e.message}"
                                ))
                            } finally {
                                processing.set(false)
                            }
                        }
                    }

                    is Frame.Text -> {
                        try {
                            val configRequest = json.decodeFromString<StreamConfigRequest>(frame.readText())
                            sessionDecoder.reconfigure(
                                escalationThreshold = configRequest.escalationThreshold,
                                enableEscalation = configRequest.enableEscalation
                            )
                            sendResponse(StreamDecodeResponse(type = "session_started", success = true))
                        } catch (e: Exception) {
                            sendResponse(StreamDecodeResponse(
                                type = "error",
                                error = "Invalid config message: ${e.message}"
                            ))
                        }
                    }

                    is Frame.Close -> {
                        logger.info("WebSocket session closed by client")
                    }

                    else -> { /* ignore ping/pong */ }
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket session error: {}", e.message)
        } finally {
            logger.info("WebSocket session ended, processed {} frames", frameCounter.get())
        }
    }
}

private suspend fun DefaultWebSocketServerSession.sendResponse(response: StreamDecodeResponse) {
    send(Frame.Text(json.encodeToString(StreamDecodeResponse.serializer(), response)))
}
