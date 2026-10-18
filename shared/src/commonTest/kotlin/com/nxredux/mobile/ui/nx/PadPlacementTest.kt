package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Rect
import com.nxredux.mobile.settings.GroupTweak
import kotlin.test.Test
import kotlin.test.assertEquals

class PadPlacementTest {
    private val bounds = Rect(0f, 0f, 1000f, 400f)
    private val dpad = Rect(100f, 200f, 250f, 350f)       // 150 x 150, centre (175, 275)
    private val unit = 50f

    private fun assertRect(expected: Rect, actual: Rect) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right, expected.bottom to actual.bottom)
            .forEach { (e, a) -> assertEquals(e, a, 0.01f, "expected $expected, was $actual") }
    }

    @Test fun identity_is_the_default_rect() = assertRect(dpad, placeGroup(dpad, GroupTweak(), unit, bounds))

    @Test fun offset_is_in_units() = assertRect(Rect(200f, 100f, 350f, 250f), placeGroup(dpad, GroupTweak(dx = 2f, dy = -2f), unit, bounds))

    @Test fun scale_is_about_the_centre() =
        assertRect(Rect(175f - 90f, 275f - 90f, 175f + 90f, 275f + 90f), placeGroup(dpad, GroupTweak(scale = 1.2f), unit, bounds))

    @Test fun no_unit_or_an_unmeasured_group_gives_the_default_and_keeps_the_tweak() {
        val t = GroupTweak(dx = 2f, dy = -1f, scale = 1.2f)
        val flat = Rect(100f, 200f, 100f, 350f)            // zero width
        assertEquals(dpad, placeGroup(dpad, t, 0f, bounds))
        assertEquals(flat, placeGroup(flat, t, unit, bounds))
        assertEquals(t, clampTweak(dpad, t, 0f, bounds))
        assertEquals(t, clampTweak(flat, t, unit, bounds))
    }

    @Test fun a_group_pushed_past_any_edge_stays_inside() {
        assertRect(Rect(0f, 200f, 150f, 350f), placeGroup(dpad, GroupTweak(dx = -100f), unit, bounds))
        assertRect(Rect(850f, 200f, 1000f, 350f), placeGroup(dpad, GroupTweak(dx = 100f), unit, bounds))
        assertRect(Rect(100f, 0f, 250f, 150f), placeGroup(dpad, GroupTweak(dy = -100f), unit, bounds))
        assertRect(Rect(100f, 250f, 250f, 400f), placeGroup(dpad, GroupTweak(dy = 100f), unit, bounds))
    }

    @Test fun a_group_bigger_than_its_bounds_shrinks_to_fit() {
        val small = Rect(0f, 0f, 200f, 120f)                              // the Fold's cover: less room than the layout's
        val placed = placeGroup(dpad, GroupTweak(scale = 1.5f), unit, small)   // 225 x 225 wanted
        assertRect(Rect(80f, 0f, 200f, 120f), placed)                      // 120 x 120, as near its centre as fits
    }

    @Test fun scale_out_of_range_is_clamped() =
        assertRect(placeGroup(dpad, GroupTweak(scale = 1.5f), unit, bounds), placeGroup(dpad, GroupTweak(scale = 4f), unit, bounds))

    @Test fun clamp_tweak_stores_where_the_group_actually_is() {
        assertEquals(GroupTweak(dx = -2f, dy = 0f), clampTweak(dpad, GroupTweak(dx = -100f), unit, bounds))
        assertEquals(GroupTweak(dx = 1f, dy = 0.5f, scale = 1.2f), clampTweak(dpad, GroupTweak(dx = 1f, dy = 0.5f, scale = 1.2f), unit, bounds))
        assertEquals(0.7f, clampTweak(dpad, GroupTweak(dy = 3f, scale = 1.2f), unit, bounds).dy, 1e-4f)   // 180 tall: centre stops at 310
    }

    @Test fun snap_zeroes_a_small_offset_and_keeps_scale() {
        assertEquals(GroupTweak(scale = 1.1f), snapToDefault(GroupTweak(dx = 0.2f, dy = -0.1f, scale = 1.1f)))
        assertEquals(GroupTweak(dx = 0.3f), snapToDefault(GroupTweak(dx = 0.3f)))
    }
}
