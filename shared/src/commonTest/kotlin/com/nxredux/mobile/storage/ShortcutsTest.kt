package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcutsTest {
    @Test fun parses_paths_and_names_like_recents() {
        val list = Shortcuts.parse("/Roms/Game Boy (GB)/Tetris.gb\n/Roms/Nintendo DS (NDS)/mk.nds\tMario Kart DS\r\n\n")
        assertEquals(listOf(Shortcut("/Roms/Game Boy (GB)/Tetris.gb"), Shortcut("/Roms/Nintendo DS (NDS)/mk.nds", "Mario Kart DS")), list)
    }

    @Test fun render_sorts_by_name_ignoring_case_falling_back_to_the_path() {
        val list = listOf(Shortcut("/Roms/Z (Z)/z.gb", "beta"), Shortcut("/Roms/A (A)/a.gb", "Alpha"), Shortcut("/Roms/B (B)/Charlie.gb"))
        assertEquals("/Roms/B (B)/Charlie.gb\n/Roms/A (A)/a.gb\tAlpha\n/Roms/Z (Z)/z.gb\tbeta\n", Shortcuts.render(list))
    }

    @Test fun add_returns_the_sorted_list_dedupes_by_path_and_refuses_when_full() {
        val added = Shortcuts.add(listOf(Shortcut("/Roms/B (B)/b.gb", "Bee")), Shortcut("/Roms/A (A)/a.gb", "Ay"))!!
        assertEquals(listOf(Shortcut("/Roms/A (A)/a.gb", "Ay"), Shortcut("/Roms/B (B)/b.gb", "Bee")), added)
        val renamed = Shortcuts.add(added, Shortcut("/Roms/B (B)/b.gb", "Zed"))!!
        assertEquals(listOf(Shortcut("/Roms/A (A)/a.gb", "Ay"), Shortcut("/Roms/B (B)/b.gb", "Zed")), renamed)
        val full = (1..Shortcuts.MAX).map { Shortcut("/Roms/X (X)/$it.gb", "Game $it") }
        assertNull(Shortcuts.add(full, Shortcut("/Roms/X (X)/new.gb", "New")))
        assertEquals(Shortcuts.MAX, Shortcuts.add(full, Shortcut("/Roms/X (X)/3.gb", "Renamed"))!!.size)   // a known path never counts as new
    }

    @Test fun remove_and_rename_match_the_exact_path() {
        val list = listOf(Shortcut("/Roms/A (A)/a.gb", "Ay"), Shortcut("/Roms/B (B)/b.gb", "Bee"))
        assertEquals(listOf(Shortcut("/Roms/B (B)/b.gb", "Bee")), Shortcuts.remove(list, "/Roms/A (A)/a.gb"))
        assertEquals(list, Shortcuts.remove(list, "/Roms/A (A)/a.GB"))
        assertEquals(listOf(Shortcut("/Roms/B (B)/b.gb", "Bee"), Shortcut("/Roms/A (A)/a.gb", "Zulu")), Shortcuts.rename(list, "/Roms/A (A)/a.gb", "Zulu"))
        assertEquals(list, Shortcuts.rename(list, "/Roms/C (C)/c.gb", "Nope"))
    }
}
