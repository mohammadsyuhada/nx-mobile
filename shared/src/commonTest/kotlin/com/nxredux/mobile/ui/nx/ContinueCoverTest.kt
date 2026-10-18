package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Continue's picture always covers its card: a resume frame captured from a letterboxed game view (the Flip's tall
 * portrait: the game in a band, black above and below) is trimmed to the game before the crop and the zoom fill the card.
 */
class ContinueCoverTest {
    private val black = 0xFF000000.toInt()
    private val grey = 0xFF808080.toInt()

    /** A [w] × [h] frame, black outside [x0, x1) × [y0, y1), grey inside. */
    private fun frame(w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int) =
        IntArray(w * h) { i -> val x = i % w; val y = i / w; if (x in x0 until x1 && y in y0 until y1) grey else black }

    @Test fun aLetterboxedFrameTrimsToItsPicture() {
        assertEquals(HomeCrop(0, 30, 100, 90), homeLetterbox(100, 120, frame(100, 120, 0, 30, 100, 90)))   // bars above and below
        assertEquals(HomeCrop(10, 0, 90, 60), homeLetterbox(100, 60, frame(100, 60, 10, 0, 90, 60)))       // pillarboxed
    }

    @Test fun aFrameWithoutBarsOrAllBlackIsLeftAlone() {
        assertNull(homeLetterbox(100, 60, frame(100, 60, 0, 0, 100, 60)))       // nothing to trim
        assertNull(homeLetterbox(100, 60, IntArray(100 * 60) { black }))        // a black frame: keep it
        assertNull(homeLetterbox(100, 100, frame(100, 100, 45, 45, 55, 55)))    // a speck: under a quarter each way, keep it
    }

    @Test fun nearBlackCountsAsABarAndADarkPictureRowDoesNot() {
        val px = frame(10, 10, 0, 2, 10, 8)
        px[0] = 0xFF040404.toInt()                                               // scaling noise in the bar
        px[2 * 10 + 3] = 0xFF101010.toInt()                                      // a dark pixel in the picture
        assertEquals(HomeCrop(0, 2, 10, 8), homeLetterbox(10, 10, px))
    }

    // Only a centred letterbox is trimmed: a dark scene's black top rows with nothing matching at the bottom are the
    // picture (a night sky), not a bar. A letterbox a pixel off centre (the decoder's rounding) is still one.
    @Test fun onlySymmetricBarsAreTrimmed() {
        assertNull(homeLetterbox(100, 100, frame(100, 100, 0, 20, 100, 100)))   // black top rows, no bottom bar
        assertNull(homeLetterbox(100, 100, frame(100, 100, 15, 0, 100, 100)))   // black left columns only
        assertEquals(HomeCrop(0, 30, 100, 91), homeLetterbox(100, 120, frame(100, 120, 0, 30, 100, 91)))   // 30 and 29
        // Symmetric bars one way, a dark edge the other: only the bars go.
        assertEquals(HomeCrop(0, 20, 100, 80), homeLetterbox(100, 100, frame(100, 100, 12, 20, 100, 80)))
    }

    // A bar is black to within 4 of 255 per channel (the preview is a lossless PNG; only scaling adds noise).
    @Test fun aVeryDarkGreyRowIsPicture() {
        val px = frame(10, 12, 0, 2, 10, 10)
        for (x in 0 until 10) { px[x] = 0xFF060606.toInt(); px[11 * 10 + x] = 0xFF060606.toInt() }   // outer rows: 6, not a bar
        assertNull(homeLetterbox(10, 12, px))
    }

    // The Android trim reads the frame a line at a time from the edges in, so a cover never copies the whole frame.
    @Test fun lineReadsMatchTheWholeFrameAndStopAtThePicture() {
        val (w, h) = 100 to 120
        val px = frame(w, h, 0, 30, w, 90)
        val rowsRead = mutableSetOf<Int>()
        val crop = homeLetterboxBy(
            w, h,
            rowHasPicture = { y -> rowsRead += y; lineHasPicture(px, y * w, 1, w) },
            colHasPicture = { x, top, bottom -> lineHasPicture(px, top * w + x, w, bottom - top) },
        )
        assertEquals(homeLetterbox(w, h, px), crop)
        assertEquals((0..30).toSet() + (89 until h).toSet(), rowsRead)   // the bars and the picture's edge rows only
    }

    @Test fun aLineFindsAPictureAlongItsStep() {
        val px = IntArray(12) { black }.also { it[7] = grey }
        assertEquals(true, lineHasPicture(px, 1, 3, 4))    // 1, 4, 7, 10
        assertEquals(false, lineHasPicture(px, 0, 3, 4))   // 0, 3, 6, 9
    }
}
