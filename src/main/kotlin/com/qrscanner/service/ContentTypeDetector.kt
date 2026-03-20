package com.qrscanner.service

class ContentTypeDetector {

    enum class ContentType {
        URL, WIFI, VCARD, VEVENT, EMAIL, PHONE, SMS, GEO, MECARD, TEXT
    }

    data class DetectionResult(
        val contentType: ContentType,
        val metadata: Map<String, String>
    )

    fun detect(rawValue: String): DetectionResult {
        val trimmed = rawValue.trim()

        return when {
            trimmed.startsWith("WIFI:", ignoreCase = true) -> parseWifi(trimmed)
            trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> parseVCard(trimmed)
            trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true) -> parseVEvent(trimmed)
            trimmed.startsWith("MECARD:", ignoreCase = true) -> parseMeCard(trimmed)
            trimmed.startsWith("geo:", ignoreCase = true) -> parseGeo(trimmed)
            trimmed.startsWith("tel:", ignoreCase = true) -> parsePhone(trimmed)
            trimmed.startsWith("mailto:", ignoreCase = true) -> parseEmail(trimmed)
            trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true) -> parseSms(trimmed)
            trimmed.matches(Regex("^https?://.*", RegexOption.IGNORE_CASE)) -> parseUrl(trimmed)
            trimmed.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) ->
                DetectionResult(ContentType.EMAIL, mapOf("address" to trimmed))
            trimmed.matches(Regex("^\\+?[0-9\\s\\-()]{7,}$")) ->
                DetectionResult(ContentType.PHONE, mapOf("number" to trimmed))
            else -> DetectionResult(ContentType.TEXT, emptyMap())
        }
    }

    private fun parseUrl(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        try {
            val url = java.net.URI(value)
            metadata["scheme"] = url.scheme ?: ""
            metadata["domain"] = url.host ?: ""
            if (url.path?.isNotEmpty() == true) metadata["path"] = url.path
        } catch (_: Exception) {
            metadata["raw"] = value
        }
        return DetectionResult(ContentType.URL, metadata)
    }

    private fun parseWifi(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val content = value.removePrefix("WIFI:").removeSuffix(";").removeSuffix(";;")
        val parts = content.split(";")
        for (part in parts) {
            val keyValue = part.split(":", limit = 2)
            if (keyValue.size == 2) {
                when (keyValue[0].uppercase()) {
                    "S" -> metadata["ssid"] = keyValue[1]
                    "T" -> metadata["security"] = keyValue[1]
                    "P" -> metadata["password"] = keyValue[1]
                    "H" -> metadata["hidden"] = keyValue[1]
                }
            }
        }
        return DetectionResult(ContentType.WIFI, metadata)
    }

    private fun parseVCard(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val lines = value.lines()
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].split(";").first().uppercase()
                when (key) {
                    "FN" -> metadata["name"] = parts[1]
                    "TEL" -> metadata["phone"] = parts[1]
                    "EMAIL" -> metadata["email"] = parts[1]
                    "ORG" -> metadata["organization"] = parts[1]
                    "URL" -> metadata["url"] = parts[1]
                }
            }
        }
        return DetectionResult(ContentType.VCARD, metadata)
    }

    private fun parseVEvent(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val lines = value.lines()
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                when (parts[0].split(";").first().uppercase()) {
                    "SUMMARY" -> metadata["summary"] = parts[1]
                    "DTSTART" -> metadata["start"] = parts[1]
                    "DTEND" -> metadata["end"] = parts[1]
                    "LOCATION" -> metadata["location"] = parts[1]
                }
            }
        }
        return DetectionResult(ContentType.VEVENT, metadata)
    }

    private fun parseMeCard(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val content = value.removePrefix("MECARD:").removeSuffix(";").removeSuffix(";;")
        val parts = content.split(";")
        for (part in parts) {
            val keyValue = part.split(":", limit = 2)
            if (keyValue.size == 2) {
                when (keyValue[0].uppercase()) {
                    "N" -> metadata["name"] = keyValue[1]
                    "TEL" -> metadata["phone"] = keyValue[1]
                    "EMAIL" -> metadata["email"] = keyValue[1]
                    "URL" -> metadata["url"] = keyValue[1]
                }
            }
        }
        return DetectionResult(ContentType.MECARD, metadata)
    }

    private fun parseGeo(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val coords = value.removePrefix("geo:").split(",")
        if (coords.size >= 2) {
            metadata["latitude"] = coords[0].split("?").first()
            metadata["longitude"] = coords[1].split("?").first()
        }
        val queryIdx = value.indexOf("?")
        if (queryIdx >= 0) {
            metadata["query"] = value.substring(queryIdx + 1)
        }
        return DetectionResult(ContentType.GEO, metadata)
    }

    private fun parsePhone(value: String): DetectionResult {
        val number = value.removePrefix("tel:").removePrefix("TEL:")
        return DetectionResult(ContentType.PHONE, mapOf("number" to number))
    }

    private fun parseEmail(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val content = value.removePrefix("mailto:").removePrefix("MAILTO:")
        val parts = content.split("?", limit = 2)
        metadata["address"] = parts[0]
        if (parts.size > 1) {
            parts[1].split("&").forEach { param ->
                val kv = param.split("=", limit = 2)
                if (kv.size == 2) {
                    when (kv[0].lowercase()) {
                        "subject" -> metadata["subject"] = kv[1]
                        "body" -> metadata["body"] = kv[1]
                    }
                }
            }
        }
        return DetectionResult(ContentType.EMAIL, metadata)
    }

    private fun parseSms(value: String): DetectionResult {
        val metadata = mutableMapOf<String, String>()
        val content = value.removePrefix("smsto:").removePrefix("SMSTO:")
            .removePrefix("sms:").removePrefix("SMS:")
        val parts = content.split(":", limit = 2)
        metadata["number"] = parts[0]
        if (parts.size > 1) {
            metadata["message"] = parts[1]
        }
        return DetectionResult(ContentType.SMS, metadata)
    }
}
