package com.nxredux.mobile.options

import kotlin.test.Test
import kotlin.test.assertEquals

class FrontendLayoutTest {
    private fun assertRect(expected: PxRect, actual: PxRect) {
        assertEquals(expected.x, actual.x, 0.01f, "x")
        assertEquals(expected.y, actual.y, 0.01f, "y")
        assertEquals(expected.width, actual.width, 0.01f, "width")
        assertEquals(expected.height, actual.height, 0.01f, "height")
    }

    private val gb = 160f / 144f

    @Test fun without_overlay_the_box_is_the_whole_area() {
        val l = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.ASPECT, 0, 0, overlayAspect = null)
        assertRect(PxRect(0f, 0f, 1080f, 1440f), l.box)
    }

    @Test fun overlay_box_is_the_largest_centred_box_of_its_aspect() {
        val portrait = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.ASPECT, 0, 0, 4f / 3f)
        assertRect(PxRect(0f, 315f, 1080f, 810f), portrait.box)
        val landscape = frontendLayout(2400f, 1080f, 160, 144, gb, ScreenScaling.ASPECT, 0, 0, 16f / 9f)
        assertRect(PxRect(240f, 0f, 1920f, 1080f), landscape.box)
    }

    @Test fun aspect_fits_keeping_the_core_aspect() {
        val l = frontendLayout(1080f, 1440f, 256, 224, 4f / 3f, ScreenScaling.ASPECT, 0, 0, null)
        assertRect(PxRect(0f, 315f, 1080f, 810f), l.content)
    }

    @Test fun native_uses_the_largest_whole_multiple_that_fits() {
        val l = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.NATIVE, 0, 0, null)
        assertRect(PxRect(60f, 288f, 960f, 864f), l.content)   // x6
    }

    @Test fun native_inside_an_overlay_box() {
        val l = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.NATIVE, 0, 0, 4f / 3f)
        assertRect(PxRect(140f, 360f, 800f, 720f), l.content)  // x5 in the 1080x810 box at y=315
    }

    @Test fun native_falls_back_to_aspect_when_the_core_is_bigger_than_the_area() {
        val l = frontendLayout(600f, 400f, 640, 480, 4f / 3f, ScreenScaling.NATIVE, 0, 0, null)
        assertRect(PxRect(33.33f, 0f, 533.33f, 400f), l.content)
    }

    @Test fun native_without_a_known_core_size_falls_back_to_aspect() {
        val l = frontendLayout(1080f, 1440f, 0, 0, 4f / 3f, ScreenScaling.NATIVE, 0, 0, null)
        assertRect(PxRect(0f, 315f, 1080f, 810f), l.content)
    }

    @Test fun fullscreen_stretches_to_the_box() {
        val l = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.FULLSCREEN, 0, 0, 4f / 3f)
        assertRect(PxRect(0f, 315f, 1080f, 810f), l.content)
    }

    @Test fun offset_scales_by_box_height_over_768_and_moves_the_content() {
        val l = frontendLayout(1080f, 1440f, 160, 144, gb, ScreenScaling.FULLSCREEN, 64, -32, 4f / 3f)
        assertEquals(67.5f, l.offsetX, 0.01f)    // 64 x 810 / 768
        assertEquals(-33.75f, l.offsetY, 0.01f)
        assertRect(PxRect(67.5f, 281.25f, 1080f, 810f), l.content)
    }

    @Test fun unknown_aspect_uses_the_core_size_then_the_box() {
        val l = frontendLayout(1000f, 1000f, 200, 100, 0f, ScreenScaling.ASPECT, 0, 0, null)
        assertRect(PxRect(0f, 250f, 1000f, 500f), l.content)
        val none = frontendLayout(1000f, 1000f, 0, 0, 0f, ScreenScaling.ASPECT, 0, 0, null)
        assertRect(PxRect(0f, 0f, 1000f, 1000f), none.content)
    }

    @Test fun empty_area_gives_an_empty_layout() {
        val l = frontendLayout(0f, 1440f, 160, 144, gb, ScreenScaling.NATIVE, 10, 10, 4f / 3f)
        val zero = PxRect(0f, 0f, 0f, 0f)
        assertEquals(FrontendLayout(zero, zero, 0f, 0f), l)
    }
}
