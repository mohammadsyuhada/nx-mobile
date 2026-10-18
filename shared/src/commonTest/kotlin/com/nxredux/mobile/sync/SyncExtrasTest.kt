package com.nxredux.mobile.sync

import com.nxredux.mobile.library.IndexedGame
import com.nxredux.mobile.library.IndexedSystem
import com.nxredux.mobile.library.LibraryIndex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncExtrasTest {
    private val homeRoms = "/storage/emulated/0/NX/Roms"
    private val roots = listOf(ExtraRoot(0, "sd", "/storage/AAAA-BBBB/Games"))
    private fun g(source: String, dir: String, file: String, lookupDir: String = dir, boxart: String? = null) =
        IndexedGame(source = source, dir = dir, file = file, ref = "file:///x/$file", folder = "file:///x", name = file,
            boxart = boxart, lookupDir = lookupDir)
    private fun index(tag: String, vararg games: IndexedGame, unassigned: Boolean = false) =
        LibraryIndex(version = LibraryIndex.VERSION, home = "file:///storage/emulated/0/NX", extras = emptyList(), defaultTags = emptyMap(),
            systems = listOf(IndexedSystem(tag = tag, displayName = tag, unassigned = unassigned, games = games.toList())))

    @Test fun loose_game_with_companions_and_art() = runTest {
        val art = "file://$homeRoms/.sources/sd/PS/.media/boxart/Crash.png"
        val m = SyncExtras.units(index("PS", g("sd", "PS", "Crash.cue", boxart = art)), roots, homeRoms) { listOf("Crash.bin") }
        assertEquals(listOf(ExtraUnit(0, "PS", "PS", listOf("Crash.cue", "Crash.bin"), "Crash.cue",
            ".sources/sd/PS/Crash.cue", "Crash.cue", listOf(".sources/sd/PS/.media/boxart/Crash.png"))), m.units)
    }

    @Test fun disc_folder_game_sends_its_folder() = runTest {
        val m = SyncExtras.units(index("DC", g("sd", "DC/Sonic", "Sonic.gdi", lookupDir = "DC")), roots, homeRoms) { error("not asked for a folder game") }
        val u = m.units.single()
        assertEquals("DC", u.parent); assertEquals(listOf("Sonic"), u.names); assertEquals("Sonic", u.landing)
        assertEquals(".sources/sd/DC/Sonic/Sonic.gdi", u.key); assertEquals("Sonic/Sonic.gdi", u.landingKey)
    }

    @Test fun home_games_unassigned_and_unknown_extras_are_left_out() = runTest {
        val ix = index("GB", g("home", "Roms/Game Boy (GB)", "a.gb"), g("gone", "", "b.gb"))
        assertEquals(emptyList(), SyncExtras.units(ix, roots, homeRoms) { emptyList() }.units)
        val un = index("UNASSIGNED", g("sd", "", "c.bin"), unassigned = true)
        assertEquals(emptyList(), SyncExtras.units(un, roots, homeRoms) { emptyList() }.units)
    }

    @Test fun root_level_extra_game_has_empty_parent() = runTest {
        val u = SyncExtras.units(index("GBA", g("sd", "", "Zelda.gba")), roots, homeRoms) { emptyList() }.units.single()
        assertEquals("", u.parent); assertEquals(".sources/sd/Zelda.gba", u.key)
    }

    @Test fun manifest_round_trips_and_bad_text_is_empty() {
        val m = ExtrasManifest(units = listOf(ExtraUnit(1, "GB", "", listOf("a.gb"), "a.gb", ".sources/x/a.gb", "a.gb", emptyList())))
        assertEquals(m, SyncExtras.decode(SyncExtras.encode(m)))
        assertEquals(ExtrasManifest(), SyncExtras.decode("{oops"))
        assertEquals(ExtrasManifest(), SyncExtras.decode(null))
    }
}
