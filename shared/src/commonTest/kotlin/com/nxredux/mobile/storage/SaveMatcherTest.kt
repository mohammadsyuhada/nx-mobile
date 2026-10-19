package com.nxredux.mobile.storage

import com.nxredux.mobile.library.IndexedGame
import com.nxredux.mobile.library.IndexedSystem
import com.nxredux.mobile.library.LibraryIndex
import com.nxredux.mobile.runner.CoreRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveMatcherTest {
    private val gb = CoreRegistry.consoleOf("GB")!!
    private val gba = CoreRegistry.consoleOf("GBA")!!

    private fun game(file: String, source: String = Recents.HOME) =
        IndexedGame(source, "Roms", file, "ref/$file", "", file)

    private fun index(vararg systems: Pair<String, List<IndexedGame>>) = LibraryIndex(
        2, "home", emptyList(), emptyMap(), systems.map { (tag, games) -> IndexedSystem(tag, tag, games = games) },
    )

    private val tetris = index("GB" to listOf(game("Tetris.gb")), "GBA" to listOf(game("Tetris.gba")))

    @Test fun one_console_matches() {
        val m = SaveMatcher(index("GBA" to listOf(game("Pokemon - Lazarus.gba"), game("Pokemon - Lazarus.gba", "x"))))
        assertEquals(SaveMatch.Matched(gba, "Pokemon - Lazarus.gba"), m.match("Pokemon - Lazarus", null))
    }

    @Test fun no_game() {
        assertEquals(SaveMatch.NoGame, SaveMatcher(tetris).match("Zelda", null))
        assertEquals(SaveMatch.NoGame, SaveMatcher(null).match("Tetris", null))
    }

    @Test fun two_consoles_without_clue_is_ambiguous() {
        assertEquals(SaveMatch.Ambiguous(listOf(gb, gba)), SaveMatcher(tetris).match("Tetris", null))
    }

    @Test fun clue_settles_it() {
        assertEquals(SaveMatch.Matched(gba, "Tetris.gba"), SaveMatcher(tetris).match("Tetris", listOf(gba)))
    }

    @Test fun prefers_plain_rom_then_home() {
        val m = SaveMatcher(index("GBA" to listOf(
            game("X.zip"), game("X.gba", "x"), game("Z.zip"), game("Z.7z", "x"),
        )))
        assertEquals(SaveMatch.Matched(gba, "X.gba"), m.match("X", null))
        assertEquals(SaveMatch.Matched(gba, "Z.zip"), m.match("Z", null))
    }

    @Test fun games_by_console_delegates() {
        assertEquals(SharedSaves.gamesByConsole(tetris), SaveMatcher(tetris).gamesByConsole())
    }
}
