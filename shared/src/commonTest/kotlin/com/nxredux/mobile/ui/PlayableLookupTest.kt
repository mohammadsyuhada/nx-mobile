package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.storage.GamePath
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayableLookupTest {
    private fun game(dir: String, file: String, source: String = "home") =
        GameEntry(ContentRef("$dir/$file"), file, file.substringBeforeLast('.'), folder = ContentRef(dir), sourceId = source, relativeDir = dir)

    private val gb = SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(game("Roms/Game Boy (GB)", "Tetris.gb"), game("Roms/Game Boy (GB)", "Kirby.gb")))
    private val coreless = SystemFolder("Nowhere", "ZZZ", null, listOf(game("Roms/Nowhere (ZZZ)", "Lost.zzz")))
    // The same file listed twice (two systems over one folder): the first system wins, as resolving does.
    private val twin = SystemFolder("Game Boy Twin", "GB", CoreRegistry.forTag("GB"), listOf(game("Roms/Game Boy (GB)", "Tetris.gb")))
    private val systems = listOf(coreless, gb, twin)

    private val lines = listOf(
        "/Roms/Game Boy (GB)/Tetris.gb", "/Roms/Game Boy (GB)/Kirby.gb", "/Roms/Game Boy (GB)/missing.gb",
        "/Roms/Nowhere (ZZZ)/Lost.zzz", "not a path", "", "/Roms/Game Boy (GB)/Tetris.gb",
    )

    @Test fun theLookupResolvesEachLineAsTheLinearScanDoes() {
        val lookup = playableLookup(systems)
        assertEquals(lines.map { resolveIn(systems, it) }, lines.map { l -> GamePath.split(l)?.let(lookup::get) })
        assertEquals(gb, lookup[GamePath.split("/Roms/Game Boy (GB)/Tetris.gb")]?.system)
    }

    // An extra folder's copy hidden behind a home game (LibraryScanner) still resolves, to the home game.
    @Test fun aHiddenCopysLineResolvesToItsHomeGame() {
        val hidden = "/Extra/e1/tetris.GB"
        val home = game("Roms/Game Boy (GB)", "Tetris.gb").copy(hiddenCopies = listOf(GamePath.split(hidden)!!))
        val systems = listOf(SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(home)))
        assertEquals(home, resolveIn(systems, hidden)?.game)
        assertEquals(home, playableLookup(systems)[GamePath.split(hidden)]?.game)
        assertEquals(2, resolvedCount(listOf(hidden, "/Roms/Game Boy (GB)/Tetris.gb"), playableLookup(systems)))   // a line each, as duplicates
    }

    // Recents and pins list a game once, though a hidden copy's line and its home game's line both resolve to it.
    @Test fun distinctResolvingListsAGameOnce() {
        val hidden = "/Extra/e1/tetris.GB"
        val home = game("Roms/Game Boy (GB)", "Tetris.gb").copy(hiddenCopies = listOf(GamePath.split(hidden)!!))
        val kirby = game("Roms/Game Boy (GB)", "Kirby.gb")
        val systems = listOf(SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(home, kirby)))
        val paths = listOf(hidden, "/Roms/Game Boy (GB)/Kirby.gb", "/Roms/Game Boy (GB)/missing.gb", "/Roms/Game Boy (GB)/Tetris.gb")
        assertEquals(listOf(home, kirby), resolveDistinct(systems, paths).map { it.game })   // first line's place wins
    }

    @Test fun theCountIsTheLinesThatResolve() {
        assertEquals(lines.count { resolveIn(systems, it) != null }, resolvedCount(lines, playableLookup(systems)))
        assertEquals(3, resolvedCount(lines, playableLookup(systems)))   // Tetris twice and Kirby, as the opened list shows them
    }
}
