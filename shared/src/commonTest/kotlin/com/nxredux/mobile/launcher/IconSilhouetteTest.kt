package com.nxredux.mobile.launcher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IconSilhouetteTest {
    private val size = 100

    /** A [size] square layer with [paint]'s pixels at [a]. */
    private fun layer(a: Int = 255, paint: (x: Int, y: Int) -> Boolean) = IntArray(size * size) { i -> if (paint(i % size, i / size)) a else 0 }

    private fun disc(cx: Float, cy: Float, r: Float) = { x: Int, y: Int -> (x + 0.5f - cx).let { it * it } + (y + 0.5f - cy).let { it * it } <= r * r }

    @Test fun anEmptyLayerHasNoShape() {
        assertNull(IconSilhouette.frame(IntArray(size * size), size))
    }

    @Test fun faintPixelsAloneAreNoShape() {
        assertNull(IconSilhouette.frame(layer(a = 20) { _, _ -> true }, size))
    }

    // A logo in the middle: the frame is centred on it, the logo spanning 75% of it, as the built-in glyphs do.
    @Test fun aLogoIsFramedLikeTheGlyphs() {
        val f = assertNotNull(IconSilhouette.frame(layer(paint = disc(50f, 50f, 30f)), size))
        assertEquals(80f, f.side)
        assertEquals(10f, f.x)
        assertEquals(10f, f.y)
    }

    // Off centre and wider than tall: the square follows the shape's centre and its longer side.
    @Test fun anOffCentreBarIsFramedOnItsOwnCentre() {
        val f = assertNotNull(IconSilhouette.frame(layer { x, y -> x in 20 until 50 && y in 60 until 70 || x in 20 until 50 && y == 75 }, size))
        assertEquals(40f, f.side)
        assertEquals(35f - 20f, f.x)
        assertEquals(68f - 20f, f.y)
    }

    // A ring that thin hardly covers its bounds, so it is a logo, not a plate.
    @Test fun aRingIsALogo() {
        val outer = disc(50f, 50f, 35f); val inner = disc(50f, 50f, 28f)
        assertNotNull(IconSilhouette.frame(layer { x, y -> outer(x, y) && !inner(x, y) }, size))
    }

    @Test fun anOpaqueLayerIsABackground() {
        assertNull(IconSilhouette.frame(layer { _, _ -> true }, size))
    }

    // A full circle plate covers most of the icon and reaches its edges.
    @Test fun aFullBleedCircleIsABackground() {
        assertNull(IconSilhouette.frame(layer(paint = disc(50f, 50f, 52f)), size))
    }

    // A legacy icon's square plate, inset: clear of the edges, but a solid block.
    @Test fun aSquarePlateIsABackground() {
        assertNull(IconSilhouette.frame(layer { x, y -> x in 20 until 80 && y in 20 until 80 }, size))
    }

    // A frame along the edges with an empty middle still bleeds to the edges.
    @Test fun aShapeAlongTheEdgesIsABackground() {
        assertNull(IconSilhouette.frame(layer { x, y -> x < 3 || y < 3 || x >= size - 3 || y >= size - 3 }, size))
    }

    // A shadow's faint alpha drops out, a firm pixel is solid, and an edge's smoothing in between is spread over the range.
    @Test fun faintAlphaIsNoShape() {
        assertEquals(0, IconSilhouette.shapeAlpha(0))
        assertEquals(0, IconSilhouette.shapeAlpha(64))
        assertEquals(127, IconSilhouette.shapeAlpha(112))
        assertEquals(255, IconSilhouette.shapeAlpha(160))
        assertEquals(255, IconSilhouette.shapeAlpha(255))
    }
}
