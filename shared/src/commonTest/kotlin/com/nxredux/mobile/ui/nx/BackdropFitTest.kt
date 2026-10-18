package com.nxredux.mobile.ui.nx

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackdropFitTest {
    // The Flip's portrait menu area (tall), the Fold cover's (wider), and a landscape one.
    private val areas = listOf(Size(1080f, 1500f), Size(1080f, 1000f), Size(2400f, 1000f), Size(2520f, 1080f))
    // 1942 (a vertical shooter, 224x256), a DS pair (256x384), a 4:3 shot, a GB 10:9 one, a wide 16:9 one.
    private val arts = listOf(Size(224f, 256f), Size(256f, 384f), Size(320f, 240f), Size(160f, 144f), Size(1920f, 1080f))

    private fun fit(art: Size, area: Size) = backdropFit(art.width, art.height, area.width, area.height, fade = true, centred = false)
    private fun scaleOf(art: Size, area: Size) = fit(art, area).scale.computeScaleFactor(art, area).scaleX

    @Test fun gameArtFillsTheFullHeightOnAnyAspect() {
        for (area in areas) for (art in arts) {
            val scaled = art.height * fit(art, area).scale.computeScaleFactor(art, area).scaleY
            assertTrue(scaled >= area.height - 0.5f, "art $art in $area is ${scaled}px tall")
        }
    }

    @Test fun fadeEdgeIsWhereTheFadeStopsBeingOpaqueOnTheBottomEdge() {
        val (w, h) = 2520f to 1080f
        val (a, b) = backdropFadeLine(w, h)
        val x = fadeOpaqueEdgeX(w, h)
        val t = ((x - a.x) * (b.x - a.x) + (h - a.y) * (b.y - a.y)) / ((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
        assertEquals(FADE_OPAQUE_STOP, t, 1e-4f)
        assertTrue(x > 0f && x < w * 0.5f, "edge at $x")
        // The line leans left: higher up, the same stop is further right.
        val tTop = ((x - a.x) * (b.x - a.x) + (0f - a.y) * (b.y - a.y)) / ((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
        assertTrue(tTop < FADE_OPAQUE_STOP)
    }

    @Test fun narrowArtOnALandscapeScreenReachesPastTheFadeEdge() {
        val area = Size(2520f, 1080f)
        for (art in listOf(Size(320f, 240f), Size(160f, 144f))) {   // 4:3, GB 10:9
            val f = fit(art, area)
            assertEquals(Alignment.CenterEnd, f.alignment)
            assertEquals(0f, f.shift)
            val s = scaleOf(art, area)
            val left = area.width - art.width * s
            val edge = fadeOpaqueEdgeX(area.width, area.height)
            assertTrue(left < edge, "left edge $left is right of the fade edge $edge")
            assertEquals(edge - FADE_EDGE_MARGIN * area.width, left, 0.5f)
            assertTrue(art.height * s > area.height)   // top and bottom cropped
        }
    }

    @Test fun artThatAlreadyCoversStaysAtFullHeight() {
        // 16:9 on 2520x1080: full height is 1920 wide, its left edge at 600, already left of the fade edge.
        val wide = Size(1920f, 1080f)
        val area = Size(2520f, 1080f)
        assertTrue(area.width - 1920f < fadeOpaqueEdgeX(area.width, area.height) - FADE_EDGE_MARGIN * area.width)
        assertEquals(1f, scaleOf(wide, area), 1e-4f)
        // A screen exactly 4:3 with 4:3 art: full height covers it.
        assertEquals(1080f / 240f, scaleOf(Size(320f, 240f), Size(1440f, 1080f)), 1e-4f)
    }

    @Test fun portraitConsoleAndMarkFitsAreUnchanged() {
        val portrait = Size(1080f, 1500f)
        val f = fit(Size(320f, 240f), portrait)
        assertEquals(ContentScale.FillHeight, f.scale)
        assertEquals(0.20f, f.shift)
        assertEquals(0.20f, fit(Size(1920f, 1080f), portrait).shift)
        val tall = fit(Size(224f, 256f), Size(2520f, 1080f))
        assertEquals(ContentScale.Crop, tall.scale)
        assertEquals(Alignment.TopCenter, tall.alignment)
        val console = backdropFit(320f, 240f, 2520f, 1080f, fade = false, centred = false)
        assertEquals(ContentScale.FillHeight, console.scale)
        assertEquals(0f, console.shift)
        assertEquals(ContentScale.Crop, backdropFit(null, null, 2520f, 1080f, fade = false, centred = true).scale)
        // Size not known yet: full height.
        assertEquals(ContentScale.FillHeight, backdropFit(null, null, 2520f, 1080f, fade = true, centred = false).scale)
    }
}
