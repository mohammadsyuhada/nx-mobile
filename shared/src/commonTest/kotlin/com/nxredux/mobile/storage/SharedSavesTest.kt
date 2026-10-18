package com.nxredux.mobile.storage

import com.nxredux.mobile.library.IndexedGame
import com.nxredux.mobile.library.IndexedSystem
import com.nxredux.mobile.library.LibraryIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedSavesTest {
    private fun game(file: String) = IndexedGame("home", "Roms/x", file, "r/$file", "r", file)

    @Test fun library_games_group_by_console_whatever_the_emulator() {
        val index = LibraryIndex(
            LibraryIndex.VERSION, "Home", emptyList(), emptyMap(),
            listOf(
                IndexedSystem("SMS", "Sega Master System", games = listOf(game("Alex Kidd.sms"))),
                IndexedSystem("GPGX_SMS", "Sega Master System", games = listOf(game("Wonder Boy.sms"))),
                IndexedSystem("", "Unassigned", unassigned = true, games = listOf(game("x.sms"))),
            ),
        )
        assertEquals(mapOf("SMS" to setOf("Alex Kidd.sms", "Wonder Boy.sms")), SharedSaves.gamesByConsole(index))
        assertEquals(emptyMap(), SharedSaves.gamesByConsole(null))
    }

    @Test fun a_gpgx_save_belongs_to_the_one_console_holding_its_game() {
        val games = mapOf("SMS" to setOf("Alex Kidd.sms", "Both.sms"), "MD" to setOf("Both.md"), "GBA" to setOf("Kirby.gba"))
        assertEquals("SMS", SharedSaves.consoleOf("GPGX", "Alex Kidd.srm", games))
        assertEquals("SMS", SharedSaves.consoleOf("gpgx", "Alex Kidd.sms.sav", games))
        assertNull(SharedSaves.consoleOf("GPGX", "Both.srm", games))    // two consoles
        assertNull(SharedSaves.consoleOf("GPGX", "Kirby.srm", games))   // GBA is no GPGX console
        assertNull(SharedSaves.consoleOf("MGBA", "Kirby.srm", games))   // not a shared folder
    }
}
