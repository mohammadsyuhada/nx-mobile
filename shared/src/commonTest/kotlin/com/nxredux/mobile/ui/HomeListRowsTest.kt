package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.ResumeInfo
import com.nxredux.mobile.ui.nx.HomeShelf
import com.nxredux.mobile.ui.nx.homeListRows
import com.nxredux.mobile.ui.nx.homeListTopRoom
import com.nxredux.mobile.ui.nx.MENU_TAB_ROW_HEIGHT
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeListRowsTest {
    private val gb = SystemFolder("Game Boy", "GB", null, emptyList(), null, null, "Game Boy (GB)", false)
    private fun g(name: String) =
        Listed(gb, GameEntry(ContentRef("r/$name"), "$name.gb", name, folder = ContentRef("f"), sourceId = "home", relativeDir = "Roms/Game Boy (GB)"))
    private val cont = MainRow.Continue(g("Tetris"), ResumeInfo.NONE)
    private val a = g("Advance Wars")
    private val c = g("Bomberman")
    private val t1 = ToolRow(ToolKind.SETTINGS, "Settings", null)

    // Continue and a shelf game wear their tags (Continue, the shelf's name) before the name.
    @Test fun rows_carry_the_continue_tag_and_shelf_names() {
        val rows = homeListRows(listOf(cont, MainRow.Pin(a), MainRow.Shelf(c, HomeShelf.NEW), MainRow.PinnedTool(t1)))
        assertEquals("Continue", rows[0].tag); assertEquals(cont.entry.game.shownName, rows[0].label)
        assertNull(rows[1].tag)
        assertEquals(a.game.shownName, rows[1].label)
        assertEquals(c.game.shownName, rows[2].label)
        assertEquals("New", rows[2].tag); assertNull(rows[2].secondary)
        assertEquals(t1.label, rows[3].label)
        assertNull(rows[3].secondary)
        assertEquals("Pick a game", homeListRows(listOf(MainRow.PickAGame)).single().label)
    }

    // The tab row hidden with no stats header: the rows run up beside Recent and Search, as a main menu List's do, so
    // the list sits as far under the page's top as over its band; the Pick a game card still keeps the row clear.
    @Test fun list_home_keeps_the_tab_rows_room_only_for_a_header_or_the_pick_a_game_card() {
        assertEquals(0.dp, homeListTopRoom(tabRow = false, header = null, pickAGame = false))
        assertEquals(MENU_TAB_ROW_HEIGHT, homeListTopRoom(tabRow = false, header = null, pickAGame = true))
        assertEquals(40.dp, homeListTopRoom(tabRow = false, header = 40.dp, pickAGame = false))   // the stats header's own height
        assertEquals(0.dp, homeListTopRoom(tabRow = true, header = null, pickAGame = false))
    }
}
