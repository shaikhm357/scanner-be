package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val code: String,
    val timestamp: String
)
