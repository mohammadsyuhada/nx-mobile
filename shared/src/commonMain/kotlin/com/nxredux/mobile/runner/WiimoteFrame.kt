package com.nxredux.mobile.runner

import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float)

/**
 * The phone's sensor axes to the frame Dolphin reads for a Wiimote. Rest pose: the phone held facing you, upright. Dolphin's remote frame is +X left, +Y back, +Z up; it rotates the sideways remote
 * itself (DolphinLibretro/Input.cpp UpdateAccelerometer), so sideways gets the display frame unrotated.
 */
object WiimoteFrame {
    /** Phone axes (Android's natural orientation) to the current screen's: right, up, out toward you. [rotation] is Surface.ROTATION_*. */
    fun display(phone: Vec3, rotation: Int): Vec3 = when (rotation and 3) {
        1 -> Vec3(phone.y, -phone.x, phone.z)
        2 -> Vec3(-phone.x, -phone.y, phone.z)
        3 -> Vec3(-phone.y, phone.x, phone.z)
        else -> phone
    }

    fun forPad(display: Vec3, pad: WiiPad): Vec3 = when (pad) {
        WiiPad.SIDEWAYS -> display
        else -> Vec3(-display.x, display.z, display.y)
    }

    /** A rotation (row-major 3x3) that takes the captured resting gravity to straight up the screen. */
    class Level private constructor(private val m: FloatArray) {
        fun apply(v: Vec3) = Vec3(
            m[0] * v.x + m[1] * v.y + m[2] * v.z,
            m[3] * v.x + m[4] * v.y + m[5] * v.z,
            m[6] * v.x + m[7] * v.y + m[8] * v.z,
        )

        companion object {
            val NONE = Level(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

            fun capture(gravityDisplay: Vec3): Level {
                val g = gravityDisplay
                val n = sqrt(g.x * g.x + g.y * g.y + g.z * g.z)
                if (n < 1e-3f) return NONE
                val a = Vec3(g.x / n, g.y / n, g.z / n)
                // Rodrigues from a to b = (0, 1, 0): axis a × b, cos = a·b.
                val c = a.y
                val k = Vec3(-a.z, 0f, a.x)   // a × (0,1,0)
                val s2 = k.x * k.x + k.z * k.z
                if (s2 < 1e-8f) {
                    return if (c > 0f) NONE else Level(floatArrayOf(1f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f))   // 180° about X
                }
                val f = (1f - c) / s2
                // R = I + [k]x + [k]x² * f  with k = (kx, 0, kz)
                return Level(floatArrayOf(
                    1f - f * k.z * k.z, -k.z, f * k.x * k.z,
                    k.z, 1f - f * (k.x * k.x + k.z * k.z), -k.x,
                    f * k.x * k.z, k.x, 1f - f * k.x * k.x,
                ))
            }
        }
    }
}
