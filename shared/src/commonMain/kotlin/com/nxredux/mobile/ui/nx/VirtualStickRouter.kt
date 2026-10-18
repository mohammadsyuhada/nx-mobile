package com.nxredux.mobile.ui.nx

/**
 * The on-screen analog stick: to the game ([onAnalog]) while playing, and as held menu directions ([onMenu], through its
 * own [AxisDirections], never the hardware stick's, so a controller's resting axes can't cancel it) while the in-game
 * menu is open. [onMenuToggled] lets go of both sides when the menu opens or closes.
 */
class VirtualStickRouter(private val onAnalog: (Float, Float) -> Unit, onMenu: (PadEvent) -> Unit) {
    private val menuAxes = AxisDirections(onMenu)

    fun onStick(x: Float, y: Float, menuOpen: Boolean) {
        if (menuOpen) menuAxes.update(0f, 0f, x, y) else onAnalog(x, y)
    }

    fun onMenuToggled() {
        menuAxes.release()
        onAnalog(0f, 0f)
    }
}
