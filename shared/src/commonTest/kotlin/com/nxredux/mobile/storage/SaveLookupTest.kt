package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveLookupTest {
    @Test fun the_configured_name_wins_when_it_exists() {
        assertEquals("a", SaveLookup.pick(listOf("a", "b"), mapOf("a" to 1L, "b" to 9L)::get))
    }
    @Test fun otherwise_the_newest_other_name() {
        assertEquals("c", SaveLookup.pick(listOf("a", "b", "c"), mapOf("b" to 5L, "c" to 9L)::get))
    }
    @Test fun none_exists() {
        assertNull(SaveLookup.pick(listOf("a", "b"), emptyMap<String, Long>()::get))
        assertNull(SaveLookup.pick(emptyList(), mapOf("a" to 1L)::get))
    }
    @Test fun ordered_is_the_configured_name_then_the_others_newest_first() {
        assertEquals(listOf("a", "c", "b"), SaveLookup.ordered(listOf("a", "b", "c", "d"), mapOf("a" to 1L, "b" to 5L, "c" to 9L)::get))
        assertEquals(listOf("c", "b"), SaveLookup.ordered(listOf("a", "b", "c"), mapOf("b" to 5L, "c" to 9L)::get))
        assertEquals(emptyList(), SaveLookup.ordered(listOf("a"), emptyMap<String, Long>()::get))
    }
    @Test fun a_rastate_header_is_skipped_and_raw_bytes_pass() {
        val payload = byteArrayOf(1, 2, 3)
        val headed = "RASTATE".encodeToByteArray() + ByteArray(9) + payload
        assertContentEquals(payload, StateBytes.stripRaStateHeader(headed))
        assertContentEquals(payload, StateBytes.stripRaStateHeader(payload))
        assertContentEquals("RASTAT".encodeToByteArray(), StateBytes.stripRaStateHeader("RASTAT".encodeToByteArray()))
    }
}
