package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.nxredux.mobile.settings.GroupTweak
import com.nxredux.mobile.settings.PadLayouts
import kotlin.math.hypot

/** How close (pad units) a dragged group's centre must come to its default spot to snap back onto it. */
const val SNAP_UNITS = 0.25f

/**
 * Where a group whose default rect is [default] is drawn with [tweak] ([unitPx] px a pad unit): scaled
 * about its centre (0.7..1.5, then shrunk further if that would not fit [bounds]) and moved, then kept inside [bounds]
 * so a layout saved on a bigger screen never leaves a group off this one. With no unit yet or an unmeasured (zero-size)
 * group there is nothing to place by: [default].
 */
fun placeGroup(default: Rect, tweak: GroupTweak, unitPx: Float, bounds: Rect): Rect {
    if (!canPlace(default, unitPx)) return default
    val s = tweak.scale.coerceIn(PadLayouts.MIN_SCALE, PadLayouts.MAX_SCALE)
    val fit = minOf(1f, bounds.width / (default.width * s), bounds.height / (default.height * s))
    val w = default.width * s * fit
    val h = default.height * s * fit
    val cx = centreWithin(default.center.x + tweak.dx * unitPx, bounds.left + w / 2f, bounds.right - w / 2f)
    val cy = centreWithin(default.center.y + tweak.dy * unitPx, bounds.top + h / 2f, bounds.bottom - h / 2f)
    return Rect(Offset(cx - w / 2f, cy - h / 2f), Size(w, h))
}

/** [c] kept in [lo]..[hi]; when rounding leaves that empty (a group exactly as big as its bounds) the middle. */
private fun centreWithin(c: Float, lo: Float, hi: Float): Float = if (lo > hi) (lo + hi) / 2f else c.coerceIn(lo, hi)

/** A unit and a non-empty default: [placeGroup] / [clampTweak] can work (else they would divide by zero). */
private fun canPlace(default: Rect, unitPx: Float): Boolean = unitPx > 0f && default.width > 0f && default.height > 0f

/**
 * [tweak] with its offset moved to where [placeGroup] actually draws the group, so a drag past an edge stops there;
 * unchanged when [placeGroup] has nothing to place by.
 */
fun clampTweak(default: Rect, tweak: GroupTweak, unitPx: Float, bounds: Rect): GroupTweak {
    if (!canPlace(default, unitPx)) return tweak
    val placed = placeGroup(default, tweak, unitPx, bounds)
    val scale = tweak.scale.coerceIn(PadLayouts.MIN_SCALE, PadLayouts.MAX_SCALE)
    return GroupTweak((placed.center.x - default.center.x) / unitPx, (placed.center.y - default.center.y) / unitPx, scale)
}

/** [tweak] back on its default spot when it ends within [SNAP_UNITS] of it; its size is kept. */
fun snapToDefault(tweak: GroupTweak): GroupTweak =
    if (hypot(tweak.dx, tweak.dy) <= SNAP_UNITS) tweak.copy(dx = 0f, dy = 0f) else tweak
