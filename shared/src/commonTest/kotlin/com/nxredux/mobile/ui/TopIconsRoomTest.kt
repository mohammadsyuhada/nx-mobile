package com.nxredux.mobile.ui

import androidx.compose.ui.unit.dp
import com.nxredux.mobile.ui.nx.MENU_TAB_ROW_HEIGHT
import kotlin.test.Test
import kotlin.test.assertEquals

class TopIconsRoomTest {
    // The tab row shown: nothing to reserve. Hidden with nothing at the top: the grid takes its room (titleRoom).
    // Hidden but Recent / Search kept as icons up there: the grid keeps the row's room clear, so no row sits under them.
    @Test fun a_grid_keeps_the_top_clear_where_the_icons_sit() {
        assertEquals(TopIconsRoom(pad = 0.dp, titleRoom = 0.dp), topIconsRoom(tabRow = true, topIcons = false))
        assertEquals(TopIconsRoom(pad = 0.dp, titleRoom = MENU_TAB_ROW_HEIGHT), topIconsRoom(tabRow = false, topIcons = false))
        assertEquals(TopIconsRoom(pad = MENU_TAB_ROW_HEIGHT, titleRoom = 0.dp), topIconsRoom(tabRow = false, topIcons = true))
    }
}
