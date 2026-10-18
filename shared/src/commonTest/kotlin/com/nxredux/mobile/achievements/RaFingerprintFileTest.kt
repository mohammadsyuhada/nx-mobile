package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RaFingerprintFileTest {
    private val h = "0123456789abcdef0123456789abcdef"
    private val one = """{"version":1,"files":{"E.gba":{"size":16,"mtime":1728800000,"console":5,"hash":"${h.uppercase()}","game":1234}}}"""

    @Test fun hit_needs_size_console_and_mtime() {
        val f = RaFingerprintFile.parse(one)
        assertEquals(RaFingerprint(h, 1234), f.lookup("E.gba", 16, 1728800000, 5))   // uppercase read, lowercase out
        assertNull(f.lookup("E.gba", 17, 1728800000, 5))
        assertNull(f.lookup("E.gba", 16, 1728800000, 6))
        assertNull(f.lookup("Other.gba", 16, 1728800000, 5))
    }
    @Test fun mtime_tolerance() {
        assertTrue(RaFingerprintFile.mtimeMatches(1000, 1000))
        assertTrue(RaFingerprintFile.mtimeMatches(1000, 1000 + 3600))
        assertTrue(RaFingerprintFile.mtimeMatches(100000, 100000 - 28800))
        assertTrue(RaFingerprintFile.mtimeMatches(100000, 100000 + 50400))
        assertFalse(RaFingerprintFile.mtimeMatches(1000, 1901))
        assertFalse(RaFingerprintFile.mtimeMatches(100000, 100000 + 54000))
    }
    @Test fun unknown_keys_and_foreign_entries_survive_a_write() {
        val f = RaFingerprintFile.parse("""{"version":1,"note":"hi","files":{"A.gba":{"size":1,"mtime":2,"console":5,"hash":"$h","game":0,"x":true}}}""")
        f.record("B.gba", 3, 4, 5, h, 7)
        val out = Json.parseToJsonElement(f.encode()).jsonObject
        assertEquals("hi", out["note"]!!.jsonPrimitive.content)
        val files = out["files"]!!.jsonObject
        assertEquals("true", files["A.gba"]!!.jsonObject["x"]!!.jsonPrimitive.content)
        assertEquals("7", files["B.gba"]!!.jsonObject["game"]!!.jsonPrimitive.content)
    }
    @Test fun an_equivalent_entry_is_not_rewritten() {
        val f = RaFingerprintFile.parse(one)
        f.record("E.gba", 16, 1728800000 + 3600, 5, h, 1234)   // tz-shifted mtime, same everything else
        assertFalse(f.changed)
        f.record("E.gba", 16, 1728800000, 5, h, 99)            // game changed
        assertTrue(f.changed)
    }
    @Test fun game_zero_is_stored_and_forget_removes() {
        val f = RaFingerprintFile.parse(null)
        f.record("N.gba", 1, 1, 5, h, 0)
        assertEquals(RaFingerprint(h, 0), f.lookup("N.gba", 1, 1, 5))
        f.forget("N.gba")
        assertNull(f.lookup("N.gba", 1, 1, 5))
    }
    @Test fun forget_with_a_hash_removes_only_the_entry_it_saw() {
        val f = RaFingerprintFile.parse(one)
        f.forget("E.gba", "f".repeat(32))   // re-recorded since (another hash): kept
        assertEquals(RaFingerprint(h, 1234), f.lookup("E.gba", 16, 1728800000, 5)); assertFalse(f.changed)
        f.forget("E.gba", h)   // the stored hash is uppercase: compared case-insensitively
        assertNull(f.lookup("E.gba", 16, 1728800000, 5)); assertTrue(f.changed)
    }
    @Test fun unreadable_or_wrong_shape_counts_as_empty() {
        assertNull(RaFingerprintFile.parse("{not json").lookup("E.gba", 16, 1728800000, 5))
        assertNull(RaFingerprintFile.parse("""{"files":[]}""").lookup("E.gba", 16, 1728800000, 5))
        assertTrue(RaFingerprintFile.parse("[]").encode().contains("\"version\":1"))
    }
    /** Device Sync keeps a handheld folder's entries by their key's first segment, the game's top-level name there. */
    @Test fun keys_are_paths_below_the_console_folder_as_written() {
        val e = """{"size":1,"mtime":2,"console":12,"hash":"$h","game":0}"""
        val f = RaFingerprintFile.parse("""{"version":1,"files":{"FF7 (Disc 1)/FF7.m3u":$e,"E.gba":$e,".sources/sd/Games/x.gba":$e}}""")
        assertEquals(listOf("FF7 (Disc 1)/FF7.m3u", "E.gba", ".sources/sd/Games/x.gba"), f.keys().toList())
        f.forget("E.gba"); f.forget(".sources/sd/Games/x.gba")
        val files = Json.parseToJsonElement(f.encode()).jsonObject["files"]!!.jsonObject
        assertEquals(setOf("FF7 (Disc 1)/FF7.m3u"), files.keys)
    }
}
