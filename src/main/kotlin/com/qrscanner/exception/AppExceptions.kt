package com.qrscanner.exception

open class AppException(override val message: String, val code: String) : RuntimeException(message)

class ImageTooLargeException(maxSize: Long = 10_485_760) :
    AppException("Image size exceeds maximum allowed size of ${maxSize / 1_048_576}MB", "IMAGE_TOO_LARGE")

class UnsupportedImageFormatException(format: String) :
    AppException("Unsupported image format: $format. Supported: PNG, JPEG, GIF, BMP, WebP, TIFF", "UNSUPPORTED_FORMAT")

class NoBarcodeFoundException :
    AppException("No barcode or QR code found in the image after all processing attempts", "NO_BARCODE_FOUND")

class CorruptedImageException :
    AppException("The provided image data is corrupted or unreadable", "CORRUPTED_IMAGE")

class InvalidBase64Exception :
    AppException("The provided base64 string is invalid or cannot be decoded", "INVALID_BASE64")

class BatchLimitExceededException(maxSize: Int = 10) :
    AppException("Batch size exceeds maximum of $maxSize images", "BATCH_LIMIT_EXCEEDED")
