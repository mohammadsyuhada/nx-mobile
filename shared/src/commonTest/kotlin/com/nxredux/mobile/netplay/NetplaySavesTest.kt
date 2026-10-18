package com.nxredux.mobile.netplay

import com.nxredux.mobile.library.NaomiSets
import com.nxredux.mobile.storage.NxPaths
import com.nxredux.mobile.storage.SaveNaming
import com.nxredux.mobile.storage.StateFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetplaySavesTest {
    @Test fun the_host_names_saves_after_the_entry_the_runner_unpacks() {
        val listed = listOf("readme.txt", "Game (USA).gba", "__MACOSX/._Game (USA).gba")
        assertEquals("Game (USA).gba", NetplaySaves.hostInnerFileName("GBA", "Game.zip", NaomiSets.EMPTY) { listed })
        assertEquals("rom.bin", NetplaySaves.hostInnerFileName("GBA", "Game.7z", NaomiSets.EMPTY) { listOf("dir/", "dir/rom.bin") })
        // not an archive, or one the core loads as is: no listing, no inner name
        var listedAtAll = false
        assertNull(NetplaySaves.hostInnerFileName("GBA", "Game.gba", NaomiSets.EMPTY) { listedAtAll = true; listed })
        assertNull(NetplaySaves.hostInnerFileName("FBN", "sf2.zip", NaomiSets.EMPTY) { listedAtAll = true; listed })
        assertFalse(listedAtAll)
        // unlistable, or no single ROM (the runner refuses the launch): the archive name
        assertNull(NetplaySaves.hostInnerFileName("GBA", "Game.zip", NaomiSets.EMPTY) { null })
        assertNull(NetplaySaves.hostInnerFileName("GBA", "Game.zip", NaomiSets.EMPTY) { listOf("a.gba", "b.gba") })
    }

    @Test fun only_srm_and_sav_travel() {
        assertEquals("nxsave.srm", NetplaySaves.wireName("Super Mario World (USA).srm", "Super Mario World (USA)"))
        assertEquals("nxsave.sav", NetplaySaves.wireName("Pokemon (USA).sav", "Pokemon (USA)"))
        assertNull(NetplaySaves.wireName("Sonic CD (USA).brm", "Sonic CD (USA)"))
        assertNull(NetplaySaves.wireName("Game.rtc", "Game"))
        assertNull(NetplaySaves.wireName("Super Mario World (USA).srm.bak", "Super Mario World (USA)"))
        assertNull(NetplaySaves.wireName("Other Game.srm", "Super Mario World (USA)"))
        assertEquals("My Game (EU).sav", NetplaySaves.localName("nxsave.sav", "My Game (EU)"))
        assertNull(NetplaySaves.localName("nxsave.brm", "My Game (EU)"))
        assertNull(NetplaySaves.localName("../nxsave.srm", "My Game (EU)"))
    }

    @Test fun whitelist_matches_the_handheld() {
        assertTrue(NetplaySaves.isSafe("nxsave.srm"))
        for (bad in listOf("", ".", "..", "-x", "a/b", "a b", "x".repeat(64))) assertFalse(NetplaySaves.isSafe(bad), bad)
    }

    @Test fun who_plays_on_a_copy() {
        assertFalse(NetplaySaves.playsOnCopy(NetplayRole.HOST, "snes9x", "snes9x"))
        assertTrue(NetplaySaves.playsOnCopy(NetplayRole.HOST, "picodrive", "genesis_plus_gx"))
        assertTrue(NetplaySaves.playsOnCopy(NetplayRole.CLIENT, "snes9x", "snes9x"))
        // a host whose saves could not be served plays on an empty isolated copy too
        assertTrue(NetplaySession(NetplayRole.HOST, "10.0.0.2", "snes9x", "snes9x", emptySaves = true).onCopy)
        assertFalse(NetplaySession(NetplayRole.HOST, "10.0.0.2", "snes9x", "snes9x").onCopy)
    }

    @Test fun a_host_on_a_core_that_shares_the_folder_cores_saves_keeps_its_real_save() {
        assertTrue(NetplaySaves.sharesSaves("gpsp", "mgba"))
        assertTrue(NetplaySaves.sharesSaves("mgba", "gpsp"))
        assertTrue(NetplaySaves.sharesSaves("snes9x", "snes9x"))
        assertTrue(NetplaySaves.sharesSaves("snes9x", "mednafen_supafaust"))
        assertTrue(NetplaySaves.sharesSaves("swanstation", "pcsx_rearmed"))
        assertFalse(NetplaySaves.sharesSaves("picodrive", "genesis_plus_gx"))
        assertFalse(NetplaySaves.sharesSaves("snes9x", "pcsx_rearmed"))
        assertFalse(NetplaySaves.playsOnCopy(NetplayRole.HOST, "snes9x", "mednafen_supafaust"))
        assertFalse(NetplaySaves.playsOnCopy(NetplayRole.HOST, "mgba", "gpsp"))
        assertTrue(NetplaySaves.playsOnCopy(NetplayRole.CLIENT, "mgba", "gpsp"))   // a joiner always plays on the host's copy
    }

    @Test fun resume_victims_drop_only_the_auto_slot() {
        val p = NxPaths("SFC", "snes9x", "Game.sfc")
        val all = setOf(p.state(9), p.thumb(9), p.lastSlot, p.state(1))
        assertEquals(listOf(p.state(9), p.thumb(9), p.lastSlot),
            NetplaySaves.resumeVictims("SFC", "snes9x", "Game.sfc", markerText = "9", exists = { it in all }))
        assertEquals(listOf(p.state(9), p.thumb(9)),
            NetplaySaves.resumeVictims("SFC", "snes9x", "Game.sfc", markerText = "1", exists = { it in all }))
    }

    @Test fun copy_session_never_writes_real_saves() {
        for (role in NetplayRole.entries) for ((run, folder) in listOf("snes9x" to "snes9x", "picodrive" to "genesis_plus_gx")) {
            val expected = role == NetplayRole.HOST && run == folder
            assertEquals(expected, !NetplaySaves.playsOnCopy(role, run, folder), "$role $run/$folder")
            assertEquals(!expected, NetplaySession(role, "10.0.0.2", run, folder).onCopy, "$role $run/$folder onCopy")
        }
    }

    @Test fun resume_victims_cover_every_auto_name() {
        val files = setOf(".userdata/shared/GBA-mgba/Game.state.auto", ".userdata/shared/GBA-mgba/Game.gba.st9")
        val victims = NetplaySaves.resumeVictims("GBA", "mgba", "Game.gba", null, SaveNaming(stateFormat = StateFormat.MINUI)) { it in files }
        assertEquals(files, victims.toSet())
    }

    @Test fun resume_victims_find_the_state_under_the_extracted_name() {
        val files = setOf(".userdata/shared/GBA-mgba/Inner (USA).state.auto")
        val victims = NetplaySaves.resumeVictims("GBA", "mgba", "Game.zip", null, SaveNaming(useExtractedFileName = true), "Inner (USA).gba") { it in files }
        assertEquals(files, victims.toSet())
    }
}
