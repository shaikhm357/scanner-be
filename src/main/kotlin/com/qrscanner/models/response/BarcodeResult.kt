package com.qrscanner.models.response

import kotlinx.serialization.Serializable

@Serializable
data class BarcodeResult(
    val format: String,
    val rawValue: String,
    val contentType: String,
    val metadata: Map<String, String> = emptyMap(),
    val boundingBox: BoundingBox? = null,
    val confidence: String = "HIGH"
)

@Serializable
data class BoundingBox(
    val topLeft: Point,
    val topRight: Point,
    val bottomLeft: Point,
    val bottomRight: Point
)

@Serializable
data class Point(
    val x: Float,
    val y: Float
)
