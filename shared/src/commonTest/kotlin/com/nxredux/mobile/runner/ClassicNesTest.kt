package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ClassicNesTest {
    /** A GBA header's first [ClassicNes.HEAD_BYTES] bytes with game code [code] and the fixed 0x96 (unless [fixed] says otherwise). */
    private fun header(code: String, fixed: Int = 0x96): ByteArray = ByteArray(ClassicNes.HEAD_BYTES).also { h ->
        "PAC-MAN".encodeToByteArray().copyInto(h, 0xA0)
        code.encodeToByteArray().copyInto(h, 0xAC)
        "01".encodeToByteArray().copyInto(h, 0xB0)
        h[0xB2] = fixed.toByte()
    }

    @Test fun a_classic_nes_series_code_is_known() {
        assertTrue(ClassicNes.isClassicNes(header("FP7E")))   // Pac-Man
        assertTrue(ClassicNes.isClassicNes(header("FSMJ")))   // Famicom Mini: Super Mario Bros.
        assertTrue(ClassicNes.isClassicNes(header("FZLP")))
    }

    @Test fun a_normal_game_is_not() {
        assertFalse(ClassicNes.isClassicNes(header("BPEE")))   // Pokemon Emerald
        assertFalse(ClassicNes.isClassicNes(header("AMKE")))
    }

    @Test fun a_short_garbage_or_unread_head_is_not() {
        assertFalse(ClassicNes.isClassicNes(null))
        assertFalse(ClassicNes.isClassicNes(ByteArray(0)))
        assertFalse(ClassicNes.isClassicNes(header("FP7E").copyOf(0xB2)))   // ends before the fixed byte
        assertFalse(ClassicNes.isClassicNes(header("FP7E", fixed = 0)))     // no GBA header
        assertFalse(ClassicNes.isClassicNes(header("F\u0000\u0001 ")))     // not a game code
        assertFalse(ClassicNes.isClassicNes(ByteArray(ClassicNes.HEAD_BYTES) { 0x46 }))   // 'F' everywhere, no 0x96
    }

    @Test fun the_console_default_gpsp_gives_way_to_mgba() {
        assertEquals("MGBA", ClassicNes.launchTag("GBA", null) { header("FP7E") })
        assertEquals("GBA", ClassicNes.launchTag("GBA", null) { header("BPEE") })
    }

    @Test fun a_per_game_pick_is_kept_and_the_header_not_read() {
        assertEquals("GBA", ClassicNes.launchTag("GBA", "GBA") { fail("read") })
        assertEquals("MGBA", ClassicNes.launchTag("MGBA", "MGBA") { fail("read") })
    }

    @Test fun a_console_default_of_mgba_is_unchanged() {
        assertEquals("MGBA", ClassicNes.launchTag("MGBA", null) { fail("read") })
        assertEquals("GB", ClassicNes.launchTag("GB", null) { fail("read") })
    }
}
