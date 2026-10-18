package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class LibraryScannerOutsideTest {
    // A home's games outside Roms are found as an extra folder's are; Roms, Bios, Saves and Collections are not walked.
    @Test fun games_outside_roms_are_found_and_the_home_folders_skipped() = runTest {
        val src = FakeRomsSource(mapOf(
            "H" to listOf("Roms", "Bios", "Saves", "Collections", "Game Boy Advance", "notes.txt", "README.md", "Tetris.gb"),
            "H/Roms" to listOf("Game Boy (GB)"), "H/Roms/Game Boy (GB)" to listOf("Zelda.gb"),
            "H/Bios" to listOf("GBA"), "H/Bios/GBA" to listOf("gba_bios.bin", "Bios.gba"),
            "H/Saves" to listOf("GBA"), "H/Saves/GBA" to listOf("Kirby.sav", "Kirby.gba"),
            "H/Collections" to listOf("Favs.txt"),
            "H/Game Boy Advance" to listOf("Kirby.gba", "readme.md"),
        ))
        val files = LibraryScanner(src).scanOutsideRoms(ContentRef("H"), emptyMap())
        assertEquals(setOf("Tetris.gb" to "GB", "Kirby.gba" to "GBA"), files.filter { it.tag != null }.map { it.entry.name to it.tag }.toSet())
        assertEquals(setOf("", "Game Boy Advance"), files.map { it.relativeDir }.toSet())
    }

    // A home at a card's root: the handheld's own folders (Emus, Tools ...), Android's and dot folders are not walked.
    @Test fun a_card_roots_system_and_handheld_folders_are_skipped() = runTest {
        val src = FakeRomsSource(mapOf(
            "H" to listOf("Roms", "Emus", "Tools", "Cheats", "Overlays", "Android", "DCIM", ".userdata", "Loose"),
            "H/Roms" to emptyList(),
            "H/Emus" to listOf("GBA.pak"), "H/Emus/GBA.pak" to listOf("test.gba"),
            "H/Tools" to listOf("tool.gb"), "H/Cheats" to listOf("x.gba"), "H/Overlays" to listOf("o.gb"),
            "H/Android" to listOf("data"), "H/Android/data" to listOf("a.gba"),
            "H/DCIM" to listOf("b.gb"), "H/.userdata" to listOf("c.gb"),
            "H/Loose" to listOf("Tetris.gb"),
        ))
        val files = LibraryScanner(src).scanOutsideRoms(ContentRef("H"), emptyMap())
        assertEquals(listOf("Tetris.gb"), files.map { it.entry.name })
    }
}
