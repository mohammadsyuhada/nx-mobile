package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GamePathTest {
    @Test fun home_paths_are_the_handhelds_sd_relative_paths() {
        val ref = GameRef(GamePath.HOME, "Roms/Game Boy (GB)", "Tetris.gb")
        assertEquals("/Roms/Game Boy (GB)/Tetris.gb", GamePath.of(ref))
        assertEquals(ref, GamePath.split("/Roms/Game Boy (GB)/Tetris.gb"))
    }

    @Test fun a_home_disc_folder_sheet_keeps_its_nested_dir() {
        val ref = GameRef(GamePath.HOME, "Roms/PlayStation (PS)/FF7", "FF7.m3u")
        assertEquals("/Roms/PlayStation (PS)/FF7/FF7.m3u", GamePath.of(ref))
        assertEquals(ref, GamePath.split("/Roms/PlayStation (PS)/FF7/FF7.m3u"))
    }

    @Test fun extra_paths_carry_the_id_and_an_optional_dir() {
        assertEquals("/Extra/Roms-73a74a58/Kirby.gba", GamePath.of(GameRef("Roms-73a74a58", "", "Kirby.gba")))
        assertEquals("/Extra/Roms-73a74a58/a/b/Kirby.gba", GamePath.of(GameRef("Roms-73a74a58", "a/b", "Kirby.gba")))
        assertEquals(GameRef("Roms-73a74a58", "", "Kirby.gba"), GamePath.split("/Extra/Roms-73a74a58/Kirby.gba"))
        assertEquals(GameRef("Roms-73a74a58", "a/b", "Kirby.gba"), GamePath.split("/Extra/Roms-73a74a58/a/b/Kirby.gba"))
    }

    @Test fun anything_else_is_null() {
        assertNull(GamePath.split("/Tools/x.pak"))
        assertNull(GamePath.split("/Roms/Tetris.gb"))          // no system folder
        assertNull(GamePath.split("/Roms/Game Boy (GB)/"))     // no file
        assertNull(GamePath.split("/Extra/"))
        assertNull(GamePath.split("/Extra/id"))
        assertNull(GamePath.split("/Extra/id/dir/"))
        assertNull(GamePath.split(""))
    }
}
