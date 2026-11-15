package com.cherryclip.app

import com.cherryclip.app.model.ClipType
import com.cherryclip.app.model.ContentClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentClassifierTest {

    @Test
    fun classifyUrlAndExtractHost() {
        val result = ContentClassifier.classify("https://github.com/CherryHQ/cherry-studio-app?utm_source=test")
        assertEquals(ClipType.URL, result.type)
        assertEquals("github.com", result.metadata)
    }

    @Test
    fun classifyColorHexAndRgb() {
        val hex = ContentClassifier.classify("#ff5757")
        assertEquals(ClipType.COLOR, hex.type)
        assertEquals("#FF5757", hex.metadata)

        val shortHex = ContentClassifier.classify("#abc")
        assertEquals(ClipType.COLOR, shortHex.type)
        assertEquals("#AABBCC", shortHex.metadata)

        val rgb = ContentClassifier.classify("rgb(18, 136, 70)")
        assertEquals(ClipType.COLOR, rgb.type)
        assertEquals("#128846", rgb.metadata)
    }

    @Test
    fun classifyOtpAndExtractCode() {
        val otp = ContentClassifier.classify("【Cherry】您的登录验证码是 482910，5分钟内有效。")
        assertEquals(ClipType.OTP, otp.type)
        assertEquals("482910", otp.metadata)
    }

    @Test
    fun classifyJsonAndCode() {
        val json = ContentClassifier.classify("""{"name":"CherryClip","ok":true}""")
        assertEquals(ClipType.CODE, json.type)
        assertEquals("JSON", json.metadata)

        val kotlin = ContentClassifier.classify("package com.cherry\nfun hello() { val x = 1 }")
        assertEquals(ClipType.CODE, kotlin.type)
        assertEquals("Kotlin", kotlin.metadata)
    }

    @Test
    fun classifyEmailPhoneAndSensitive() {
        assertEquals(ClipType.EMAIL, ContentClassifier.classify("hello@cherry-ai.com").type)
        assertEquals(ClipType.PHONE, ContentClassifier.classify("+86 138-0013-8000").type)

        val secret = ContentClassifier.classify("sk-proj1234567890abcdef12345")
        assertTrue(secret.isSensitive)

        val normal = ContentClassifier.classify("今天下午三点开会")
        assertFalse(normal.isSensitive)
        assertEquals(ClipType.TEXT, normal.type)
    }
}
