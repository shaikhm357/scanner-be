package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class BatchDecodeResponse(
    val success: Boolean,
    val totalImages: Int,
    val successfulDecodes: Int,
    val results: List<BatchItemResult>,
    val totalProcessingTimeMs: Long,
    val timestamp: String
)

@Serializable
data class BatchItemResult(
    val index: Int,
    val success: Boolean,
    val barcodesFound: Int = 0,
    val results: List<BarcodeResult> = emptyList(),
    val error: String? = null,
    val processingTimeMs: Long = 0,
    val preprocessingApplied: Boolean = false
)
