package com.qrscanner.routes

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DecodeRoutesTest {

    @Test
    fun `decode returns 422 for image with no barcode`() = testApplication {
        // Create a plain white 10x10 PNG image (no barcode)
        val image = java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, 10, 10)
        g.dispose()
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", baos)
        val imageBytes = baos.toByteArray()

        val response = client.post("/api/v1/decode") {
            setBody(MultiPartFormDataContent(formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"test.png\"")
                })
            }))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("NO_BARCODE_FOUND", body["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun `decode returns 400 for invalid base64`() = testApplication {
        val response = client.post("/api/v1/decode") {
            contentType(ContentType.Application.Json)
            setBody("""{"image": "not-valid-base64!!!"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("INVALID_BASE64", body["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun `decode returns 413 for oversized image`() = testApplication {
        // Create base64 that decodes to > 10MB
        val largeBytes = ByteArray(11_000_000) { 0xFF.toByte() }
        val base64 = Base64.getEncoder().encodeToString(largeBytes)

        val response = client.post("/api/v1/decode") {
            contentType(ContentType.Application.Json)
            setBody("""{"image": "$base64"}""")
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `batch decode returns 400 when exceeding limit`() = testApplication {
        // Build JSON with 11 images (over the 10 limit)
        val dummyBase64 = Base64.getEncoder().encodeToString(ByteArray(10) { 0 })
        val images = (1..11).joinToString(",") { "\"$dummyBase64\"" }

        val response = client.post("/api/v1/decode/batch") {
            contentType(ContentType.Application.Json)
            setBody("""{"images": [$images]}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("BATCH_LIMIT_EXCEEDED", body["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun `decode returns full response with valid QR code image`() = testApplication {
        // Generate a real QR code image using ZXing
        val qrContent = "https://example.com/test"
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200)
        val qrImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(bitMatrix)
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(qrImage, "png", baos)
        val base64Image = Base64.getEncoder().encodeToString(baos.toByteArray())

        val response = client.post("/api/v1/decode") {
            contentType(ContentType.Application.Json)
            setBody("""{"image": "$base64Image"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Verify full DecodeResponse structure matches Android DTOs
        assertTrue(body["success"]?.jsonPrimitive?.boolean == true)
        assertTrue(body["barcodesFound"]?.jsonPrimitive?.int!! >= 1)
        assertNotNull(body["processingTimeMs"])
        assertTrue(body["processingTimeMs"]?.jsonPrimitive?.long!! >= 0)
        assertNotNull(body["preprocessingApplied"])
        assertNotNull(body["timestamp"])
        assertTrue(body["timestamp"]?.jsonPrimitive?.content?.isNotEmpty() == true)

        // Verify results array structure
        val results = body["results"] as JsonArray
        assertTrue(results.isNotEmpty())
        val firstResult = results[0].jsonObject
        assertEquals("QR_CODE", firstResult["format"]?.jsonPrimitive?.content)
        assertEquals(qrContent, firstResult["rawValue"]?.jsonPrimitive?.content)
        assertNotNull(firstResult["contentType"])
        assertNotNull(firstResult["confidence"])
    }
}
