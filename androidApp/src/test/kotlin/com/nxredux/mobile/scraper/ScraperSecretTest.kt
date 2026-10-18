package com.nxredux.mobile.scraper

import com.nxredux.mobile.BuildConfig
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScraperSecretTest {
    /** build.gradle.kts's encodedSecret, mirrored: SHA-256("nx-mobile/<name>/<value>") as the repeated XOR key. */
    private fun encode(name: String, value: String): Pair<ByteArray, ByteArray> {
        val plain = value.toByteArray(Charsets.UTF_8)
        if (plain.isEmpty()) return ByteArray(0) to ByteArray(0)
        val key = MessageDigest.getInstance("SHA-256").digest("nx-mobile/$name/$value".toByteArray(Charsets.UTF_8))
        return ScraperSecret.xor(plain, key) to key
    }

    @Test fun decodeOfEncodeIsTheValue() {
        for (value in listOf("a", "ZZtestSecretZZ", "p@ss\"w\\rd with spaces", "ünïcödé ✓ 日本", "x".repeat(100))) {
            val (enc, key) = encode("SS_DEV_PASSWORD", value)
            assertEquals(value, ScraperSecret.decode(enc, key))
            assertFalse("plain bytes must not survive", enc.contentEquals(value.toByteArray(Charsets.UTF_8)))
        }
    }

    @Test fun emptySecretIsEmpty() {
        val (enc, key) = encode("SS_DEV_ID", "")
        assertEquals(0, enc.size)
        assertEquals("", ScraperSecret.decode(enc, key))
        assertEquals("", ScraperSecret.decode(ByteArray(0), ByteArray(0)))
    }

    @Test fun xorIsItsOwnInverse() {
        val data = ByteArray(70) { it.toByte() }
        val key = byteArrayOf(-1, 0, 90, 127, -128)
        assertArrayEquals(data, ScraperSecret.xor(ScraperSecret.xor(data, key), key))
    }

    @Test fun buildConfigFieldsAreWellFormed() {
        for ((enc, key) in listOf(
            BuildConfig.SS_DEV_ID_ENC to BuildConfig.SS_DEV_ID_KEY,
            BuildConfig.SS_DEV_PASSWORD_ENC to BuildConfig.SS_DEV_PASSWORD_KEY,
        )) {
            assertTrue(if (enc.isEmpty()) key.isEmpty() else key.size == 32)
            assertEquals(enc.size, ScraperSecret.decode(enc, key).toByteArray(Charsets.UTF_8).size)
        }
    }
}
