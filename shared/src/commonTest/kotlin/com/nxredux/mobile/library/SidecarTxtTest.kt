package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals

class SidecarTxtTest {
    @Test fun parses_file_to_uppercase_tag_and_skips_bad_lines() {
        val map = SidecarTxt.parse("Tetris.gb\tgb\r\nno tab here\n\tMGBA\nMario.gba\tMGBA\nWeird.bin\tFUTURE\n")
        assertEquals(mapOf("Tetris.gb" to "GB", "Mario.gba" to "MGBA", "Weird.bin" to "FUTURE"), map)
    }
    @Test fun render_is_sorted_and_round_trips() {
        val text = SidecarTxt.render(mapOf("b.gba" to "MGBA", "a.gb" to "GB"))
        assertEquals("a.gb\tGB\nb.gba\tMGBA\n", text)
        assertEquals(mapOf("a.gb" to "GB", "b.gba" to "MGBA"), SidecarTxt.parse(text))
    }
    @Test fun with_adds_or_replaces_one_entry_and_keeps_unknown_tags() {
        val text = SidecarTxt.with("a.gb\tGB\nx.bin\tFUTURE\n", "a.gb", "MGBA")
        assertEquals("a.gb\tMGBA\nx.bin\tFUTURE\n", text)
        assertEquals("n.nds\tNDS\n", SidecarTxt.with(null, "n.nds", "nds"))
    }
    @Test fun file_names_with_surrounding_spaces_are_kept_exactly() {
        // " Tetris .gb" is a legal file name; trimming it would key the wrong file.
        assertEquals(mapOf(" Tetris .gb" to "GB"), SidecarTxt.parse(" Tetris .gb\tGB\n"))
        assertEquals(" Tetris .gb\tGB\n", SidecarTxt.with(null, " Tetris .gb", "GB"))
    }
    @Test fun a_file_name_containing_a_tab_is_refused() {
        assertEquals("", SidecarTxt.with(null, "bad\tname.gb", "GB"))
    }
}
