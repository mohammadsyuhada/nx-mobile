package com.nxredux.mobile.ui.nx

/**
 * Accumulates the on-screen d-pad directions currently held and yields a hat vector for a libretro
 * MOTION_SOURCE_DPAD motion event. Convention matches Android AXIS_HAT: x = LEFT(-1)/RIGHT(+1),
 * y = UP(-1)/DOWN(+1). Opposite directions held together cancel to 0. Pure; no platform types.
 */
class DpadHat {
    private val held = HashSet<NxButton>()

    /** Adds [dir] if it is a direction; returns true when the held set changed. */
    fun press(dir: NxButton): Boolean = dir.isDirection && held.add(dir)

    /** Removes [dir]; returns true when it had been held. */
    fun release(dir: NxButton): Boolean = held.remove(dir)

    val x: Int get() = (if (NxButton.RIGHT in held) 1 else 0) - (if (NxButton.LEFT in held) 1 else 0)
    val y: Int get() = (if (NxButton.DOWN in held) 1 else 0) - (if (NxButton.UP in held) 1 else 0)

    fun clear() = held.clear()
}
