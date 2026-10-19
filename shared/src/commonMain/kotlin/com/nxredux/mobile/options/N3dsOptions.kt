package com.nxredux.mobile.options

import com.nxredux.mobile.runner.N3dsNewControls

/**
 * Nintendo 3DS (Azahar) screen controls. Unlike the DS, the core draws both screens itself: the layout is Azahar's own
 * core option ([LAYOUT]), which it applies live, so the SELECT + Left/Right hotkey sets that option and saves it to the
 * game file like the DS hotkeys do.
 */
object N3dsOptions {
    const val LAYOUT = "citra_layout_option"
    /** The core's per-axis right-stick deadzone in percent; the stylus d-pad cursor starts just past it. */
    const val DEADZONE = "citra_analog_deadzone"
    const val DEFAULT_DEADZONE = 0.15f

    /** Console Settings → New 3DS Controls ([N3dsNewControls]): auto, on or off, per game with a console default. */
    const val NEW_CONTROLS = "nx_3ds_new_controls"

    val newControlsRow = FrontendOption(
        NEW_CONTROLS, "New 3DS Controls", listOf(N3dsNewControls.AUTO, N3dsNewControls.ON, N3dsNewControls.OFF), N3dsNewControls.AUTO,
        "ZL, ZR and the C-stick on the on-screen pad.\nAuto shows them for games that use them\n(New 3DS and Circle Pad Pro games).\n" +
            "A controller's right stick, L2 and R2 always reach the game.",
        visual = false, valueLabels = listOf("Auto", "On", "Off"),
    )

    fun appliesTo(tag: String): Boolean = tag.trim().uppercase() == "3DS"

    /**
     * How many times smaller Azahar draws the second screen in Top over bottom (nx-mobile's core patch; not a declared
     * option, so never shown or saved): landscape gives the top screen three quarters of the height, portrait keeps both alike.
     */
    const val STACKED_SMALL_PROPORTION = "citra_stacked_small_proportion"

    fun stackedSmallProportion(landscape: Boolean): String = if (landscape) "3" else "1"

    /** SELECT + Right walks this order (Left the other way), wrapping. */
    val LAYOUT_CYCLE = listOf("default", "large_screen", "side_by_side", "single_screen")
    private val LABELS = mapOf(
        "default" to "Top over bottom", "large_screen" to "Large screen",
        "side_by_side" to "Side by side", "single_screen" to "Single screen",
    )

    /** SELECT + Left/Right: the layout [delta] steps from [current] (unknown or missing counts as "default"), as key to value. */
    fun cycleLayout(current: String?, delta: Int): Pair<String, String> {
        val i = LAYOUT_CYCLE.indexOf(current).coerceAtLeast(0)
        return LAYOUT to LAYOUT_CYCLE[(i + delta).mod(LAYOUT_CYCLE.size)]
    }

    /** What the hotkey's notice shows for a layout value. */
    fun layoutLabel(value: String): String = LABELS[value] ?: value

    /** [DEADZONE]'s stored percentage as 0..1; the core's 15% when missing or unreadable. */
    fun deadzone(value: String?): Float = value?.toIntOrNull()?.coerceIn(0, 100)?.let { it / 100f } ?: DEFAULT_DEADZONE
}
