package com.nxredux.mobile.ui.nx

/**
 * The Sega 6-button face cluster, following the real pad (the layout of the 8BitDo M30, a Mega Drive 6-button pad):
 * A B C on the bottom row and X Y Z on the top row, each row rising to the right, the top row offset right of the
 * bottom one. Each drawn button is the [NxButton] the Sega [com.nxredux.mobile.runner.PadMap] routes to that Sega
 * button (A = Y, B = X, C = A, X = B, Y = L1, Z = R1), so routing is unchanged. [slots] are in units of one face button
 * (all full size) inside a [WIDTH]×[HEIGHT] box, 0.05 clear of the outermost buttons on every side; the drawn
 * circles never overlap.
 */
object SegaCluster {
    data class Slot(val button: NxButton, val x: Float, val y: Float, val diameter: Float)

    const val WIDTH = 3.75f
    const val HEIGHT = 2.80f

    private const val D = 1.0f

    /** A button whose centre is at ([cx], [cy]). */
    private fun at(button: NxButton, cx: Float, cy: Float) = Slot(button, cx - D / 2, cy - D / 2, D)

    val slots: List<Slot> = listOf(
        at(NxButton.Y, 0.55f, 2.25f),     // A
        at(NxButton.X, 1.7f, 2.0f),     // B
        at(NxButton.A, 2.85f, 1.75f),     // C
        at(NxButton.B, 0.9f, 1.05f),    // X
        at(NxButton.L1, 2.05f, 0.8f),     // Y
        at(NxButton.R1, 3.2f, 0.55f),   // Z
    )

    /** The bottom row, A B C: the only buttons a Sega 3-button pad draws. */
    private val bottomRow: List<Slot> = slots.take(3)

    /**
     * The [slots] not [hidden]. When only the bottom row A B C is left (the Sega 3-button pad), the row is moved as
     * a whole so its bounding box is centred in the [WIDTH]×[HEIGHT] box, keeping its slant, spacing and size, so the
     * three buttons do not sit in the bottom-left corner of a box sized for six.
     */
    fun visibleSlots(hidden: (NxButton) -> Boolean): List<Slot> {
        val visible = slots.filterNot { hidden(it.button) }
        if (visible != bottomRow) return visible
        val dx = (WIDTH - visible.minOf { it.x } - visible.maxOf { it.x + it.diameter }) / 2
        val dy = (HEIGHT - visible.minOf { it.y } - visible.maxOf { it.y + it.diameter }) / 2
        return visible.map { it.copy(x = it.x + dx, y = it.y + dy) }
    }
}
