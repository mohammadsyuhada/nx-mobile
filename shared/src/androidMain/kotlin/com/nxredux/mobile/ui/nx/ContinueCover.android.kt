package com.nxredux.mobile.ui.nx

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/** [homeLetterbox] on the decoded bitmap: its picture cut out of the black bars, or the bitmap as it is. */
private object LetterboxTrim : Transformation() {
    override val cacheKey: String = "nx.letterboxTrim"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        // One row and one column buffer, read from the edges inward: only the bars and the picture's first line are read.
        val row = IntArray(w)
        val col = IntArray(h)
        val crop = homeLetterboxBy(
            w, h,
            rowHasPicture = { y -> input.getPixels(row, 0, w, 0, y, w, 1); lineHasPicture(row, 0, 1, w) },
            colHasPicture = { x, top, bottom -> input.getPixels(col, 0, 1, x, top, 1, bottom - top); lineHasPicture(col, 0, 1, bottom - top) },
        ) ?: return input
        return Bitmap.createBitmap(input, crop.left, crop.top, crop.right - crop.left, crop.bottom - crop.top)
    }
}

internal actual fun homeCoverTrim(): Transformation = LetterboxTrim
