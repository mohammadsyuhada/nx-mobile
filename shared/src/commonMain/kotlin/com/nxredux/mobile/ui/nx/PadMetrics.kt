package com.nxredux.mobile.ui.nx

/**
 * Physical height, in pixels, for the portrait pad band: [padCm] centimetres from the display's real
 * vertical dpi when it looks sane (40..1200), otherwise the density bucket (density * 160). Never more
 * than 60% of [availablePx]. Pure; shared by HandheldShell (menu) and GameActivity (game) so the two
 * cannot drift. See PAD_PORTRAIT_CM.
 */
fun padBandPx(padCm: Float, ydpi: Float, density: Float, availablePx: Float): Float {
    val dpi = if (ydpi in 40f..1200f) ydpi else density * 160f
    val exact = padCm / 2.54f * dpi
    val cap = availablePx * 0.6f
    return if (exact > cap) cap else exact
}
