package com.cherryclip.app.model

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.UUID

object TextTransformers {

    private val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid",
        "igshid", "si", "spm", "spm_id_from", "from_spmid", "vd_source",
        "share_source", "share_medium", "ref", "ref_src", "ref_url"
    )

    private val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val compactGson = GsonBuilder().disableHtmlEscaping().create()

    fun trimAndCleanBlankLines(text: String): String {
        return text.lines()
            .map { it.trimEnd() }
            .filterIndexed { index, line ->
                if (line.isNotBlank()) true
                else index > 0 && index < text.lines().lastIndex && text.lines()[index - 1].isNotBlank()
            }
            .joinToString("\n")
            .trim()
    }

    fun cleanTrackingUrl(text: String): Result<String> = runCatching {
        val trimmed = text.trim()
        val hasScheme = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
        val normalized = if (hasScheme) trimmed else "https://$trimmed"
        val uri = URI(normalized)
        val query = uri.rawQuery ?: return@runCatching normalized
        val kept = query.split("&")
            .filter { pair ->
                val key = pair.substringBefore("=").lowercase()
                key.isNotEmpty() && key !in trackingParams && !key.startsWith("utm_")
            }
        val cleanQuery = if (kept.isEmpty()) null else kept.joinToString("&")
        URI(
            uri.scheme,
            uri.rawAuthority,
            uri.rawPath,
            cleanQuery,
            uri.rawFragment
        ).toASCIIString()
    }

    fun prettyJson(text: String): Result<String> = runCatching {
        val el = JsonParser.parseString(text.trim())
        prettyGson.toJson(el)
    }

    fun minifyJson(text: String): Result<String> = runCatching {
        val el = JsonParser.parseString(text.trim())
        compactGson.toJson(el)
    }

    fun encodeBase64(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(StandardCharsets.UTF_8))
    }

    fun decodeBase64(text: String): Result<String> = runCatching {
        val decoded = Base64.getDecoder().decode(text.trim())
        String(decoded, StandardCharsets.UTF_8)
    }

    fun encodeUrl(text: String): String {
        return URLEncoder.encode(text, StandardCharsets.UTF_8.name())
    }

    fun decodeUrl(text: String): Result<String> = runCatching {
        URLDecoder.decode(text, StandardCharsets.UTF_8.name())
    }

    fun toUpperCase(text: String): String = text.uppercase(Locale.getDefault())

    fun toLowerCase(text: String): String = text.lowercase(Locale.getDefault())

    fun toTitleCase(text: String): String {
        return text.split(Regex("(\\s+)")).joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    fun extractLinks(text: String): String {
        val regex = Regex("https?://[^\\s\\u4e00-\\u9fa5\"'<>()]+", RegexOption.IGNORE_CASE)
        return regex.findAll(text).map { it.value }.distinct().joinToString("\n")
    }

    fun extractEmails(text: String): String {
        val regex = Regex("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        return regex.findAll(text).map { it.value }.distinct().joinToString("\n")
    }

    fun extractNumbers(text: String): String {
        val regex = Regex("-?\\d+(?:\\.\\d+)?")
        return regex.findAll(text).map { it.value }.joinToString("\n")
    }

    fun expandSnippetTemplate(template: String, currentClipboard: String = "", now: Date = Date()): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val weekdayFmt = SimpleDateFormat("EEEE", Locale.CHINA)

        return template
            .replace("{{date}}", dateFmt.format(now))
            .replace("{{time}}", timeFmt.format(now))
            .replace("{{datetime}}", dateTimeFmt.format(now))
            .replace("{{weekday}}", weekdayFmt.format(now))
            .replace("{{clipboard}}", currentClipboard)
            .replace("{{uuid}}", UUID.randomUUID().toString())
    }
}
