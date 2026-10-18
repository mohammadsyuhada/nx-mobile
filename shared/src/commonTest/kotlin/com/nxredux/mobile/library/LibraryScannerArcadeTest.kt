package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.MemorySidecarStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class LibraryScannerArcadeTest {
    private val fbn = "H/Roms/Arcade (FBN)"
    private val gb = "H/Roms/Game Boy (GB)"
    private val table = ArcadeSets.parse("mslug2\tMetal Slug 2\t\tneogeo\t0\nneogeo\tNeo Geo\t\t\t1\n")

    private suspend fun scanHome(
        dirs: Map<String, List<String>>,
        texts: Map<String, String> = emptyMap(),
        arcadeSets: (() -> ArcadeSets)? = null,
        naomiSets: () -> NaomiSets = { NaomiSets.EMPTY },
    ): LibraryIndex {
        val src = FakeRomsSource(dirs = mapOf("H" to listOf("Roms")) + dirs, texts = texts)
        val sidecars = Sidecars(src, MemorySidecarStore()) { ContentRef("H") }
        val scanner = LibraryScanner(src, sidecars, arcadeSets ?: { ArcadeSets.EMPTY }, naomiSets)
        return scanner.scan(ContentRef("H"), emptyList(), emptyMap())
    }

    private suspend fun arcadeHome(arcadeSets: (() -> ArcadeSets)? = null) = scanHome(
        mapOf("H/Roms" to listOf("Arcade (FBN)"), fbn to listOf("mslug2.zip", "neogeo.zip", "MyHack.zip", "map.txt")),
        mapOf("$fbn/map.txt" to "MyHack.zip\tMy Hack\n"),
        arcadeSets,
    )

    private fun LibraryIndex.names(tag: String) = systems.first { it.tag == tag }.games.map { it.name }

    @Test fun arcade_names_come_from_the_table_and_bios_sets_are_hidden() = runTest {
        val index = arcadeHome { table }
        assertEquals(listOf("Metal Slug 2", "My Hack"), index.names("FBN"))
        assertEquals(listOf("mslug2.zip", "MyHack.zip"), index.systems.first { it.tag == "FBN" }.games.map { it.file })
    }

    @Test fun without_a_table_names_are_stems() = runTest {
        assertEquals(listOf("mslug2", "My Hack", "neogeo"), arcadeHome().names("FBN"))
    }

    @Test fun dot_alias_hides_an_entry_on_any_system() = runTest {
        val index = scanHome(
            mapOf("H/Roms" to listOf("Game Boy (GB)"), gb to listOf("Tetris.gb", "bios.gb", "map.txt")),
            mapOf("$gb/map.txt" to "bios.gb\t.hidden\n"),
        )
        assertEquals(listOf("Tetris"), index.names("GB"))
    }

    @Test fun table_titles_are_only_for_arcade() = runTest {
        val index = scanHome(mapOf("H/Roms" to listOf("Game Boy (GB)"), gb to listOf("mslug2.gb")), arcadeSets = { table })
        assertEquals(listOf("mslug2"), index.names("GB"))
    }

    @Test fun the_table_is_resolved_once_per_scan_and_only_for_arcade() = runTest {
        var calls = 0
        arcadeHome { calls++; table }
        assertEquals(1, calls)
        calls = 0
        scanHome(mapOf("H/Roms" to listOf("Game Boy (GB)"), gb to listOf("mslug2.gb", "Tetris.gb")), arcadeSets = { calls++; table })
        assertEquals(0, calls)
    }

    private val dc = "H/Roms/Dreamcast (DC)"
    private val naomi = NaomiSets.parse("awbios\t\t\tawbios\nmslug6\t\tMetal Slug 6\tawbios\nnaomi\t\t\tnaomi\n")

    private suspend fun dcHome(naomiSets: () -> NaomiSets) = scanHome(
        mapOf(
            "H/Roms" to listOf("Dreamcast (DC)"),
            dc to listOf("mslug6.zip", "naomi.zip", "awbios.7z", "Crazy Taxi (USA).zip", "Shenmue.chd", "naomi.chd"),
        ),
        naomiSets = naomiSets,
    )

    @Test fun dc_sets_take_flycasts_titles_and_its_bios_archives_are_hidden() = runTest {
        val index = dcHome { naomi }
        assertEquals(listOf("Crazy Taxi", "Metal Slug 6", "naomi", "Shenmue"), index.names("DC"))
        assertEquals(
            listOf("Crazy Taxi (USA).zip", "mslug6.zip", "naomi.chd", "Shenmue.chd"),
            index.systems.first { it.tag == "DC" }.games.map { it.file },
        )
    }

    @Test fun dc_without_a_table_lists_every_archive_by_its_stem() = runTest {
        assertEquals(listOf("awbios", "Crazy Taxi", "mslug6", "naomi", "naomi", "Shenmue").sorted(), dcHome { NaomiSets.EMPTY }.names("DC").sorted())
    }

    @Test fun the_naomi_table_is_read_once_and_only_for_dc() = runTest {
        var calls = 0
        dcHome { calls++; naomi }
        assertEquals(1, calls)
        calls = 0
        scanHome(mapOf("H/Roms" to listOf("Game Boy (GB)"), gb to listOf("Tetris.gb")), naomiSets = { calls++; naomi })
        assertEquals(0, calls)
    }
}
