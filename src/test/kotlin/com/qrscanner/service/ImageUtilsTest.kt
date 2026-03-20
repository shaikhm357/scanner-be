package com.qrscanner.service

import com.qrscanner.exception.InvalidBase64Exception
import com.qrscanner.exception.UnsupportedImageFormatException
import com.qrscanner.util.ImageUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImageUtilsTest {

    @Test
    fun `decodeBase64ToBytes handles valid base64`() {
        val original = "Hello, World!".toByteArray()
        val encoded = Base64.getEncoder().encodeToString(original)
        val decoded = ImageUtils.decodeBase64ToBytes(encoded)
        assertEquals(String(original), String(decoded))
    }

    @Test
    fun `decodeBase64ToBytes strips data URI prefix`() {
        val original = "test data".toByteArray()
        val encoded = "data:image/png;base64," + Base64.getEncoder().encodeToString(original)
        val decoded = ImageUtils.decodeBase64ToBytes(encoded)
        assertEquals("test data", String(decoded))
    }

    @Test
    fun `decodeBase64ToBytes throws on invalid base64`() {
        assertThrows<InvalidBase64Exception> {
            ImageUtils.decodeBase64ToBytes("not-valid-base64!!!")
        }
    }

    @Test
    fun `validateContentType accepts supported types`() {
        ImageUtils.validateContentType("image/png")
        ImageUtils.validateContentType("image/jpeg")
        ImageUtils.validateContentType("image/gif")
        ImageUtils.validateContentType("image/bmp")
        ImageUtils.validateContentType("image/webp")
        ImageUtils.validateContentType("image/tiff")
    }

    @Test
    fun `validateContentType rejects unsupported types`() {
        assertThrows<UnsupportedImageFormatException> {
            ImageUtils.validateContentType("application/pdf")
        }
    }

    @Test
    fun `validateContentType handles null`() {
        ImageUtils.validateContentType(null) // Should not throw
    }

    @Test
    fun `detectMimeType detects PNG`() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals("image/png", ImageUtils.detectMimeType(pngHeader))
    }

    @Test
    fun `detectMimeType detects JPEG`() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals("image/jpeg", ImageUtils.detectMimeType(jpegHeader))
    }

    @Test
    fun `detectMimeType returns null for unknown format`() {
        val unknown = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertEquals(null, ImageUtils.detectMimeType(unknown))
    }

    @Test
    fun `bytesToBufferedImage reads valid PNG`() {
        // Create a minimal 1x1 red PNG programmatically
        val image = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, 0xFF0000)
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", baos)
        val bytes = baos.toByteArray()

        val result = ImageUtils.bytesToBufferedImage(bytes)
        assertNotNull(result)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }
}
