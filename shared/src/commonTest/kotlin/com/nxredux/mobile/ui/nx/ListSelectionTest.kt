package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class ListSelectionTest {
    @Test fun down_and_up_wrap_at_both_ends() {
        val s = ListSelection(index = 0, count = 3)
        assertEquals(1, s.down().index)
        assertEquals(2, s.down().down().index)
        assertEquals(0, s.down().down().down().index)
        assertEquals(2, s.up().index)
    }
    @Test fun without_wrap_up_and_down_stop_at_the_ends() {
        assertEquals(0, ListSelection(0, 3).up(wrap = false).index)
        assertEquals(2, ListSelection(2, 3).down(wrap = false).index)
        assertEquals(1, ListSelection(0, 3).down(wrap = false).index)
        assertEquals(1, ListSelection(2, 3).up(wrap = false).index)
        assertEquals(0, ListSelection(0, 0).up(wrap = false).index)
    }
    @Test fun empty_list_stays_at_zero() {
        val s = ListSelection(0, 0)
        assertEquals(0, s.down().index); assertEquals(0, s.up().index)
    }
    @Test fun clamping_after_the_list_shrinks_keeps_a_valid_index() {
        assertEquals(1, ListSelection(5, 6).clampedTo(2).index)
        assertEquals(0, ListSelection(5, 6).clampedTo(0).index)
        assertEquals(3, ListSelection(3, 6).clampedTo(6).index)
    }
}
