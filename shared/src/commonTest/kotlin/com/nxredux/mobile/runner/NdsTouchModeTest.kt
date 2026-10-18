package com.nxredux.mobile.runner

import com.nxredux.mobile.options.NdsLandscapeLayout
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.NdsSettings
import com.nxredux.mobile.options.PxRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NdsTouchModeTest {
    @Test fun touch_mode_makes_the_bottom_screen_big_in_picture_in_picture() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.PIP, big = NdsScreen.TOP)
        assertEquals(s.copy(big = NdsScreen.BOTTOM), NdsTouchMode.settings(s, touchMode = true))
    }

    @Test fun touch_mode_makes_the_bottom_screen_the_single_one() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.SINGLE, big = NdsScreen.TOP)
        assertEquals(s.copy(big = NdsScreen.BOTTOM), NdsTouchMode.settings(s, touchMode = true))
    }

    @Test fun side_by_side_keeps_the_users_order_its_bottom_half_already_takes_touch() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.SIDE_BY_SIDE, big = NdsScreen.TOP)
        assertEquals(s, NdsTouchMode.settings(s, touchMode = true))
    }

    @Test fun out_of_touch_mode_the_users_big_screen_stands() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.PIP, big = NdsScreen.TOP)
        assertEquals(s, NdsTouchMode.settings(s, touchMode = false))
    }

    @Test fun a_bottom_big_screen_stays_bottom() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.PIP, big = NdsScreen.BOTTOM)
        assertEquals(s, NdsTouchMode.settings(s, touchMode = true))
    }

    @Test fun the_controls_chip_is_never_fainter_than_sixty_percent() {
        assertEquals(0.6f, NdsTouchMode.chipAlpha(0.4f))
        assertEquals(0.6f, NdsTouchMode.chipAlpha(0f))
        assertEquals(0.8f, NdsTouchMode.chipAlpha(0.8f))
        assertEquals(1f, NdsTouchMode.chipAlpha(1f))
    }

    @Test fun touch_mode_is_offered_only_for_the_landscape_pad_on_a_touchscreen_with_the_stylus_off() {
        assertTrue(NdsTouchMode.available(hasTouchscreen = true, landscape = true, controllers = false, touchMode = false, stylus = false))
        assertFalse(NdsTouchMode.available(hasTouchscreen = false, landscape = true, controllers = false, touchMode = false, stylus = false))
        assertFalse(NdsTouchMode.available(hasTouchscreen = true, landscape = false, controllers = false, touchMode = false, stylus = false))
        assertFalse(NdsTouchMode.available(hasTouchscreen = true, landscape = true, controllers = true, touchMode = false, stylus = false))
        assertFalse(NdsTouchMode.available(hasTouchscreen = true, landscape = true, controllers = false, touchMode = true, stylus = false))
        assertFalse(NdsTouchMode.available(hasTouchscreen = true, landscape = true, controllers = false, touchMode = false, stylus = true))
    }

    // Chip 250x100 px with a 36 px margin (about the Flip's 12 dp plus the chip's own size).
    private fun corner(regions: List<NdsRegion>, w: Float, h: Float) = NdsTouchMode.chipCorner(regions, w, h, 250f, 100f, 36f)

    @Test fun flip_side_by_side_puts_the_chip_over_the_top_screen_on_the_left() {
        // Big = TOP: the touchable bottom screen is on the right, 67.5 px below the top edge: the top-end chip would cover it.
        val s = NdsSettings(landscape = NdsLandscapeLayout.SIDE_BY_SIDE, big = NdsScreen.TOP)
        val regions = NdsLayout.regions(2520f, 1080f, landscape = true, settings = NdsTouchMode.settings(s, touchMode = true), marginPx = 36f)
        assertTrue(regions.any { it.touch && it.dst.x >= 1260f })
        assertEquals(NdsTouchMode.ChipCorner.TOP_START, corner(regions, 2520f, 1080f))
    }

    @Test fun a_centred_single_bottom_screen_leaves_the_top_end_corner_free() {
        val s = NdsSettings(landscape = NdsLandscapeLayout.SINGLE, big = NdsScreen.TOP)
        val regions = NdsLayout.regions(2520f, 1080f, landscape = true, settings = NdsTouchMode.settings(s, touchMode = true), marginPx = 36f)
        assertTrue(regions.single().touch)
        assertEquals(NdsTouchMode.ChipCorner.TOP_END, corner(regions, 2520f, 1080f))
    }

    @Test fun when_every_corner_overlaps_the_least_overlap_wins() {
        fun touch(x: Float, y: Float, w: Float, h: Float) =
            NdsRegion(NdsScreen.BOTTOM, NdsLayout.BOTTOM_SRC, PxRect(x, y, w, h), shaded = true, touch = true)
        // Chip rect 100x100 (margin 0). Top corners: 10000 each; bottom corners: 5000 from the first region, and the
        // bottom-end one another 10000 from the second: bottom-start overlaps least.
        val regions = listOf(touch(0f, 0f, 1000f, 950f), touch(500f, 900f, 500f, 100f))
        assertEquals(NdsTouchMode.ChipCorner.BOTTOM_START, NdsTouchMode.chipCorner(regions, 1000f, 1000f, 100f, 100f, 0f))
    }

    @Test fun equal_overlap_everywhere_keeps_the_first_corner_and_no_regions_mean_top_end() {
        val full = NdsRegion(NdsScreen.BOTTOM, NdsLayout.BOTTOM_SRC, PxRect(0f, 0f, 1000f, 1000f), shaded = true, touch = true)
        assertEquals(NdsTouchMode.ChipCorner.TOP_END, NdsTouchMode.chipCorner(listOf(full), 1000f, 1000f, 100f, 100f, 0f))
        assertEquals(NdsTouchMode.ChipCorner.TOP_END, NdsTouchMode.chipCorner(emptyList(), 1000f, 1000f, 100f, 100f, 0f))
    }
}
