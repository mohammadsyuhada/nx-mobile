package com.nxredux.mobile.scraper

import com.nxredux.mobile.library.ArtRefs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Has art: a box (boxart, box2d or grid) and a background (screenshot or hero). */
class ArtHaveTest {
    @Test fun boxAndScreenshotIsComplete() = assertTrue(ArtHave(box = true, screenshot = true).complete)

    @Test fun boxAndHeroIsComplete() = assertTrue(ArtHave(box = true, hero = true).complete)

    @Test fun gridCountsAsBox() {
        val have = ArtHave.of(ArtRefs(grid = "g.jpg", screenshot = "s.png"))
        assertTrue(have.box)
        assertTrue(have.grid)
        assertTrue(have.complete)
    }

    @Test fun screenshotAloneIsNotComplete() {
        assertFalse(ArtHave(screenshot = true).complete)
        assertFalse(ArtHave.of(ArtRefs(screenshot = "s.png", hero = "h.jpg")).complete)
    }

    @Test fun boxAloneIsNotComplete() {
        assertFalse(ArtHave(box = true).complete)
        assertFalse(ArtHave.of(ArtRefs(boxart = "b.png", box2d = "c.jpg")).complete)
    }

    @Test fun withGenerateMixAGameNeedsItsMix() {
        val art = ArtRefs(boxart = "b.png", screenshot = "s.png")
        assertTrue(ArtHave.of(art).complete)
        assertFalse(ArtHave.of(art, needMix = true).complete)
        assertTrue(ArtHave.of(art.copy(mix = "m.png"), needMix = true).complete)
        assertFalse(ArtHave.of(ArtRefs(mix = "m.png"), needMix = true).complete)   // a mix alone is not art
    }

    @Test fun box2dAndWheelNeverCount() {
        val have = ArtHave.of(ArtRefs(box2d = "c.png", wheel = "w.png"))
        assertTrue(have.box2d && have.wheel && have.box)
        assertFalse(ArtHave.of(ArtRefs(wheel = "w.png", screenshot = "s.png")).complete)
    }
}
