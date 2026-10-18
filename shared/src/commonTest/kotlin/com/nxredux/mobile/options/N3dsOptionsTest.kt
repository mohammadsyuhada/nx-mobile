package com.nxredux.mobile.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class N3dsOptionsTest {
    @Test fun applies_to_the_3ds_tag_only() {
        assertTrue(N3dsOptions.appliesTo("3DS"))
        assertTrue(N3dsOptions.appliesTo(" 3ds "))
        assertFalse(N3dsOptions.appliesTo("NDS"))
    }

    @Test fun select_right_walks_default_large_side_by_side_single_and_wraps() {
        assertEquals("citra_layout_option" to "large_screen", N3dsOptions.cycleLayout("default", 1))
        assertEquals(N3dsOptions.LAYOUT to "side_by_side", N3dsOptions.cycleLayout("large_screen", 1))
        assertEquals(N3dsOptions.LAYOUT to "single_screen", N3dsOptions.cycleLayout("side_by_side", 1))
        assertEquals(N3dsOptions.LAYOUT to "default", N3dsOptions.cycleLayout("single_screen", 1))
        assertEquals(N3dsOptions.LAYOUT to "single_screen", N3dsOptions.cycleLayout("default", -1))
    }

    @Test fun an_unknown_layout_counts_as_the_default() {
        assertEquals(N3dsOptions.LAYOUT to "large_screen", N3dsOptions.cycleLayout(null, 1))
        assertEquals(N3dsOptions.LAYOUT to "large_screen", N3dsOptions.cycleLayout("bogus", 1))
    }

    @Test fun layout_labels() {
        assertEquals("Top over bottom", N3dsOptions.layoutLabel("default"))
        assertEquals("Large screen", N3dsOptions.layoutLabel("large_screen"))
        assertEquals("Side by side", N3dsOptions.layoutLabel("side_by_side"))
        assertEquals("Single screen", N3dsOptions.layoutLabel("single_screen"))
        assertEquals("odd", N3dsOptions.layoutLabel("odd"))
    }

    @Test fun deadzone_reads_the_core_percentage() {
        assertEquals(0.15f, N3dsOptions.deadzone(null))
        assertEquals(0.15f, N3dsOptions.deadzone("x"))
        assertEquals(0.35f, N3dsOptions.deadzone("35"))
        assertEquals(0f, N3dsOptions.deadzone("0"))
    }
}
