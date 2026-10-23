package com.nxredux.mobile.launcher

/**
 * Where an app icon layer's shape sits, so a tool tile can draw it as a one-colour glyph like the built-in tools'.
 * The layer is judged by its alpha alone: a logo stays clear of the icon's edges and leaves most of it empty, while a
 * layer that is really a background (an opaque fill, a legacy icon's square or circle plate) does not, and is refused.
 */
object IconSilhouette {
    /** The square to crop, in the layer's pixels; it may reach past the layer's edges (that part is empty). */
    data class Frame(val x: Float, val y: Float, val side: Float)

    /** The share of the frame the shape spans, as the built-in glyphs span 36 of their 48 (MenuArt's menu_icon_*). */
    const val CONTENT = 0.75f

    private const val SEEN = 32        // alpha a pixel needs to count toward the shape's bounds
    private const val SOLID = 128      // alpha a pixel needs to count as covering
    private const val MAX_COVER = 0.8f // more of the icon than this covered: a background, not a logo
    private const val MAX_EDGE = 0.5f  // more of the icon's outer ring than this covered: it bleeds to the edges
    private const val MAX_FILL = 0.9f  // more of the shape's own bounds than this covered: a plain block (a legacy plate)
    private const val FAINT = 64       // [shapeAlpha]: at or below this a pixel is shade, not shape
    private const val FIRM = 160       // [shapeAlpha]: at or above this a pixel is wholly shape

    /**
     * Alpha [a] (0-255) as the shape's: a faint pixel (a long shadow, a soft glow under the logo, which stand out once
     * tinted) drops out, a firm one is opaque, and the edges between keep their smoothing.
     */
    fun shapeAlpha(a: Int): Int = when {
        a <= FAINT -> 0
        a >= FIRM -> 255
        else -> (a - FAINT) * 255 / (FIRM - FAINT)
    }

    /**
     * [alpha] is a [size] × [size] layer's alpha (0-255, [shapeAlpha]'s), row by row, covering the icon's visible
     * square. The frame centred on its shape with the shape at [CONTENT] of it; null when there is no shape or it is
     * not a logo.
     */
    fun frame(alpha: IntArray, size: Int): Frame? {
        require(size > 0 && alpha.size == size * size)
        var minX = size; var minY = size; var maxX = -1; var maxY = -1
        var cover = 0L; var edge = 0; var solid = 0
        for (y in 0 until size) for (x in 0 until size) {
            val a = alpha[y * size + x]
            cover += a
            if (a >= SOLID) {
                solid++
                if (x == 0 || y == 0 || x == size - 1 || y == size - 1) edge++
            }
            if (a >= SEEN) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < 0) return null
        if (cover > MAX_COVER * 255L * size * size) return null
        if (size > 1 && edge > MAX_EDGE * 4 * (size - 1)) return null
        val w = maxX - minX + 1
        val h = maxY - minY + 1
        if (solid > MAX_FILL * w * h) return null
        val side = maxOf(w, h) / CONTENT
        return Frame((minX + maxX + 1) / 2f - side / 2, (minY + maxY + 1) / 2f - side / 2, side)
    }
}
