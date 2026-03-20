package com.qrscanner.routes

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScanRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun createBlankJpegBytes(): ByteArray {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, 100, 100)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }

    private fun receiveTextFrame(frame: Frame): String {
        assertTrue(frame is Frame.Text)
        return frame.readText()
    }

    @Test
    fun `websocket connects and receives session_started`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/v1/decode/stream") {
            val body = json.parseToJsonElement(receiveTextFrame(incoming.receive())).jsonObject
            assertEquals("session_started", body["type"]?.jsonPrimitive?.content)
            assertEquals("true", body["success"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun `binary frame with blank image returns result with success false`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/v1/decode/stream") {
            // Consume session_started
            incoming.receive()

            // Send a blank JPEG frame
            send(Frame.Binary(true, createBlankJpegBytes()))

            val body = json.parseToJsonElement(receiveTextFrame(incoming.receive())).jsonObject
            assertEquals("result", body["type"]?.jsonPrimitive?.content)
            assertEquals("false", body["success"]?.jsonPrimitive?.content)
            assertEquals("0", body["barcodesFound"]?.jsonPrimitive?.content)
            assertNotNull(body["frameNumber"])
        }
    }

    @Test
    fun `oversized binary frame returns error`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/v1/decode/stream") {
            // Consume session_started
            incoming.receive()

            // Send oversized frame (> 2MB)
            val oversizedBytes = ByteArray(2 * 1024 * 1024 + 1)
            send(Frame.Binary(true, oversizedBytes))

            val body = json.parseToJsonElement(receiveTextFrame(incoming.receive())).jsonObject
            assertEquals("error", body["type"]?.jsonPrimitive?.content)
            assertNotNull(body["error"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun `text frame with config reconfigures session`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/v1/decode/stream") {
            // Consume session_started
            incoming.receive()

            // Send config message
            send(Frame.Text("""{"escalationThreshold": 5, "enableEscalation": true}"""))

            val body = json.parseToJsonElement(receiveTextFrame(incoming.receive())).jsonObject
            assertEquals("session_started", body["type"]?.jsonPrimitive?.content)
            assertEquals("true", body["success"]?.jsonPrimitive?.content)
        }
    }
}
