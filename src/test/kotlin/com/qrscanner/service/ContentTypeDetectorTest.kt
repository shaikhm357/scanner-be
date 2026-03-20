package com.qrscanner.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContentTypeDetectorTest {

    private val detector = ContentTypeDetector()

    @Test
    fun `detect URL`() {
        val result = detector.detect("https://example.com/path")
        assertEquals(ContentTypeDetector.ContentType.URL, result.contentType)
        assertEquals("example.com", result.metadata["domain"])
        assertEquals("https", result.metadata["scheme"])
    }

    @Test
    fun `detect WiFi QR code`() {
        val result = detector.detect("WIFI:S:MyNetwork;T:WPA;P:password123;;")
        assertEquals(ContentTypeDetector.ContentType.WIFI, result.contentType)
        assertEquals("MyNetwork", result.metadata["ssid"])
        assertEquals("WPA", result.metadata["security"])
        assertEquals("password123", result.metadata["password"])
    }

    @Test
    fun `detect vCard`() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:John Doe
            TEL:+1234567890
            EMAIL:john@example.com
            END:VCARD
        """.trimIndent()
        val result = detector.detect(vcard)
        assertEquals(ContentTypeDetector.ContentType.VCARD, result.contentType)
        assertEquals("John Doe", result.metadata["name"])
        assertEquals("+1234567890", result.metadata["phone"])
        assertEquals("john@example.com", result.metadata["email"])
    }

    @Test
    fun `detect email`() {
        val result = detector.detect("mailto:test@example.com?subject=Hello")
        assertEquals(ContentTypeDetector.ContentType.EMAIL, result.contentType)
        assertEquals("test@example.com", result.metadata["address"])
        assertEquals("Hello", result.metadata["subject"])
    }

    @Test
    fun `detect bare email address`() {
        val result = detector.detect("user@example.com")
        assertEquals(ContentTypeDetector.ContentType.EMAIL, result.contentType)
        assertEquals("user@example.com", result.metadata["address"])
    }

    @Test
    fun `detect phone`() {
        val result = detector.detect("tel:+1234567890")
        assertEquals(ContentTypeDetector.ContentType.PHONE, result.contentType)
        assertEquals("+1234567890", result.metadata["number"])
    }

    @Test
    fun `detect SMS`() {
        val result = detector.detect("smsto:+1234567890:Hello World")
        assertEquals(ContentTypeDetector.ContentType.SMS, result.contentType)
        assertEquals("+1234567890", result.metadata["number"])
        assertEquals("Hello World", result.metadata["message"])
    }

    @Test
    fun `detect geo location`() {
        val result = detector.detect("geo:40.7128,-74.0060")
        assertEquals(ContentTypeDetector.ContentType.GEO, result.contentType)
        assertEquals("40.7128", result.metadata["latitude"])
        assertEquals("-74.0060", result.metadata["longitude"])
    }

    @Test
    fun `detect MECARD`() {
        val result = detector.detect("MECARD:N:John Doe;TEL:+1234567890;EMAIL:john@example.com;")
        assertEquals(ContentTypeDetector.ContentType.MECARD, result.contentType)
        assertEquals("John Doe", result.metadata["name"])
        assertEquals("+1234567890", result.metadata["phone"])
    }

    @Test
    fun `detect VEVENT`() {
        val vevent = """
            BEGIN:VEVENT
            SUMMARY:Meeting
            DTSTART:20260315T100000Z
            DTEND:20260315T110000Z
            LOCATION:Room 101
            END:VEVENT
        """.trimIndent()
        val result = detector.detect(vevent)
        assertEquals(ContentTypeDetector.ContentType.VEVENT, result.contentType)
        assertEquals("Meeting", result.metadata["summary"])
        assertEquals("Room 101", result.metadata["location"])
    }

    @Test
    fun `detect plain text`() {
        val result = detector.detect("Just some random text")
        assertEquals(ContentTypeDetector.ContentType.TEXT, result.contentType)
    }
}
