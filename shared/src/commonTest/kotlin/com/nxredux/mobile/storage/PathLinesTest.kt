package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class PathLinesTest {
    @Test fun parses_one_path_per_line_dropping_blanks_and_duplicates() {
        assertEquals(
            listOf("/Roms/A (A)/a.gb", "/Extra/id/b.gba"),
            PathLines.parse("/Roms/A (A)/a.gb\r\n\n/Extra/id/b.gba\n/Roms/A (A)/a.gb\n"),
        )
    }

    @Test fun renders_with_a_trailing_newline_and_round_trips() {
        val list = listOf("/Roms/A (A)/Pokémon - Red (USA, Europe).gb", "/Roms/B (B)/b.gb")
        assertEquals("/Roms/A (A)/Pokémon - Red (USA, Europe).gb\n/Roms/B (B)/b.gb\n", PathLines.render(list))
        assertEquals(list, PathLines.parse(PathLines.render(list)))
        assertEquals("", PathLines.render(emptyList()))
    }

    @Test fun add_appends_once_and_remove_drops_by_path() {
        val list = listOf("/Roms/A (A)/a.gb")
        assertEquals(listOf("/Roms/A (A)/a.gb", "/Roms/B (B)/b.gb"), PathLines.add(list, "/Roms/B (B)/b.gb"))
        assertEquals(list, PathLines.add(list, "/Roms/A (A)/a.gb"))
        assertEquals(emptyList<String>(), PathLines.remove(list, "/Roms/A (A)/a.gb"))
        assertEquals(list, PathLines.remove(list, "/Roms/Z (Z)/z.gb"))
    }
}
