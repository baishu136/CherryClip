package com.cherryclip.app

import com.cherryclip.app.model.TextTransformers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TextTransformersTest {

    @Test
    fun cleanTrackingUrlStripsUtmAndKeepsRealParams() {
        val dirty = "https://example.com/post?id=42&utm_source=twitter&fbclid=xyz&spm=123#top"
        val cleaned = TextTransformers.cleanTrackingUrl(dirty).getOrThrow()
        assertEquals("https://example.com/post?id=42#top", cleaned)
    }

    @Test
    fun jsonPrettyAndMinifyRoundTrip() {
        val raw = """{"b":2,"a":1}"""
        val pretty = TextTransformers.prettyJson(raw).getOrThrow()
        assertTrue(pretty.contains("\n"))
        val minified = TextTransformers.minifyJson(pretty).getOrThrow()
        assertEquals(raw, minified)
    }

    @Test
    fun base64AndUrlRoundTrip() {
        val text = "CherryClip 樱桃剪贴板!"
        val b64 = TextTransformers.encodeBase64(text)
        assertEquals(text, TextTransformers.decodeBase64(b64).getOrThrow())

        val urlEnc = TextTransformers.encodeUrl(text)
        assertEquals(text, TextTransformers.decodeUrl(urlEnc).getOrThrow())
    }

    @Test
    fun expandTemplateReplacesTokens() {
        val out = TextTransformers.expandSnippetTemplate(
            template = "Date={{date}} Clip={{clipboard}} ID={{uuid}}",
            currentClipboard = "hello",
            now = Date(1700000000000L)
        )
        assertTrue(out.contains("Clip=hello"))
        assertTrue(!out.contains("{{date}}"))
        assertTrue(!out.contains("{{uuid}}"))
    }
}
