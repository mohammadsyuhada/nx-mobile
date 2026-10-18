package com.nxredux.mobile.runner

import com.nxredux.mobile.options.NdsLandscapeLayout
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.NdsSettings

/**
 * DS landscape touch mode (L2 with the landscape pad shown, [NdsAction.TouchMode]): the pad hides for finger touch on the
 * bottom screen. Pure rules; the host keeps the mode itself and never saves what these return.
 */
object NdsTouchMode {
    /** The "Controls" chip is never fainter than this, so it stays findable at a low Pad Opacity (landscape). */
    const val CHIP_MIN_ALPHA = 0.6f

    /**
     * The layout settings to draw with: in [touchMode], single screen and picture in picture show the bottom screen big (only
     * a full-size bottom screen takes touches), a transient override of [NdsSettings.big] that ends with the mode. Side by
     * side keeps the user's order: its bottom half already takes touches.
     */
    fun settings(s: NdsSettings, touchMode: Boolean): NdsSettings =
        if (touchMode && s.landscape != NdsLandscapeLayout.SIDE_BY_SIDE) s.copy(big = NdsScreen.BOTTOM) else s

    /**
     * Whether L2 enters touch mode ([NdsHotkeys.touchModeAvailable]): the landscape pad is shown (landscape, no [controllers],
     * not already in [touchMode]) on a touchscreen, and the [stylus] is off. With the stylus on, L2 turns it off first.
     */
    fun available(hasTouchscreen: Boolean, landscape: Boolean, controllers: Boolean, touchMode: Boolean, stylus: Boolean): Boolean =
        hasTouchscreen && landscape && !controllers && !touchMode && !stylus

    /** The "Controls" chip's alpha for the landscape pad's [padAlpha]. */
    fun chipAlpha(padAlpha: Float): Float = maxOf(padAlpha, CHIP_MIN_ALPHA)

    /** A game-area corner for the "Controls" chip, in [chipCorner]'s order of preference. END is the right edge. */
    enum class ChipCorner(val top: Boolean, val end: Boolean) {
        TOP_END(true, true), TOP_START(true, false), BOTTOM_END(false, true), BOTTOM_START(false, false),
    }

    /**
     * Where the "Controls" chip goes so it does not cover the touchable screen: the first [ChipCorner] (top-end, top-start,
     * bottom-end, bottom-start; the pad is hidden in touch mode, so the bottom corners are free) whose chip rectangle
     * ([chipW] x [chipH] plus its [margin] from both edges, in [areaW] x [areaH] game-area px) meets no region with touch;
     * if every corner does, the one with the least overlap (the earlier corner on a tie).
     */
    fun chipCorner(regions: List<NdsRegion>, areaW: Float, areaH: Float, chipW: Float, chipH: Float, margin: Float): ChipCorner {
        val w = chipW + margin
        val h = chipH + margin
        val touch = regions.filter { it.touch }
        fun overlap(c: ChipCorner): Float {
            val x0 = if (c.end) areaW - w else 0f
            val y0 = if (c.top) 0f else areaH - h
            return touch.sumOf { r ->
                val ix = minOf(x0 + w, r.dst.x + r.dst.width) - maxOf(x0, r.dst.x)
                val iy = minOf(y0 + h, r.dst.y + r.dst.height) - maxOf(y0, r.dst.y)
                if (ix > 0f && iy > 0f) (ix * iy).toDouble() else 0.0
            }.toFloat()
        }
        return ChipCorner.entries.minBy { overlap(it) }   // minBy keeps the first of equal minima: a free corner wins in order
    }
}
