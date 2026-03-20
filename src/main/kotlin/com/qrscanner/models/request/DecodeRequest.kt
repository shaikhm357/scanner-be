package com.qrscanner.models.request

import kotlinx.serialization.Serializable

@Serializable
data class DecodeRequest(
    val image: String,
    val formats: List<String>? = null
)

@Serializable
data class BatchDecodeRequest(
    val images: List<String>
)
