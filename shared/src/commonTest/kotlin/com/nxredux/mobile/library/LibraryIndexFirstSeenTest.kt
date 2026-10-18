package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LibraryIndexFirstSeenTest {
    private fun game(file: String, firstSeen: Long? = null) =
        IndexedGame(source = "home", dir = "Roms/Game Boy (GB)", file = file, ref = "r/$file", folder = "f", name = file, firstSeen = firstSeen)
    private fun index(vararg g: IndexedGame) = LibraryIndex(
        version = LibraryIndex.VERSION, home = "h", extras = emptyList(), defaultTags = emptyMap(),
        systems = listOf(IndexedSystem(displayName = "Game Boy", tag = "GB", games = g.toList())),
    )
    private fun LibraryIndex.seen(file: String) = systems.single().games.single { it.file == file }.firstSeen

    @Test fun keepsThePreviousDate() {
        val prev = index(game("a.gb", firstSeen = 100))
        val now = index(game("a.gb", firstSeen = 999)).withFirstSeen(prev, now = 5000)
        assertEquals(100, now.seen("a.gb"))
    }
    // The first scan (no previous index) dates each game by its file, so a whole library is not New at once.
    @Test fun theFirstScanKeepsTheFileDate() {
        assertEquals(700, index(game("b.gb", firstSeen = 700)).withFirstSeen(null, now = 5000).seen("b.gb"))
    }
    // A copy keeps its file's old date (adb push, MTP, a card restore): a game the previous index lacks is new now.
    @Test fun aGameThePreviousIndexLacksTakesTheScanTime() {
        val prev = index(game("a.gb", firstSeen = 100))
        assertEquals(5000, index(game("a.gb", firstSeen = 999), game("b.gb", firstSeen = 700)).withFirstSeen(prev, now = 5000).seen("b.gb"))
    }
    // An index from before the field lists the game with no date: it is known, so it keeps its file date.
    @Test fun aGameInAnUndatedPreviousIndexKeepsTheFileDate() {
        val prev = index(game("b.gb"))
        assertEquals(700, index(game("b.gb", firstSeen = 700)).withFirstSeen(prev, now = 5000).seen("b.gb"))
    }
    @Test fun noClockLeavesAnUnknownGameItsFileDate() {
        assertEquals(700, index(game("b.gb", firstSeen = 700)).withFirstSeen(index(), now = null).seen("b.gb"))
    }
    @Test fun noFileDateTakesTheScanTime() {
        assertEquals(5000, index(game("c.gb")).withFirstSeen(null, now = 5000).seen("c.gb"))
    }
    @Test fun noClockLeavesItNull() {
        assertNull(index(game("c.gb")).withFirstSeen(null, now = null).seen("c.gb"))
    }
    // An index from before firstSeen has no such key: the codec writes none for a null date, so this is that file.
    @Test fun oldIndexDecodesWithoutFirstSeen() {
        val json = LibraryIndexCodec.encode(index(game("d.gb")))
        assertFalse("firstSeen" in json)
        assertNull(LibraryIndexCodec.decode(json)!!.seen("d.gb"))
    }
    @Test fun aDateRoundTrips() {
        val json = LibraryIndexCodec.encode(index(game("d.gb", firstSeen = 1_759_536_000_000L)))
        assertEquals(1_759_536_000_000L, LibraryIndexCodec.decode(json)!!.seen("d.gb"))
    }
    @Test fun toSystemsCarriesIt() {
        assertEquals(42, index(game("e.gb", firstSeen = 42)).toSystems().single().games.single().firstSeen)
    }
}
