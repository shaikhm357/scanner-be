package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class StreamDecodeResponse(
    val type: String,
    val success: Boolean = false,
    val barcodesFound: Int = 0,
    val results: List<BarcodeResult> = emptyList(),
    val processingTimeMs: Long = 0,
    val preprocessingApplied: Boolean = false,
    val frameNumber: Long = 0,
    val error: String? = null
)
