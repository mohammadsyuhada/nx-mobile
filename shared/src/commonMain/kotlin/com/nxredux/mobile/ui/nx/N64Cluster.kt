package com.nxredux.mobile.ui.nx

/**
 * The Nintendo 64 face cluster, following the real pad (its layout measured from a photo): a C diamond at the upper
 * right, a big B level with C▼ to the left of the diamond, and a big A close below-right of B (below-left of C▼), so A
 * and B read as a pair. A and B are placed relative to the C diamond's centre. The buttons are packed tight so they can
 * be big (A / B 1.15, C 0.9 of a diamond face button) inside a box barely larger than the diamond's: [slots] are in
 * units of one face button inside a [WIDTH]×[HEIGHT] box, growing left into the gap toward the d-pad / stick.
 * The drawn circles never overlap; the square touch boxes may touch at their corners, outside both circles.
 */
object N64Cluster {
    data class Slot(val button: NxButton, val x: Float, val y: Float, val diameter: Float)

    const val WIDTH = 3.75f
    const val HEIGHT = 3.45f

    private const val BIG = 1.15f
    private const val SMALL = 0.9f
    private const val C_CX = 2.535f    // C diamond centre
    private const val C_CY = 1.2f
    private const val C_STEP = 0.75f   // centre to each C button's centre

    private fun c(button: NxButton, dx: Float, dy: Float) =
        Slot(button, C_CX + dx * C_STEP - SMALL / 2, C_CY + dy * C_STEP - SMALL / 2, SMALL)

    /** A big button whose centre is ([dx], [dy]) from the C diamond's centre. */
    private fun big(button: NxButton, dx: Float, dy: Float) =
        Slot(button, C_CX + dx - BIG / 2, C_CY + dy - BIG / 2, BIG)

    val slots: List<Slot> = listOf(
        big(NxButton.B, -1.96f, C_STEP),    // level with C_DOWN, left of the diamond
        big(NxButton.A, -1.0f, 1.65f),      // below-right of B, below-left of C_DOWN
        c(NxButton.C_UP, 0f, -1f),
        c(NxButton.C_LEFT, -1f, 0f),
        c(NxButton.C_RIGHT, 1f, 0f),
        c(NxButton.C_DOWN, 0f, 1f),
    )
}
