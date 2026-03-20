package com.qrscanner.util

import com.google.zxing.BarcodeFormat
import com.qrscanner.models.response.FormatInfo

object BarcodeFormatMapper {

    private val ONE_D_FORMATS = setOf(
        BarcodeFormat.CODABAR,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.EAN_8,
        BarcodeFormat.EAN_13,
        BarcodeFormat.ITF,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED,
        BarcodeFormat.UPC_EAN_EXTENSION
    )

    private val TWO_D_FORMATS = setOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.PDF_417,
        BarcodeFormat.AZTEC,
        BarcodeFormat.MAXICODE
    )

    val ALL_FORMATS: Set<BarcodeFormat> = ONE_D_FORMATS + TWO_D_FORMATS

    fun getFormatType(format: BarcodeFormat): String {
        return when (format) {
            in ONE_D_FORMATS -> "1D"
            in TWO_D_FORMATS -> "2D"
            else -> "UNKNOWN"
        }
    }

    fun getAllFormatInfo(): List<FormatInfo> {
        return ALL_FORMATS.map { format ->
            FormatInfo(
                name = format.name,
                type = getFormatType(format)
            )
        }.sortedWith(compareBy({ it.type }, { it.name }))
    }
}
