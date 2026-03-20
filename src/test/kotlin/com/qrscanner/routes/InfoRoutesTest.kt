package com.qrscanner.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InfoRoutesTest {

    @Test
    fun `health endpoint returns UP status`() = testApplication {
        val response = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("UP", body["status"]?.jsonPrimitive?.content)
        assertTrue(body.containsKey("uptime"))
        assertTrue(body.containsKey("opencvAvailable"))
        assertTrue(body.containsKey("timestamp"))
    }

    @Test
    fun `formats endpoint returns 17 formats`() = testApplication {
        val response = client.get("/api/v1/formats")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val formats = body["formats"]?.jsonArray
        assertEquals(17, formats?.size)
        assertEquals(17, body["totalFormats"]?.jsonPrimitive?.content?.toInt())
    }
}
