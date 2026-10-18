package com.nxredux.mobile.scraper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The looser tier after an exact miss; the names are the phone checks' real misses. */
class TitleMatchTest {
    private fun same(a: String, b: String) = assertEquals(TitleMatch.loose(a), TitleMatch.loose(b), "$a / $b")
    private fun differ(a: String, b: String) = kotlin.test.assertNotEquals(TitleMatch.loose(a), TitleMatch.loose(b), "$a / $b")

    @Test fun looseIgnoresPunctuationAndAnd() {
        same("ChuChu Rocket", "ChuChu Rocket!")
        same("Marvel vs Capcom 2", "Marvel vs. Capcom 2")
        same("Tom and Jerry", "Tom & Jerry")
        same("Tom & Jerry", "Tom _ Jerry (USA)")
        same("Diddys Kong Quest", "Diddy's Kong Quest")
        same("Legend of Zelda", "The Legend of Zelda")
        same("Legend of Zelda, The - A Link to the Past (USA)", "The Legend of Zelda: A Link to the Past")
    }

    @Test fun looseDropsVersionSuffixes() {
        same("Crazy Taxi 2 v1.004", "Crazy Taxi 2")
        same("Crazy Taxi 2 (Rev 1)", "Crazy Taxi 2")
        same("Crazy Taxi 2 Rev A", "Crazy Taxi 2")
        for (p in listOf("Emerald", "Ruby", "Sapphire", "FireRed", "LeafGreen")) {
            same("Pokémon $p", "Pokemon - $p Version (USA, Europe)")
            same("Pokemon $p", "Pokémon $p Version")
        }
        // Only as a trailing word: "Version" inside a title stays.
        differ("Version Control", "Control")
    }

    @Test fun looseReadsRomanNumeralsAsDigits() {
        same("Sonic The Hedgehog II", "Sonic the Hedgehog 2")
        same("Street Fighter II", "Street Fighter 2")
        same("Final Fantasy I", "Final Fantasy 1")
        // A trailing I / 1 after two words is the first game.
        same("Sonic The Hedgehog I", "Sonic the Hedgehog")
        // Only trailing: "I, Robot" keeps its I; V and X stay words.
        differ("I, Robot", "1 Robot")
        differ("Mega Man X", "Mega Man 10")
        differ("Doom 1", "Doom")
    }

    @Test fun looseKeepsDifferentGamesApart() {
        differ("Super Mario Bros", "Super Mario Bros. 3")
        differ("Tetris", "Tetris 2")
        differ("Street Fighter II", "Street Fighter II Turbo")
        differ("Sonic the Hedgehog 2", "Sonic the Hedgehog 3")
        differ("Mortal Kombat II", "Mortal Kombat III")
    }

    @Test fun headIsTheTitleBeforeASubtitleSeparator() {
        assertEquals(TitleMatch.loose("Marvel vs Capcom 2"), TitleMatch.head("Marvel vs. Capcom 2: New Age of Heroes"))
        assertEquals(TitleMatch.loose("Marvel vs Capcom 2"), TitleMatch.head("Marvel vs. Capcom 2 - New Age of Heroes (USA)"))
        assertEquals(TitleMatch.loose("Marvel vs Capcom 2"), TitleMatch.head("Marvel vs. Capcom 2_ New Age of Heroes (USA)"))
        assertEquals(TitleMatch.loose("Legend of Zelda"), TitleMatch.head("Legend of Zelda, The - A Link to the Past (USA)"))
        // No separator, or a one-word head: none.
        assertNull(TitleMatch.head("Super Mario Bros. 3"))
        assertNull(TitleMatch.head("Street Fighter II Turbo"))
        assertNull(TitleMatch.head("Pokemon - Emerald Version"))
        assertNull(TitleMatch.head("Tetris: The Grand Master"))
        assertNull(TitleMatch.head("Pac-Man"))
    }

