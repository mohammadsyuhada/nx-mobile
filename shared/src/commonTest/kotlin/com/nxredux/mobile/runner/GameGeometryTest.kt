package com.nxredux.mobile.runner

import com.nxredux.mobile.options.PxRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameGeometryTest {
    // Azahar at proportion 1: the stacked "default" frame (400 x 480) and the side-by-side one (400 + 320 x 240).
    private val stacked = GameGeometry(400f, 480f, 400f / 480f, 0f)
    private val sideBySide = GameGeometry(720f, 240f, 3f, 0f)

    @Test fun of_reads_the_view_array() =
        assertEquals(GameGeometry(256f, 384f, 2f / 3f, 1.5708f), GameGeometry.of(floatArrayOf(256f, 384f, 2f / 3f, 1.5708f)))

    @Test fun of_fills_missing_fields() {
        assertEquals(GameGeometry(256f, 384f, 0.5f, 0f), GameGeometry.of(floatArrayOf(256f, 384f, 0.5f)))
        assertEquals(GameGeometry.NONE, GameGeometry.of(null))
    }

    @Test fun rotated_past_a_hundredth_of_a_radian() {
        assertTrue(GameGeometry(224f, 144f, 1.5f, 1.5708f).rotated)
        assertFalse(GameGeometry(224f, 144f, 1.5f, 0.005f).rotated)
    }

    /**
     * Panel up from split on a 3DS game: Azahar switches to its side-by-side frame a frame or so after the options are
     * set. A layout made from the stacked geometry fits that frame at 400:480 (1333 x 1599 on a 2560 x 1600 display,
     * both screens squeezed); laid out again from the new geometry it is 3:1.
     */
    @Test fun azahar_layout_switch_needs_the_new_aspect() {
        assertEquals(PxRect(613f, 0f, 1333f, 1599f), DualScreen.fullFrame(2560f, 1600f, stacked.aspect))
        assertEquals(PxRect(0f, 373f, 2560f, 853f), DualScreen.fullFrame(2560f, 1600f, sideBySide.aspect))
    }
}
