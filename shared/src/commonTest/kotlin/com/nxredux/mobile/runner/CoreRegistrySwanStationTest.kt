package com.nxredux.mobile.runner

import com.nxredux.mobile.options.CoreConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreRegistrySwanStationTest {
    @Test fun psx_runs_swanstation_and_ps_stays_pcsx_rearmed() {
        val sw = CoreRegistry.forTag("PSX")!!
        assertEquals("swanstation", sw.id)
        assertEquals("libswanstation_libretro_android.so", sw.libraryFileName)
        assertEquals("SwanStation", sw.name)
        assertEquals(sw, CoreRegistry.forId("swanstation"))
        assertEquals("pcsx_rearmed", CoreRegistry.forTag("PS")!!.id)
    }

    @Test fun the_playstation_console_groups_ps_and_psx() {
        val ps = CoreRegistry.consoleOf("PSX")!!
        assertEquals("ps", ps.id)
        assertEquals(listOf("PS", "PSX"), ps.tags)
        assertEquals(ps, CoreRegistry.consoleOf("PS"))
    }

    @Test fun psx_reads_its_bios_from_bios_psx() {
        assertEquals(listOf("PS", "PSX"), BiosFolders.forTag("PSX"))
        assertEquals(listOf("scph5500.bin", "scph5501.bin", "scph5502.bin"), CoreRegistry.forTag("PSX")!!.biosFiles)
    }

    // SwanStation's DualShock is RETRO_DEVICE_SUBCLASS(ANALOG, 0) = 261; its 517 is the Analog Joystick (flight stick),
    // which starts in analog mode and which most games ignore (no buttons reached Marvel Super Heroes).
    @Test fun swanstation_plugs_its_dualshock_on_both_ports_for_two_players() {
        val sw = CoreRegistry.forTag("PSX")!!
        assertEquals(261, CoreDescriptor.SWANSTATION_DUALSHOCK)
        assertEquals(2, CoreRegistry.playersFor("PSX", sw))
        assertEquals(mapOf(0 to 261, 1 to 261), sw.controllerPortsFor(2))
        assertEquals(mapOf(0 to 261), sw.controllerPortsFor(1))
    }

    @Test fun swanstation_runs_the_playstation_disc_formats() {
        val ext = CoreRegistry.forTag("PSX")!!.extensions
        for (e in listOf("cue", "bin", "img", "iso", "chd", "m3u", "pbp", "exe")) assertTrue(e in ext, e)
    }

    @Test fun psx_pads_read_like_ps() {
        // PadMap has no equals: compare what it draws and where the menus act.
        val ps = PadMap.forTag("PS")
        val psx = PadMap.forTag("PSX")
        for (b in com.nxredux.mobile.ui.nx.NxButton.entries) {
            assertEquals(ps.label(b), psx.label(b), "$b")
            assertEquals(ps.menuButton(b), psx.menuButton(b), "$b")
        }
        assertEquals("×", psx.label(com.nxredux.mobile.ui.nx.NxButton.B))
    }

    @Test fun memory_card_1_is_locked_to_libretro_so_saves_stay_per_game_srm() {
        val d = CoreRegistry.forTag("PSX")!!
        assertEquals("Libretro", d.defaultOptions["swanstation_MemoryCards_Card1Type"])
        assertTrue("swanstation_MemoryCards_Card1Type" in d.lockedOptions)
        val game = CoreConfig.parse("swanstation_MemoryCards_Card1Type = PerGame\n")
        val r = CoreConfig.resolve(d.defaultOptions, null, game, forced = d.lockedOptions)
        assertEquals("Libretro", r.values["swanstation_MemoryCards_Card1Type"])
    }
}
