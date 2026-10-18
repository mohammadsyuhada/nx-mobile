package com.nxredux.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncMapTest {
    @Test fun folders_map_by_display_name_then_tag() {
        assertEquals("gba", SyncMap.consoleOf("Game Boy Advance (MGBA)")!!.id)
        assertEquals("sms", SyncMap.consoleOf("Sega Master System (GPGX)")!!.id)   // display name beats GPGX->md
        assertEquals("dc", SyncMap.consoleOf("Dreamcast (DCX)")!!.id)               // unknown tag, by name
        assertEquals("fc", SyncMap.consoleOf("Famicom (FC)")!!.id)                   // other name, by tag
        assertEquals("segacd", SyncMap.consoleOf("Mega CD (GPGX)")!!.id)              // an alias of Sega CD beats GPGX->md
        assertNull(SyncMap.consoleOf("Amiga (AMIGA)"))
        assertNull(SyncMap.consoleOf("not a system folder"))
    }

    @Test fun renamed_folders_map_by_a_looser_name_and_a_shared_tag_never_guesses() {
        assertEquals("gb", SyncMap.consoleOf("Gameboy (GB)")!!.id)                          // squashed name (and its tag)
        assertEquals("gb", SyncMap.consoleOf("Gameboy (XYZ)")!!.id)                         // squashed name alone
        assertEquals("sms", SyncMap.consoleOf("Mastersystem (GPGX)")!!.id)                  // not GPGX -> Genesis
        assertEquals("segacd", SyncMap.consoleOf("Sega Mega-CD (GPGX)")!!.id)               // the name mentions Mega CD
        assertEquals("gg", SyncMap.consoleOf("My Game Gear (GPGX)")!!.id)
        assertEquals("md", SyncMap.consoleOf("Megadrive (GPGX)")!!.id)
        assertNull(SyncMap.consoleOf("Sega (GPGX)"))                                        // shared tag, no name: skipped
        assertEquals("gba", SyncMap.consoleOf("GBA Roms (MGBA)")!!.id)
        assertEquals("sfc", SyncMap.consoleOf("SNES (SUPA)")!!.id)
        assertEquals("dc", SyncMap.consoleOf("Dreamcast Lite (DCX)")!!.id)                  // unknown tag, the name mentions Dreamcast
        assertNull(SyncMap.consoleOf("Xtra Games (EXTRAS)"))
        assertNull(SyncMap.consoleOf("Ports (PORTS)"))
    }

    @Test fun save_folders_map_to_their_consoles() {
        val m = SyncMap.saveConsoles(listOf("Game Boy Advance (GBA)", "Game Boy Advance (MGBA)", "Sega Genesis (MD)",
            "Sega Genesis (GPGX)", "Sega Master System (GPGX)", "Dreamcast (DC)", "Dreamcast (DCX)", "Amiga (AMIGA)"))
        assertEquals(setOf("GBA"), m["MGBA"])
        assertEquals(setOf("GBA"), m["GBA"])
        assertEquals(setOf("PS"), m["PSX"])                 // allTags, no ROM folder needed
        assertEquals(setOf("MD", "SMS"), m["GPGX"])
        assertEquals(setOf("DC"), m["DC"])
        assertNull(m["DCX"])                                 // no phone core
        assertNull(m["AMIGA"])
    }
}
