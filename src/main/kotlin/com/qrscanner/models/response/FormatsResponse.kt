package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class FormatsResponse(
    val formats: List<FormatInfo>,
    val totalFormats: Int
)

@Serializable
data class FormatInfo(
    val name: String,
    val type: String
)
