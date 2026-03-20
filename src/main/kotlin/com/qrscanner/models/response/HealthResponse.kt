package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val uptime: String,
    val opencvAvailable: Boolean,
    val timestamp: String
)
