package com.nxredux.mobile.achievements

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaFingerprintMergeTest {
    private fun e(mtime: Long, hash: String) = """{"size":1,"mtime":$mtime,"console":4,"hash":"$hash","game":7}"""
    private fun file(vararg entries: Pair<String, String>) =
        """{"version":1,"files":{${entries.joinToString(",") { (k, v) -> "\"$k\":$v" }}}}"""
    private fun files(text: String?) = Json.parseToJsonElement(text!!).jsonObject["files"]!!.jsonObject
    private val a = "0123456789abcdef0123456789abcdef"
    private val b = "fedcba9876543210fedcba9876543210"

    @Test fun union_and_newer_mtime_wins() {
        val r = RaFingerprintMerge.plan(file("x.gb" to e(10, a), "y.gb" to e(5, a)), file("y.gb" to e(9, b), "z.gb" to e(1, b)), emptyList(), emptyList())
        val own = files(r.own); val peer = files(r.peer)
        assertEquals(setOf("x.gb", "y.gb", "z.gb"), own.keys); assertEquals(own.keys, peer.keys)
        assertEquals(b, own["y.gb"]!!.jsonObject["hash"]!!.jsonPrimitive.content)
    }

    @Test fun a_tie_keeps_each_sides_own_entry() {
        val r = RaFingerprintMerge.plan(file("y.gb" to e(5, a)), file("y.gb" to e(5, b)), emptyList(), emptyList())
        assertNull(r.own)   // unchanged
        assertNull(r.peer)  // the peer's own entry is kept: nothing new for it either
    }

    @Test fun sources_keys_stay_home_and_sent_extras_are_rekeyed() {
        val own = file("x.gb" to e(10, a), ".sources/sd/GB/t.gb" to e(3, b))
        val r = RaFingerprintMerge.plan(own, null, listOf(Rekey(".sources/sd/GB/t.gb", "t.gb")), emptyList())
        assertNull(r.own)
        val peer = files(r.peer)
        assertEquals(setOf("x.gb", "t.gb"), peer.keys)
    }

    @Test fun peer_phone_extras_land_rekeyed_in_own_file() {
        val peer = file(".sources/p1/Disc/g.cue" to e(4, a))
        val r = RaFingerprintMerge.plan(null, peer, emptyList(), listOf(Rekey(".sources/p1/Disc/g.cue", "Disc/g.cue")))
        assertEquals(setOf("Disc/g.cue"), files(r.own).keys)
        assertNull(r.peer)   // its .sources entry is its own; nothing of ours to add
    }

    @Test fun bad_peer_file_is_empty() {
        val r = RaFingerprintMerge.plan(file("x.gb" to e(1, a)), "not json", emptyList(), emptyList())
        assertNull(r.own)
        assertTrue(files(r.peer).containsKey("x.gb"))
    }

    @Test fun nothing_on_either_side_writes_nothing() {
        assertEquals(MergedHashes(null, null), RaFingerprintMerge.plan(null, null, emptyList(), emptyList()))
    }
}
