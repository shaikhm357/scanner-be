package com.qrscanner.models.request

import kotlinx.serialization.Serializable

@Serializable
data class StreamConfigRequest(
    val escalationThreshold: Int = 10,
    val enableEscalation: Boolean = true
)
