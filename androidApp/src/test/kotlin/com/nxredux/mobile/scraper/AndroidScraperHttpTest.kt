package com.nxredux.mobile.scraper

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidScraperHttpTest {
    @Test fun aBodyWithinTheCapIsReadWhole() {
        val bytes = ByteArray(10_000) { it.toByte() }
        assertArrayEquals(bytes, BoundedBody.read(ByteArrayInputStream(bytes), max = 10_000))
        assertArrayEquals(ByteArray(0), BoundedBody.read(ByteArrayInputStream(ByteArray(0)), max = 10))
    }

    @Test fun aBodyOverTheCapIsNull() {
        assertNull(BoundedBody.read(ByteArrayInputStream(ByteArray(10_001)), max = 10_000))
    }
}
