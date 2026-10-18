package com.nxredux.mobile.runner

import kotlin.math.abs
import kotlin.math.sign

/** A right stick moving the Wii's IR pointer: a full picture width per second at full tilt. */
object WiiPointer {
    const val DEAD_ZONE = 0.15f

    fun velocity(x: Float, y: Float): Pair<Float, Float> = axis(x) to axis(y)

    private fun axis(v: Float): Float {
        val a = abs(v).coerceAtMost(1f)
        if (a <= DEAD_ZONE) return 0f
        return sign(v) * (a - DEAD_ZONE) / (1f - DEAD_ZONE)
    }
}
