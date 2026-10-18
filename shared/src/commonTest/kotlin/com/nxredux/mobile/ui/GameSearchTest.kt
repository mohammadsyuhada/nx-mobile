package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSearchTest {
    private fun game(name: String, file: String = "$name.rom") =
        GameEntry(ContentRef("r/$file"), file, name, folder = ContentRef("f"), sourceId = "home", relativeDir = "Roms/x")
    private fun system(tag: String, vararg games: GameEntry) =
        SystemFolder("Console $tag", tag, null, games.toList(), null, null, "Console ($tag)", false)
    private fun names(r: List<Listed>) = r.map { it.game.shownName }

    // A game list's own search: only its entries, ranked as the library search ranks.
    @Test fun filterKeepsOnlyTheListsMatchesRanked() {
        val snes = system("SFC", game("Super Mario World"), game("F-Zero"), game("Dr. Mario"))
        val list = snes.games.map { Listed(snes, it) }
        assertEquals(listOf("Super Mario World", "Dr. Mario").sorted(), names(GameSearch.filter(list, "mario")).sorted())
        assertEquals(listOf("F-Zero"), names(GameSearch.filter(list, "zero")))
        assertTrue(GameSearch.filter(list, "  ").isEmpty())
        assertEquals(names(GameSearch.search(listOf(snes), "mario")), names(GameSearch.filter(list, "mario")))
    }

    @Test fun normalizeFoldsCaseAccentsPunctuationAndNumerals() {
        assertEquals("pokemon edicao azul", GameSearch.normalize("Pokémon: Edição  Azul"))
        assertEquals("street fighter 2 turbo", GameSearch.normalize("Street Fighter II' Turbo"))
        assertEquals("final fantasy 6", GameSearch.normalize("Final Fantasy VI"))
        assertEquals("", GameSearch.normalize("  -- , "))
    }

    @Test fun loneRomanLettersStayLetters() {
        assertEquals("mega man x", GameSearch.normalize("Mega Man X"))
        assertEquals("final fantasy v", GameSearch.normalize("Final Fantasy V"))
        val snes = system("SFC", game("Mega Man X"))
        assertEquals(listOf("Mega Man X"), names(GameSearch.search(listOf(snes), "x")))
    }

    @Test fun nonLatinLettersSurvive() {
        assertEquals("ポケモン 赤", GameSearch.normalize("ポケモン・赤"))
        val gb = system("GB", game("ポケモン 赤"))
        assertEquals(listOf("ポケモン 赤"), names(GameSearch.search(listOf(gb), "ポケモン")))
    }

    @Test fun accentsAreIgnored() {
        val gb = system("GB", game("Pokémon Red"))
        assertEquals(listOf("Pokémon Red"), names(GameSearch.search(listOf(gb), "pokemon")))
    }

    @Test fun decomposedAccentsAreIgnored() {
        val nfd = "Pokémon Red"   // NFD: e + combining acute
        assertEquals("pokemon", GameSearch.normalize("Pokémon"))
        assertEquals("pokemon red", GameSearch.normalize(nfd))
        val gb = system("GB", game("Super Pokemon Red"), game(nfd))
        assertEquals(listOf(nfd, "Super Pokemon Red"), names(GameSearch.search(listOf(gb), "pokemon red")))
    }

    @Test fun everyWordMustMatchInAnyOrder() {
        val snes = system("SFC", game("The Legend of Zelda - A Link to the Past"), game("Super Mario World"))
        assertEquals(listOf("The Legend of Zelda - A Link to the Past"), names(GameSearch.search(listOf(snes), "zelda link")))
        assertEquals(listOf("The Legend of Zelda - A Link to the Past"), names(GameSearch.search(listOf(snes), "link zelda")))
        assertEquals(emptyList(), names(GameSearch.search(listOf(snes), "mario zelda")))
    }

    @Test fun romanNumeralsMatchDigits() {
        val snes = system("SFC", game("Street Fighter II Turbo"))
        assertEquals(listOf("Street Fighter II Turbo"), names(GameSearch.search(listOf(snes), "street fighter 2")))
        assertEquals(listOf("Street Fighter II Turbo"), names(GameSearch.search(listOf(snes), "fighter ii")))
    }

    @Test fun gluedWordsMatchTheJoinedName() {
        val arcade = system("FBN", game("Ms. Pac-Man"), game("Mega Man X"))
        assertEquals(listOf("Ms. Pac-Man"), names(GameSearch.search(listOf(arcade), "pacman")))
        assertEquals(listOf("Mega Man X"), names(GameSearch.search(listOf(arcade), "megaman")))
    }

    @Test fun theTagMatchesEveryGameOfItsSystem() {
        val gba = system("GBA", game("Metroid Fusion"), game("Golden Sun"))
        val gb = system("GB", game("Tetris"))
        assertEquals(listOf("Golden Sun", "Metroid Fusion"), names(GameSearch.search(listOf(gba, gb), "gba")))
    }

    @Test fun theFileNameMatchesWithoutItsExtension() {
        val gb = system("GB", game("Pocket Monsters Aka", file = "Pokemon Red (USA).gb"), game("Zip Racer", file = "Zip Racer.gb"))
        assertEquals(listOf("Pocket Monsters Aka"), names(GameSearch.search(listOf(gb), "pokemon red")))
        assertEquals(emptyList(), names(GameSearch.search(listOf(system("GB", game("Tetris", file = "Tetris.zip"))), "zip")))
    }

    @Test fun rankingPutsNameStartsFirstThenWordStartsThenTheRest() {
        val snes = system(
            "SFC",
            game("Super Mario World"), game("Dr. Mario"), game("Mario Kart"),
            game("Kart Racer", file = "Mario Kart Clone.sfc"),   // filename only: last group
            game("Smario Bros"),                                  // inside a word: last group
        )
        assertEquals(
            listOf("Mario Kart", "Dr. Mario", "Super Mario World", "Kart Racer", "Smario Bros"),
            names(GameSearch.search(listOf(snes), "mario")),
        )
    }

    @Test fun equalNamesAreOrderedByTag() {
        val nes = system("NES", game("Tetris"))
        val gb = system("GB", game("Tetris"))
        assertEquals(listOf("GB", "NES"), GameSearch.search(listOf(nes, gb), "tetris").map { it.system.tag })
    }

    @Test fun aBlankQueryFindsNothing() {
        val gb = system("GB", game("Tetris"))
        assertTrue(GameSearch.search(listOf(gb), "").isEmpty())
        assertTrue(GameSearch.search(listOf(gb), "  ,. ").isEmpty())
    }

    @Test fun aRomanLookingQueryWordAlsoMatchesAsTyped() {
        val sat = system("SAT", game("Virtua Fighter"), game("Vigilante"))
        val snes = system("SFC", game("Final Fantasy VI"), game("Street Fighter II"), game("Ivy the Kiwi"))
        val all = listOf(sat, snes)
        val vi = names(GameSearch.search(all, "vi"))
        assertTrue("Virtua Fighter" in vi && "Vigilante" in vi && "Final Fantasy VI" in vi, vi.toString())
        assertEquals(listOf("Ivy the Kiwi"), names(GameSearch.search(all, "iv")))
        assertEquals(listOf("Street Fighter II"), names(GameSearch.search(all, "street fighter 2")))
    }

    @Test fun aGluedNameStartRanksFirst() {
        val arcade = system("ARC", game("Spacman Zone"), game("Ms. Pac-Man"), game("Pac-Man"))
        assertEquals(listOf("Pac-Man", "Ms. Pac-Man", "Spacman Zone"), names(GameSearch.search(listOf(arcade), "pacman")))
    }
}
