package com.cherryclip.app.model

import com.google.gson.JsonParser
import java.net.URI

data class ClassificationResult(
    val type: ClipType,
    val metadata: String = "",
    val isSensitive: Boolean = false
)

object ContentClassifier {

    private val hexColorRegex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}|[A-Fa-z0-9]{3})$")
    private val strictHexColorRegex = Regex("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
    private val rgbColorRegex = Regex("^rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})(?:\\s*,\\s*(0|1|0?\\.\\d+))?\\s*\\)$", RegexOption.IGNORE_CASE)
    private val urlRegex = Regex("^(https?://|ftp://)[^\\s/$.?#].[^\\s]*$", RegexOption.IGNORE_CASE)
    private val domainUrlRegex = Regex("^(www\\.)?[a-zA-Z0-9-]+\\.(com|cn|net|org|io|dev|app|ai|me|co|edu|gov)(/[\\S]*)?$", RegexOption.IGNORE_CASE)
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^(\\+?\\d{1,4}[-.\\s]?)?(\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{4}$")

    private val otpKeywordRegex = Regex("(验证码|校验码|动态码|安全码|取件码|口令|verification\\s*code|auth\\s*code|otp|passcode|pin\\s*code)", RegexOption.IGNORE_CASE)
    private val otpDigitsRegex = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")

    private val apiKeyRegex = Regex("(sk-[A-Za-z0-9]{16,}|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z-_]{20,}|-----BEGIN [A-Z ]*PRIVATE KEY-----)")
    private val passwordHintRegex = Regex("(密码|口令|password|passwd|secret|token)\\s*[:：=]\\s*\\S+", RegexOption.IGNORE_CASE)
    private val cardRegex = Regex("^\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{3,4}$")

    fun classify(content: String, hasImage: Boolean = false): ClassificationResult {
        if (hasImage) {
            return ClassificationResult(ClipType.IMAGE, metadata = "图片")
        }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return ClassificationResult(ClipType.TEXT)
        }

        val sensitive = detectSensitive(trimmed)

        // 1. Color (#RRGGBB, #RGB, #AARRGGBB, rgb())
        parseColorHex(trimmed)?.let { normalizedHex ->
            return ClassificationResult(ClipType.COLOR, metadata = normalizedHex, isSensitive = false)
        }

        // 2. URL
        if (urlRegex.matches(trimmed) || (!trimmed.contains(" ") && domainUrlRegex.matches(trimmed))) {
            val host = extractHost(trimmed)
            return ClassificationResult(ClipType.URL, metadata = host, isSensitive = sensitive)
        }

        // 3. Email
        if (emailRegex.matches(trimmed)) {
            return ClassificationResult(ClipType.EMAIL, metadata = trimmed.substringAfter("@"), isSensitive = false)
        }

        // 4. Phone
        val compactPhone = trimmed.replace(" ", "").replace("-", "")
        if (compactPhone.length in 7..16 && phoneRegex.matches(trimmed)) {
            return ClassificationResult(ClipType.PHONE, metadata = trimmed, isSensitive = false)
        }

        // 5. OTP / 验证码
        if (trimmed.length <= 240 && otpKeywordRegex.containsMatchIn(trimmed)) {
            val code = otpDigitsRegex.find(trimmed)?.groupValues?.getOrNull(1)
            if (code != null) {
                return ClassificationResult(ClipType.OTP, metadata = code, isSensitive = false)
            }
        }

        // 6. Code / JSON
        if (looksLikeJson(trimmed)) {
            return ClassificationResult(ClipType.CODE, metadata = "JSON", isSensitive = sensitive)
        }
        detectCodeLanguage(trimmed)?.let { lang ->
            return ClassificationResult(ClipType.CODE, metadata = lang, isSensitive = sensitive)
        }

        return ClassificationResult(ClipType.TEXT, metadata = "${trimmed.length} 字符", isSensitive = sensitive)
    }

    fun detectSensitive(text: String): Boolean {
        val t = text.trim()
        if (apiKeyRegex.containsMatchIn(t)) return true
        if (passwordHintRegex.containsMatchIn(t)) return true
        if (cardRegex.matches(t)) return true
        return false
    }

    fun parseColorHex(text: String): String? {
        val t = text.trim()
        if (strictHexColorRegex.matches(t)) {
            val hex = t.substring(1).uppercase()
            return when (hex.length) {
                3 -> "#" + hex.map { "$it$it" }.joinToString("")
                6, 8 -> "#$hex"
                else -> null
            }
        }
        val rgbMatch = rgbColorRegex.matchEntire(t) ?: return null
        val r = rgbMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val g = rgbMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val b = rgbMatch.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val aFloat = rgbMatch.groupValues[4].toFloatOrNull()
        return if (aFloat != null) {
            val a = (aFloat.coerceIn(0f, 1f) * 255).toInt()
            String.format("#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    fun colorHexToInt(hex: String): Int? {
        return try {
            val clean = hex.removePrefix("#")
            when (clean.length) {
                6 -> (0xFF000000.toInt()) or clean.toInt(16)
                8 -> clean.toLong(16).toInt()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractHost(url: String): String {
        return try {
            val normalized = if (url.startsWith("http", ignoreCase = true) || url.startsWith("ftp", ignoreCase = true)) {
                url
            } else {
                "https://$url"
            }
            URI(normalized).host?.removePrefix("www.") ?: url.take(32)
        } catch (_: Exception) {
            url.take(32)
        }
    }

    fun looksLikeJson(text: String): Boolean {
        val t = text.trim()
        if (!((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]")))) {
            return false
        }
        return try {
            val element = JsonParser.parseString(t)
            element.isJsonObject || element.isJsonArray
        } catch (_: Exception) {
            false
        }
    }

    private fun detectCodeLanguage(text: String): String? {
        val lines = text.lines()
        if (lines.size < 2 && !text.contains(";") && !text.contains("=>") && !text.contains("SELECT ", ignoreCase = true)) {
            return null
        }
        val lower = text.lowercase()
        return when {
            lower.contains("fun ") && (lower.contains("val ") || lower.contains("var ") || lower.contains("package ")) -> "Kotlin"
            lower.contains("function ") || lower.contains("const ") && lower.contains("=>") || lower.contains("import ") && lower.contains("from ") -> "JS/TS"
            lower.contains("def ") && lower.contains(":") || lower.contains("import ") && lower.contains("print(") -> "Python"
            Regex("\\b(SELECT|INSERT|UPDATE|DELETE|CREATE TABLE)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "SQL"
            text.startsWith("<") && text.endsWith(">") && text.contains("</") -> "HTML/XML"
            lines.size >= 2 && (text.contains("{") && text.contains("}") && text.contains(";")) -> "Code"
            text.startsWith("curl ") || text.startsWith("git ") || text.startsWith("adb ") || text.startsWith("docker ") -> "Shell"
            else -> null
        }
    }
}
