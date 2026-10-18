package com.nxredux.mobile.runner

/**
 * The phone's sensor axes (Android's natural orientation) to the 3DS's: +X left, +Y out of the screen, +Z up the screen,
 * as Azahar's Android frontend turns them for the screen's rotation (src/android/.../jni/ndk_motion.cpp TransformAxes;
 * [rotation] is Surface.ROTATION_*). Not through [WiimoteFrame.display]: in landscape that comes out turned 180° about
 * the screen's normal, reversing both tilts. The console reports gravity, not the accelerometer's reaction to
 * it, so acceleration is negated; it stays in g and the gyro in rad/s (the libretro core turns those into degrees).
 * No level: a 3DS reads its tilt as it is.
 */
object N3dsFrame {
    private fun frame(p: Vec3, rotation: Int) = when (rotation and 3) {
        1 -> Vec3(p.y, p.z, p.x)
        2 -> Vec3(p.x, p.z, -p.y)
        3 -> Vec3(-p.y, p.z, -p.x)
        else -> Vec3(-p.x, p.z, p.y)
    }

    fun accel(phoneG: Vec3, rotation: Int): Vec3 = frame(phoneG, rotation).let { Vec3(-it.x, -it.y, -it.z) }

    fun gyro(phone: Vec3, rotation: Int): Vec3 = frame(phone, rotation)
}
