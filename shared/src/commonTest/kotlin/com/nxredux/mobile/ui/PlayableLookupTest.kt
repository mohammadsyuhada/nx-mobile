package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.library.matches
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
    // A collection, recents or pins list resolved through the lookup is the list the linear resolve gives: same games, same
    // systems, same order, duplicates kept (collections) or dropped (distinct), and a hidden copy's line falling back to its
    // home game only when no game is listed under that path itself.
    @Test fun resolvingThroughTheLookupGivesTheSameListsAsTheLinearResolve() {
        val copy = "/Extra/e1/Tetris.gb"
        val listedCopy = "/Extra/e1/Kirby.gb"   // a hidden copy's path that a listed game also has: the listed game wins
        val home = game("Roms/Game Boy (GB)", "Tetris.gb").copy(hiddenCopies = listOf(GamePath.split(copy)!!))
        val kirby = game("Roms/Game Boy (GB)", "Kirby.gb").copy(hiddenCopies = listOf(GamePath.split(listedCopy)!!))
        val extraKirby = game("", "Kirby.gb", source = "e1")
        val gbHome = SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(home, kirby))
        val extra = SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(extraKirby, game("Roms/Game Boy (GB)", "Tetris.gb")))
        val dead = SystemFolder("Nowhere", "ZZZ", null, listOf(game("Extra/e1", "Tetris.gb", source = "e1")))
        val library = listOf(dead, gbHome, extra, twin, coreless)
        val paths = lines + listOf(copy, listedCopy, "/Extra/e1/Kirby.gb", copy, "/Roms/Game Boy (GB)/Kirby.gb", "/Roms/Nowhere (ZZZ)/Lost.zzz")
        val lookup = playableLookup(library)
        assertEquals(paths.map { resolveIn(library, it) }, paths.map { resolveIn(lookup, it) })
        assertEquals(paths.mapNotNull { resolveIn(library, it) }, paths.mapNotNull { resolveIn(lookup, it) })   // a collection's list
        assertEquals(resolveDistinct(library, paths), resolveDistinct(lookup, paths))                           // recents and pins
        assertEquals(home, resolveIn(lookup, copy)?.game)
        assertEquals(extraKirby, resolveIn(lookup, listedCopy)?.game)
    }

    // Hidden's lines resolve against every system, coreless ones included, the first system holding the game winning; no
    // hidden-copy fallback (a hidden line is a listed game's own path).
    @Test fun theWholeLibraryLookupMatchesTheLinearHiddenResolve() {
        val copy = "/Extra/e1/tetris.GB"
        val home = game("Roms/Game Boy (GB)", "Tetris.gb").copy(hiddenCopies = listOf(GamePath.split(copy)!!))
        val library = listOf(coreless, SystemFolder("Game Boy", "GB", CoreRegistry.forTag("GB"), listOf(home)), twin, gb)
        fun linear(p: String) = GamePath.split(p)?.let { ref -> library.firstNotNullOfOrNull { s -> s.games.firstOrNull { it.matches(ref) }?.let { Listed(s, it) } } }
        val paths = lines + copy
        val lookup = gameLookup(library)
        assertEquals(paths.map(::linear), paths.map { resolveIn(lookup, it) })
        assertEquals(coreless, resolveIn(lookup, "/Roms/Nowhere (ZZZ)/Lost.zzz")?.system)
        assertEquals(null, resolveIn(lookup, copy))
    }
}
