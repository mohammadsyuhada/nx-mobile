package com.nxredux.mobile.library.disc

import com.nxredux.mobile.library.FakeRomsSource
import com.nxredux.mobile.runner.ContentRef
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscProbeTest {
    private fun ps1Bin(): ByteArray = ByteArray(17 * 2352).also { b ->
        val at = 16 * 2352 + 24
        b[at] = 1; "CD001".encodeToByteArray().copyInto(b, at + 1); "PLAYSTATION".encodeToByteArray().copyInto(b, at + 8)
        b[0] = 0; for (i in 1..10) b[i] = 0xFF.toByte(); b[15] = 2
    }

    internal fun ps1BinBytes() = ps1Bin()

    @Test fun a_ps1_bin_and_its_cue() = runTest {
        val src = FakeRomsSource(mapOf("d" to listOf("G.bin", "G.cue")), texts = mapOf("d/G.cue" to "FILE \"G.bin\" BINARY\n TRACK 01 MODE2/2352\n  INDEX 01 00:00:00\n"), bytes = mapOf("d/G.bin" to ps1Bin()))
        val dir = src.listChildren(ContentRef("d"))
        val probe = DiscProbe(src)
        assertEquals("ps", probe.identify(dir.first { it.name == "G.bin" }, dir))
        assertEquals("ps", probe.identify(dir.first { it.name == "G.cue" }, dir))
    }

    @Test fun saturn_and_unknowns_stay_unplaced() = runTest {
        val sat = ByteArray(2048 * 17).also { "SEGA SEGASATURN ".encodeToByteArray().copyInto(it, 0) }
        val src = FakeRomsSource(mapOf("d" to listOf("S.iso", "M.cue")), texts = mapOf("d/M.cue" to "FILE \"gone.bin\" BINARY\n TRACK 01 MODE1/2352\n"), bytes = mapOf("d/S.iso" to sat))
        val dir = src.listChildren(ContentRef("d"))
        assertNull(DiscProbe(src).identify(dir.first { it.name == "S.iso" }, dir))
        assertNull(DiscProbe(src).identify(dir.first { it.name == "M.cue" }, dir))      // Review Focus 2: its bin is missing
    }

    @Test fun chd_by_metadata_then_by_its_sectors() = runTest {
        val gd = ChdHeaderTest().chdBytes("CHGD"); val cd = ChdHeaderTest().chdBytes("CHT2")
        val src = FakeRomsSource(mapOf("d" to listOf("D.chd", "S.chd")), bytes = mapOf("d/D.chd" to gd, "d/S.chd" to cd))
        val dir = src.listChildren(ContentRef("d"))
        val segaZero = ByteArray(2048).also { "SEGADISCSYSTEM  ".encodeToByteArray().copyInto(it, 0) }
        val probe = DiscProbe(src) { _, _ -> listOf(segaZero, null, null) }
        assertEquals("dc", probe.identify(dir.first { it.name == "D.chd" }, dir))
        assertEquals("segacd", probe.identify(dir.first { it.name == "S.chd" }, dir))
        assertNull(DiscProbe(src).identify(dir.first { it.name == "S.chd" }, dir))       // no CHD reader: a CD CHD stays unplaced
    }
}
