package com.qrscanner.service

import com.google.zxing.BarcodeFormat
import com.qrscanner.util.BarcodeFormatMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarcodeFormatMapperTest {

    @Test
    fun `all 17 formats are present`() {
        assertEquals(17, BarcodeFormatMapper.ALL_FORMATS.size)
    }

    @Test
    fun `QR_CODE is classified as 2D`() {
        assertEquals("2D", BarcodeFormatMapper.getFormatType(BarcodeFormat.QR_CODE))
    }

    @Test
    fun `CODE_128 is classified as 1D`() {
        assertEquals("1D", BarcodeFormatMapper.getFormatType(BarcodeFormat.CODE_128))
    }

    @Test
    fun `getAllFormatInfo returns sorted format info`() {
        val formats = BarcodeFormatMapper.getAllFormatInfo()
        assertEquals(17, formats.size)
        assertTrue(formats.any { it.name == "QR_CODE" && it.type == "2D" })
        assertTrue(formats.any { it.name == "EAN_13" && it.type == "1D" })
    }
}
