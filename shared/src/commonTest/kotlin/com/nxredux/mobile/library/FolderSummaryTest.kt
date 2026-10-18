package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FolderSummaryTest {
    private fun game(source: String, name: String) = IndexedGame(source, "", "$name.x", "r/$name", "f", name)

    @Test fun counts_only_that_folders_games_largest_first() {
        val index = LibraryIndex(
            LibraryIndex.VERSION, "H", emptyList(), emptyMap(),
            listOf(
                IndexedSystem("GBA", "Game Boy Advance", games = listOf(game("x", "A"), game("home", "B"))),
                IndexedSystem("SFC", "Super Nintendo ES", games = listOf(game("x", "C"), game("x", "D"))),
                IndexedSystem("GB", "Game Boy", games = listOf(game("home", "E"))),
                IndexedSystem("", "Unassigned", unassigned = true, games = listOf(game("x", "F"))),
            ),
        )
        val s = folderSummary(index, "x")
        assertEquals(4, s.games)
        assertEquals(listOf("Super Nintendo ES" to 2, "Game Boy Advance" to 1), s.byConsole)
        assertEquals(1, s.unassigned)
        assertEquals("Super Nintendo ES (2) · Game Boy Advance (1)", s.line())
    }

    @Test fun the_unassigned_hint() {
        assertNull(unassignedHint(0))
        assertEquals("1 game needs a console. Find them in Settings → Library → Unassigned games.", unassignedHint(1))
        assertEquals("6 games need a console. Find them in Settings → Library → Unassigned games.", unassignedHint(6))
    }

    // The home: its games in Roms (the index's) and the placed ones outside, per console; unplaced files not counted.
    @Test fun the_home_counts_its_roms_games_and_the_placed_ones_outside() {
        val index = LibraryIndex(LibraryIndex.VERSION, "H", emptyList(), emptyMap(), listOf(
            IndexedSystem("GBA", "Game Boy Advance", games = listOf(IndexedGame("home", "Roms/Game Boy Advance (GBA)", "A.gba", "r", "f", "A"))),
            IndexedSystem("", "Unassigned", unassigned = true, games = listOf(IndexedGame("e1", "", "x.chd", "r", "f", "x"))),
        ))
        val e = Entry(ContentRef("H/x/K.gba"), "K.gba", false, 4L)
        val outside = listOf(
            LooseFile(e, ContentRef("H/x"), "x", "GBA"),
            LooseFile(e.copy(name = "T.gb"), ContentRef("H/x"), "x", "GB"),
            LooseFile(e.copy(name = "n.txt"), ContentRef("H/x"), "x", null),
        )
        assertEquals(FolderSummary(3, listOf("Game Boy Advance" to 2, "Game Boy" to 1), 0, outside = 2), homeSummary(index, outside))
    }
}
