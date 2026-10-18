package com.nxredux.mobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtChoiceTest {
    @Test fun boxChain() {
        assertEquals(BoxShown.Art3d("3d"), boxShown("3d", "2d", "g"))
        assertEquals(BoxShown.Cover("2d"), boxShown(null, "2d", "g"))
        assertEquals(BoxShown.Cover("g"), boxShown(null, null, "g"))
        assertEquals(BoxShown.Placeholder, boxShown(null, null, null))
        // A url that failed to load falls to the next link.
        assertEquals(BoxShown.Cover("2d"), boxShown("3d", "2d", "g", failed = setOf("3d")))
        assertEquals(BoxShown.Cover("g"), boxShown("3d", "2d", "g", failed = setOf("3d", "2d")))
        assertEquals(BoxShown.Cover("g"), boxShown(null, "2d", "g", failed = setOf("2d")))
        assertEquals(BoxShown.Placeholder, boxShown("3d", "2d", "g", failed = setOf("3d", "2d", "g")))
    }

    @Test fun backgroundPrefersScreenshot() {
        assertEquals("s", backgroundArt("s", "h", "g", portrait = false))
        assertEquals("s", backgroundArt("s", "h", "g", portrait = true))
    }

    @Test fun landscapeUsesHero() = assertEquals("h", backgroundArt(null, "h", "g", portrait = false))

    @Test fun portraitUsesGrid() = assertEquals("g", backgroundArt(null, "h", "g", portrait = true))

    @Test fun noCrossFallback() {
        assertNull(backgroundArt(null, null, "g", portrait = false))
        assertNull(backgroundArt(null, "h", null, portrait = true))
        assertNull(backgroundArt(null, null, null, portrait = false))
    }
    private fun tileUrl(w: Float, h: Float, s: String?, hero: String?, grid: String?, abstract: String? = null) = tileArt(w, h, s, hero, grid, abstract)?.url

    // A hero fills its tile's height, anchored right (the subject's side); every other picture is centre-cropped.
    @Test fun aHeroKeepsItsFullHeightAnchoredRight() {
        assertEquals(TileArt("h", TileFit.HERO), tileArt(340f, 240f, null, "h", "g"))
        assertEquals(TileArt("h", TileFit.HERO), tileArt(183f, 117f, null, "h", "g", abstract = "a"))
        assertEquals(TileArt("s", TileFit.CROP), tileArt(340f, 240f, "s", "h", "g"))
        assertEquals(TileArt("g", TileFit.CROP), tileArt(140f, 154f, null, "h", "g"))
        assertEquals(TileArt("a", TileFit.CROP), tileArt(340f, 240f, null, null, null, abstract = "a"))
    }

    // Grid and Carousel tiles: the screenshot, else the stand-in nearer the tile's shape, never the other one.
    @Test fun tileTakesScreenshotFirst() {
        assertEquals("s", tileUrl(340f, 240f, "s", "h", "g"))
        assertEquals("s", tileUrl(140f, 154f, "s", "h", "g"))
    }

    @Test fun wideTileTakesHero() {
        assertEquals("h", tileUrl(340f, 240f, null, "h", "g"))   // Carousel
        assertEquals("h", tileUrl(183f, 117f, null, "h", "g"))   // tall-portrait Grid
        assertNull(tileUrl(340f, 240f, null, null, "g"))
    }

    @Test fun tallOrSquareTileTakesGrid() {
        assertEquals("g", tileUrl(140f, 154f, null, "h", "g"))   // Grid
        assertEquals("g", tileUrl(150f, 150f, null, "h", "g"))
        assertNull(tileUrl(140f, 154f, null, "h", null))
    }

    // The generated abstract picture is a tile's last fallback, after the screenshot and the stand-in of its shape.
    @Test fun abstractArtIsTheLastFallback() {
        assertEquals("s", tileUrl(140f, 154f, "s", "h", "g", abstract = "a"))
        assertEquals("g", tileUrl(140f, 154f, null, "h", "g", abstract = "a"))
        assertEquals("h", tileUrl(340f, 240f, null, "h", "g", abstract = "a"))
        assertEquals("a", tileUrl(140f, 154f, null, "h", null, abstract = "a"))   // a hero only: no stand-in for a tall tile
        assertEquals("a", tileUrl(340f, 240f, null, null, null, abstract = "a"))
        assertNull(tileUrl(0f, 0f, null, "h", "g", abstract = "a"))                 // unmeasured: nothing yet
    }

    @Test fun unmeasuredTileHasNoStandIn() {
        assertNull(tileUrl(0f, 0f, null, "h", "g"))
        assertEquals("s", tileUrl(0f, 0f, "s", "h", "g"))
    }
}
