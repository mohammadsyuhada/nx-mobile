package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.ui.MainRow
import com.nxredux.mobile.ui.ToolKind
import com.nxredux.mobile.ui.ToolRow
import com.nxredux.mobile.ui.minimalTabRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinimalListTest {
    // A list of thousands composes a screen's names around the position, never the whole list.
    @Test fun the_window_is_a_screen_around_the_position_whatever_the_list() {
        assertEquals(4990..5011, minimalWindow(5000f, 10_000, above = 10, below = 10))
        assertEquals(4990..5011, minimalWindow(5000.6f, 10_000, above = 10, below = 10))   // between two names: both sides
        assertEquals(0..11, minimalWindow(0f, 10_000, above = 10, below = 10))             // clamped at the ends
        assertEquals(9989..9999, minimalWindow(9999f, 10_000, above = 10, below = 10))
        assertEquals(0..0, minimalWindow(-0.4f, 1, above = 10, below = 10))                // pulled past the first
        assertTrue(minimalWindow(0f, 0, above = 10, below = 10).isEmpty())
    }

    // A tab's names alone: no tag, count or second line on any row.
    @Test fun a_tabs_names_carry_nothing_but_the_name() {
        val rows = minimalTabRows(listOf(MainRow.Collection("RPGs", 12), MainRow.Tool(ToolRow(ToolKind.SETTINGS, "Settings", null))))
        assertEquals(listOf("RPGs", "Settings"), rows.map { it.label })
        assertTrue(rows.all { it.tag == null && it.secondary == null })
    }
}
