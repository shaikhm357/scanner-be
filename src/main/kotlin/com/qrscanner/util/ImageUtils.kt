package com.qrscanner.util

import com.qrscanner.exception.CorruptedImageException
import com.qrscanner.exception.InvalidBase64Exception
import com.qrscanner.exception.UnsupportedImageFormatException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

object ImageUtils {

    private val SUPPORTED_MIME_TYPES = setOf(
        "image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp", "image/tiff"
    )

    private val SUPPORTED_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "tif"
    )

    fun decodeBase64ToBytes(base64String: String): ByteArray {
        val cleaned = base64String
            .substringAfter(",", base64String)  // Strip data URI prefix if present
            .replace("\\s".toRegex(), "")
        return try {
            Base64.getDecoder().decode(cleaned)
        } catch (e: IllegalArgumentException) {
            throw InvalidBase64Exception()
        }
    }

    fun bytesToBufferedImage(bytes: ByteArray): BufferedImage {
        return try {
            ImageIO.read(ByteArrayInputStream(bytes))
                ?: throw CorruptedImageException()
        } catch (e: CorruptedImageException) {
            throw e
        } catch (e: Exception) {
            throw CorruptedImageException()
        }
    }

    fun validateContentType(contentType: String?) {
        if (contentType == null) return
        val normalized = contentType.lowercase().split(";").first().trim()
        if (normalized !in SUPPORTED_MIME_TYPES) {
            throw UnsupportedImageFormatException(normalized)
        }
    }

    fun validateFileExtension(filename: String?) {
        if (filename == null) return
        val ext = filename.substringAfterLast(".", "").lowercase()
        if (ext.isNotEmpty() && ext !in SUPPORTED_EXTENSIONS) {
            throw UnsupportedImageFormatException(ext)
        }
    }

    fun detectMimeType(bytes: ByteArray): String? {
        if (bytes.size < 4) return null
        return when {
            // PNG: 89 50 4E 47
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
            // JPEG: FF D8 FF
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte() -> "image/jpeg"
            // GIF: 47 49 46
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() -> "image/gif"
            // BMP: 42 4D
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "image/bmp"
            // TIFF: 49 49 or 4D 4D
            (bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte()) ||
                (bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte()) -> "image/tiff"
            // WebP: RIFF....WEBP
            bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() -> "image/webp"
            else -> null
        }
    }
}
