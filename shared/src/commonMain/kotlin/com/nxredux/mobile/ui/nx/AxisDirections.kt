package com.nxredux.mobile.ui.nx

/**
 * A hardware hat and left stick as held menu directions: each axis past [DEADZONE] holds a direction (the hat wins over
 * the stick on the same axis), and [onEvent] gets a Down when one starts and an Up when it ends, so an [InputRouter]
 * repeats a held direction as it does a held d-pad key. Pure; the caller reads the axes from its platform's events.
 */
class AxisDirections(private val onEvent: (PadEvent) -> Unit) {
    companion object {
        const val DEADZONE = 0.5f

        /** -1, 0 or 1: [value] past [DEADZONE] either way, else 0. */
        fun step(value: Float): Int = if (value < -DEADZONE) -1 else if (value > DEADZONE) 1 else 0

        /** The [hat]'s direction when it has one, else the [stick]'s. */
        fun combine(hat: Float, stick: Float): Int = step(hat).takeIf { it != 0 } ?: step(stick)
    }

    private var x = 0
    private var y = 0

    fun update(hatX: Float, hatY: Float, stickX: Float, stickY: Float) {
        val newY = combine(hatY, stickY)
        if (newY != y) { emitY(y, down = false); emitY(newY, down = true); y = newY }
        val newX = combine(hatX, stickX)
        if (newX != x) { emitX(x, down = false); emitX(newX, down = true); x = newX }
    }

    /** Ups for whatever is held (a controller unplugged mid-hold never sends its axes back to rest). */
    fun release() {
        emitY(y, down = false); emitX(x, down = false)
        reset()
    }

    /** Forgets what is held without any event (the router was released another way). */
    fun reset() { x = 0; y = 0 }

    private fun emitY(dir: Int, down: Boolean) = emit(if (dir < 0) NxButton.UP else if (dir > 0) NxButton.DOWN else null, down)
    private fun emitX(dir: Int, down: Boolean) = emit(if (dir < 0) NxButton.LEFT else if (dir > 0) NxButton.RIGHT else null, down)
    private fun emit(b: NxButton?, down: Boolean) { if (b != null) onEvent(if (down) PadEvent.Down(b) else PadEvent.Up(b)) }
}
