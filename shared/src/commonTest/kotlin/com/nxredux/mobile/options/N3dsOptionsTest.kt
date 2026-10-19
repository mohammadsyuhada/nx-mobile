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
        fun next(v: String?, d: Int = 1) = N3dsOptions.cycleLayout(v?.let { mapOf(N3dsOptions.LAYOUT_PORTRAIT to it) } ?: emptyMap(), false, false, d)
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "large_screen", next("default"))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "side_by_side", next("large_screen"))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "single_screen", next("side_by_side"))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "default", next("single_screen"))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "single_screen", next("default", -1))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "large_screen", next(null))      // unknown or missing: the default
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT to "large_screen", next("bogus"))
    }

    @Test fun one_layout_per_orientation_and_the_row_in_use_is_cycled() {
        val v = mapOf(N3dsOptions.LAYOUT_PORTRAIT to "large_screen", N3dsOptions.LAYOUT_PORTRAIT_CONTROLLER to "single_screen",
            N3dsOptions.LAYOUT_LANDSCAPE to "side_by_side")
        assertEquals("large_screen", N3dsOptions.layoutFor(v, landscape = false, controllerMode = false))
        assertEquals("single_screen", N3dsOptions.layoutFor(v, landscape = false, controllerMode = true))
        assertEquals("side_by_side", N3dsOptions.layoutFor(v, landscape = true, controllerMode = false))
        assertEquals("side_by_side", N3dsOptions.layoutFor(v, landscape = true, controllerMode = true))   // landscape has one row
        assertEquals("default", N3dsOptions.layoutFor(emptyMap(), landscape = true, controllerMode = false))
        assertEquals(N3dsOptions.LAYOUT_LANDSCAPE to "single_screen", N3dsOptions.cycleLayout(v, true, false, 1))
        assertEquals(N3dsOptions.LAYOUT_PORTRAIT_CONTROLLER to "default", N3dsOptions.cycleLayout(v, false, true, 1))
        assertEquals(listOf("Layout (portrait)", "Layout (portrait, controller)", "Layout (landscape)"), N3dsOptions.layoutRows.map { it.label })
        assertTrue(N3dsOptions.layoutRows.all { it.default == "default" && it.values == N3dsOptions.LAYOUT_CYCLE })
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
