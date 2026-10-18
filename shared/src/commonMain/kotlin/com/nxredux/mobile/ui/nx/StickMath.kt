package com.nxredux.mobile.ui.nx

import kotlin.math.sqrt

/**
 * Normalizes a thumbstick knob offset (pixels from centre) to libretro analog axes in [-1, 1].
 * Past [maxRadius] the vector is clamped to unit length. y is positive-down to match how
 * MOTION_SOURCE_ANALOG_LEFT is fed from Android's AXIS_Y. Pure; no platform types.
 */
fun normalizeStick(offsetX: Float, offsetY: Float, maxRadius: Float): Pair<Float, Float> {
    if (maxRadius <= 0f) return 0f to 0f
    val dist = sqrt(offsetX * offsetX + offsetY * offsetY)
    if (dist <= 0f) return 0f to 0f
    val scale = if (dist > maxRadius) maxRadius / dist else 1f
    return (offsetX * scale / maxRadius) to (offsetY * scale / maxRadius)
}
