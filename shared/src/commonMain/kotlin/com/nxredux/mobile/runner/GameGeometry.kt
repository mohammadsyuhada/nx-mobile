package com.nxredux.mobile.runner

import kotlin.math.abs

/**
 * The running game's geometry as the view reports it (`[baseWidth, baseHeight, aspectRatio, rotation]`, zeros before it
 * loads). A core can change it mid-game (SET_GEOMETRY): Azahar does when its layout option changes, as the dual-screen
 * modes switch it between the stacked and the side-by-side frame.
 */
data class GameGeometry(val width: Float, val height: Float, val aspect: Float, val rotation: Float) {
    /** The core turns its picture (SET_ROTATION, radians). */
    val rotated: Boolean get() = abs(rotation) > ROTATED

    /** What a change from this geometry to [next] needs: the layouts made from the old size and aspect are stale. */
    fun changeTo(next: GameGeometry): Change = Change(
        relayout = width != next.width || height != next.height || aspect != next.aspect,
        rotation = rotated != next.rotated,
    )

    /** [relayout]: a new frame size or aspect (the frontend lays out again); [rotation]: the core turned its picture or back. */
    data class Change(val relayout: Boolean, val rotation: Boolean)

    companion object {
        private const val ROTATED = 0.01f

        val NONE = GameGeometry(0f, 0f, 0f, 0f)

        /** From the view's array; missing fields (an older library's three) are 0. */
        fun of(g: FloatArray?): GameGeometry =
            if (g == null) NONE else GameGeometry(g.getOrElse(0) { 0f }, g.getOrElse(1) { 0f }, g.getOrElse(2) { 0f }, g.getOrElse(3) { 0f })
    }
}