    @Test fun searchTermDropsOnlyVersionNumbers() {
        assertEquals("Crazy Taxi 2", TitleMatch.searchTerm("Crazy Taxi 2 v1.004"))
        assertEquals("Crazy Taxi 2", TitleMatch.searchTerm("Crazy Taxi 2 Rev A (USA)"))
        assertEquals("Pokemon Emerald Version", TitleMatch.searchTerm("Pokemon Emerald Version"))
        assertEquals("ChuChu Rocket!", TitleMatch.searchTerm("ChuChu Rocket!"))
        assertEquals("Version", TitleMatch.searchTerm("Version"))
    }

    private data class T(val id: Int, val title: String)
    private fun pick(name: String, vararg titles: String) = TitleMatch.pick(titles.mapIndexed { i, t -> T(i, t) }, name) { it.title }.map { it.title }

    @Test fun pickTiersExactThenLooseThenUniqueHead() {
        // Exact (normaliseTitle) wins over a looser one.
        assertEquals(listOf("ChuChu Rocket"), pick("ChuChu Rocket", "ChuChu Rocket!", "ChuChu Rocket"))
        assertEquals(listOf("ChuChu Rocket!"), pick("ChuChu Rocket", "ChuChu Rocket!", "ChuChu Rocket! 2"))
        // Loose before head.
        assertEquals(listOf("Marvel vs. Capcom 2"), pick("Marvel vs Capcom 2", "Marvel vs. Capcom 2: New Age of Heroes", "Marvel vs. Capcom 2"))
        assertEquals(listOf("Marvel vs. Capcom 2: New Age of Heroes"), pick("Marvel vs Capcom 2", "Marvel vs. Capcom 2: New Age of Heroes", "Marvel vs. Capcom"))
        // Every hit of a tier, in the given order.
        assertEquals(listOf("Pokémon Emerald Version", "Pokemon - Emerald Version"), pick("Pokemon Emerald", "Pokémon Emerald Version", "Pokemon Ruby Version", "Pokemon - Emerald Version"))
    }

    @Test fun pickNeverGuessesBetweenSubtitles() {
        assertEquals(emptyList(), pick("Street Fighter II", "Street Fighter II: The World Warrior", "Street Fighter II: Champion Edition"))
        // The same title twice (two entries, or two regions) is one title.
        assertEquals(2, pick("Marvel vs Capcom 2", "Marvel vs. Capcom 2: New Age of Heroes", "Marvel vs Capcom 2 - New Age of Heroes").size)
    }

    @Test fun pickKeepsAnApiTitlesGroups() {
        assertEquals(emptyList(), pick("Tetris", "Tetris Ultimate", "Tetris (IBM DOS)"))
        assertEquals(emptyList(), pick("Pokemon - Emerald", "Pokemon Emerald Version [Case Bundle]"))
        // The user's own groups are tags, dropped.
        assertEquals(listOf("ChuChu Rocket!"), pick("ChuChu Rocket (USA)", "ChuChu Rocket!"))
    }

    @Test fun pickTakesAHeadOnlyFromTheFirstResult() {
        assertEquals(emptyList(), pick("Pokemon - FireRed", "Pokemon - FireRed Extended", "Pokémon FireRed: VR Missions"))
        assertEquals(listOf("Pokémon FireRed: VR Missions"), pick("Pokemon - FireRed", "Pokémon FireRed: VR Missions", "Pokemon - FireRed Extended"))
    }

    @Test fun pickFalsePositives() {
        assertEquals(emptyList(), pick("Super Mario Bros", "Super Mario Bros. 3", "Super Mario Bros. 2"))
        assertEquals(emptyList(), pick("Tetris", "Tetris 2", "Tetris: The Grand Master"))
        assertEquals(emptyList(), pick("Street Fighter II", "Street Fighter II Turbo"))
        // A one-word title never takes a subtitled one.
        assertEquals(emptyList(), pick("Castlevania", "Castlevania: Symphony of the Night"))
        // A shorter listed title never takes a longer user title (no known-subtitle list).
        assertEquals(emptyList(), pick("Street Fighter II The World Warrior", "Street Fighter II"))
        // No safe rule: the user's words leave out "The Legend of" and "A".
        assertEquals(emptyList(), pick("Zelda Link To The Past", "Legend of Zelda, The - A Link to the Past"))
    }
}
