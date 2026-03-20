package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class DecodeResponse(
    val success: Boolean,
    val barcodesFound: Int,
    val results: List<BarcodeResult>,
    val processingTimeMs: Long,
    val preprocessingApplied: Boolean,
    val timestamp: String
)
