package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecentsTest {
    @Test fun parses_paths_and_aliases_skipping_blanks() {
        val list = Recents.parse("/Roms/Game Boy (GB)/Tetris.gb\n\n/Roms/Nintendo DS (NDS)/mk.nds\tMario Kart DS\r\n")
        assertEquals(listOf(Recent("/Roms/Game Boy (GB)/Tetris.gb"), Recent("/Roms/Nintendo DS (NDS)/mk.nds", "Mario Kart DS")), list)
    }

    @Test fun round_trips_real_names() {
        val list = listOf(Recent("/Roms/Game Boy (GB)/Pokémon - Red (USA, Europe).gb", "Pokémon Red"), Recent("/Roms/Game Boy (GB)/Tetris.gb"))
        assertEquals(list, Recents.parse(Recents.render(list)))
        assertEquals("/Roms/Game Boy (GB)/Pokémon - Red (USA, Europe).gb\tPokémon Red\n/Roms/Game Boy (GB)/Tetris.gb\n", Recents.render(list))
    }

    @Test fun add_moves_to_top_dedups_and_caps() {
        val start = (1..24).map { Recent("/Roms/X (X)/$it.gb") }
        val added = Recents.add(start, Recent("/Roms/X (X)/5.gb", "Five"))
        assertEquals(Recent("/Roms/X (X)/5.gb", "Five"), added.first())
        assertEquals(24, added.size)
        assertEquals(1, added.count { it.path == "/Roms/X (X)/5.gb" })
        val capped = Recents.add(start, Recent("/Roms/X (X)/new.gb"))
        assertEquals(24, capped.size)
        assertEquals("/Roms/X (X)/new.gb", capped.first().path)
        assertEquals("/Roms/X (X)/23.gb", capped.last().path)
    }

    @Test fun re_adding_with_null_alias_keeps_old_alias() {
        val list = listOf(Recent("/Roms/B (B)/b.gb"), Recent("/Roms/A (A)/a.gb", "Alias A"))
        assertEquals(listOf(Recent("/Roms/A (A)/a.gb", "Alias A"), Recent("/Roms/B (B)/b.gb")), Recents.add(list, Recent("/Roms/A (A)/a.gb")))
    }

    @Test fun remove_drops_by_path() {
        val list = listOf(Recent("/Roms/A (A)/a.gb"), Recent("/Roms/B (B)/b.gb"))
        assertEquals(listOf(Recent("/Roms/B (B)/b.gb")), Recents.remove(list, "/Roms/A (A)/a.gb"))
    }

    @Test fun home_paths_round_trip_through_recent_refs() {
        val ref = GameRef(Recents.HOME, "Roms/Game Boy (GB)", "Tetris.gb")
        assertEquals("/Roms/Game Boy (GB)/Tetris.gb", GamePath.of(ref))
        assertEquals(ref, GamePath.split("/Roms/Game Boy (GB)/Tetris.gb"))
    }
    @Test fun extra_paths_carry_the_folder_id_and_relative_dir() {
        assertEquals("/Extra/0a1b2c3d/Tetris.gb", GamePath.of(GameRef("0a1b2c3d", "", "Tetris.gb")))
        assertEquals("/Extra/0a1b2c3d/a/b/Tetris.gb", GamePath.of(GameRef("0a1b2c3d", "a/b", "Tetris.gb")))
        assertEquals(GameRef("0a1b2c3d", "", "Tetris.gb"), GamePath.split("/Extra/0a1b2c3d/Tetris.gb"))
        assertEquals(GameRef("0a1b2c3d", "a/b", "Tetris.gb"), GamePath.split("/Extra/0a1b2c3d/a/b/Tetris.gb"))
        assertNull(GamePath.split("/Extra/0a1b2c3d"))
        assertNull(GamePath.split("/Extra/0a1b2c3d/"))
        assertNull(GamePath.split("/Extra//Tetris.gb"))
        assertNull(GamePath.split("/Tools/x"))
    }

    @Test fun update_alias_keeps_the_order_and_ignores_unknown_paths() {
        val list = listOf(Recent("/Roms/B (B)/b.gb"), Recent("/Roms/A (A)/a.gb", "Alias A"))
        assertEquals(listOf(Recent("/Roms/B (B)/b.gb", "Bee"), Recent("/Roms/A (A)/a.gb", "Alias A")), Recents.updateAlias(list, "/Roms/B (B)/b.gb", "Bee"))
        assertEquals(list, Recents.updateAlias(list, "/Roms/C (C)/c.gb", "Cee"))
    }

    @Test fun split_path_resolves_a_nested_home_disc_folder() {
        assertEquals(GameRef(GamePath.HOME, "Roms/PlayStation (PS)/FF7", "FF7.m3u"), GamePath.split("/Roms/PlayStation (PS)/FF7/FF7.m3u"))
    }
}
