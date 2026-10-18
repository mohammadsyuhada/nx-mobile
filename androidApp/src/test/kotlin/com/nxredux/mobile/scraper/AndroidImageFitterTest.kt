package com.nxredux.mobile.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidImageFitterTest {
    @Test fun sampleSizeNeverDropsBelowTheFittedSize() {
        assertEquals(1, FitterGeometry.sampleSize(640, 480, 640, 480))
        assertEquals(1, FitterGeometry.sampleSize(1279, 2000, 640, 1000))
        assertEquals(2, FitterGeometry.sampleSize(1280, 960, 640, 480))
        assertEquals(4, FitterGeometry.sampleSize(4000, 3000, 900, 675))
        assertEquals(1, FitterGeometry.sampleSize(10, 10, 10, 10))
        assertEquals(2, FitterGeometry.sampleSize(3840, 1240, 1920, 620))   // a 4K hero decodes at half, already the cap
    }

    @Test fun sampleSizeKeepsEveryCapsFittedSize() {
        for (slot in ArtSlot.entries) {
            val cap = ArtCaps.of(slot)
            for ((w, h) in listOf(4000 to 3000, 3000 to 4000, 1300 to 5000, 3840 to 1240, 920 to 1430, 256 to 224)) {
                val (fw, fh) = ArtFit.size(w, h, cap)
                val s = FitterGeometry.sampleSize(w, h, fw, fh)
                assertTrue("$slot ${w}x$h", w / s >= fw && h / s >= fh)
            }
        }
    }

    @Test fun notificationProgressParsesTheControllerText() {
        assertEquals(3 to 10, ScrapeNotificationText.progressOf("Fetching art 3/10"))
        assertEquals(10 to 10, ScrapeNotificationText.progressOf("Fetching art 12/10"))
        assertNull(ScrapeNotificationText.progressOf("Fetching art 0/0"))
        assertNull(ScrapeNotificationText.progressOf(null))
        assertNull(ScrapeNotificationText.progressOf("Starting"))
        assertEquals("Fetching art 3/10", ScrapeNotificationText.body("Fetching art 3/10"))
        assertEquals("Starting…", ScrapeNotificationText.body(null))
    }
}
