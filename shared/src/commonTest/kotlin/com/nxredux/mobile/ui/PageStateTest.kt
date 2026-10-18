package com.nxredux.mobile.ui

import androidx.compose.runtime.mutableStateOf
import com.nxredux.mobile.ui.nx.ListSelection
import kotlin.test.Test
import kotlin.test.assertEquals

class PageStateTest {
    // A page's selection: the live one while the page shows, then the last it showed (the page sliding out keeps its own).
    @Test fun followsTheSourceWhileLiveThenKeepsTheLastItShowed() {
        val live = mutableStateOf(true)
        val source = mutableStateOf(ListSelection(0, 5))
        val page = liveOrLast(live, source)
        assertEquals(0, page.value.index)
        source.value = ListSelection(3, 5)
        assertEquals(3, page.value.index)                   // live: follows the selection
        live.value = false
        source.value = ListSelection(0, 12)                 // the new list's selection
        assertEquals(ListSelection(3, 5), page.value)       // the page sliding out keeps its own
        source.value = ListSelection(7, 12)
        assertEquals(ListSelection(3, 5), page.value)
    }

    @Test fun aPageNeverReadWhileLiveKeepsItsFirstValue() {
        val live = mutableStateOf(true)
        val source = mutableStateOf(ListSelection(2, 4))
        val page = liveOrLast(live, source)
        live.value = false
        source.value = ListSelection(0, 9)
        assertEquals(ListSelection(2, 4), page.value)
    }
}
