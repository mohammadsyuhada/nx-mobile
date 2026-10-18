package com.nxredux.mobile.ui.nx

import coil3.transform.Transformation

/** A crop box in pixels: columns [left, right), rows [top, bottom). */
data class HomeCrop(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * A pixel this dark (every channel at most 4 of 255) is a letterbox bar. The resume frame is a lossless PNG
 * (SaveStates writes it with `CompressFormat.PNG`), so a bar is pure black; the margin only covers the decoder's scaling.
 */
private const val BAR_MAX = 4

/** Two bars are the same size (a centred letterbox) within this many px, or 1% of the side, for rounding. */
private const val BAR_SLACK = 2

private fun isBar(argb: Int): Boolean =
    (argb shr 16 and 0xFF) <= BAR_MAX && (argb shr 8 and 0xFF) <= BAR_MAX && (argb and 0xFF) <= BAR_MAX

/**
 * Where a [w] × [h] frame's picture is inside its letterbox ([pixels], ARGB, row by row): the smallest box holding every
 * row and column that is not all black, trimmed only where the bars are symmetric (top ≈ bottom, left ≈ right), as a
 * centred letterbox always is, so a dark scene's black edge on one side stays. Null when there is nothing to trim, or
 * when the box would be under a quarter of the frame either way (a black or nearly black frame keeps its look).
 *
 * A resume frame is a copy of the whole game view, so on a tall portrait screen (the Flip) it holds the game in a band
 * with black above and below; trimmed first, the crop and zoom make it cover Continue's card whatever its shape.
 */
fun homeLetterbox(w: Int, h: Int, pixels: IntArray): HomeCrop? {
    if (w <= 0 || h <= 0 || pixels.size < w * h) return null
    return homeLetterboxBy(
        w, h,
        rowHasPicture = { y -> lineHasPicture(pixels, y * w, 1, w) },
        colHasPicture = { x, top, bottom -> lineHasPicture(pixels, top * w + x, w, bottom - top) },
    )
}

/** Whether any of [count] pixels of [pixels], from [from] every [step], is not a bar. */
internal fun lineHasPicture(pixels: IntArray, from: Int, step: Int, count: Int): Boolean {
    var i = from
    repeat(count) { if (!isBar(pixels[i])) return true; i += step }
    return false
}

/**
 * [homeLetterbox] over a frame read a line at a time: [rowHasPicture] (row y) and [colHasPicture] (column x between
 * rows top and bottom, exclusive). It reads only from each edge inward to the picture, so a caller can scan the edge
 * strips without copying the whole frame.
 */
internal fun homeLetterboxBy(
    w: Int, h: Int, rowHasPicture: (y: Int) -> Boolean, colHasPicture: (x: Int, top: Int, bottom: Int) -> Boolean,
): HomeCrop? {
    if (w <= 0 || h <= 0) return null
    val top = (0 until h).firstOrNull(rowHasPicture) ?: return null
    val bottom = (h - 1 downTo top).first(rowHasPicture) + 1
    val left = (0 until w).first { colHasPicture(it, top, bottom) }
    val right = (w - 1 downTo left).first { colHasPicture(it, top, bottom) } + 1
    fun same(a: Int, b: Int, side: Int) = kotlin.math.abs(a - b) <= maxOf(BAR_SLACK, side / 100)
    val rows = same(top, h - bottom, h)       // bars above and below alike: trim them, else keep every row
    val cols = same(left, w - right, w)
    val crop = HomeCrop(if (cols) left else 0, if (rows) top else 0, if (cols) right else w, if (rows) bottom else h)
    if (crop == HomeCrop(0, 0, w, h)) return null
    if ((crop.right - crop.left) * 4 < w || (crop.bottom - crop.top) * 4 < h) return null
    return crop
}

/** Continue's picture trim ([homeLetterbox]) as an image-loading step, run off the main thread with the decode. */
internal expect fun homeCoverTrim(): Transformation
