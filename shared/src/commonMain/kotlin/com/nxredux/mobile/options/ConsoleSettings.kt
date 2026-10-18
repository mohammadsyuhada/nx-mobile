package com.nxredux.mobile.options

/**
 * App-owned settings that belong to a console: shown on the in-game Options → Console Settings page,
 * saved like the Frontend rows (`nx_` keys, per game or console). Static and pure, so a settings screen outside a game
 * can offer them without a core. Keyed by console tag, not core: the DS screen rows follow the console's screens; the
 * controller rows are shared ([PadOptions]).
 */
object ConsoleSettings {
    fun tableFor(tag: String): List<FrontendOption> = when {
        NdsOptions.appliesTo(tag) -> NdsOptions.table
        N3dsOptions.appliesTo(tag) -> listOf(N3dsOptions.newControlsRow)
        WiiOptions.appliesTo(tag) -> WiiOptions.table
        else -> emptyList()
    } + graphicsRows(tag) + PadOptions.tableFor(tag)

    private fun graphicsRows(tag: String): List<FrontendOption> =
        if (GraphicsApiOptions.appliesTo(tag)) listOf(GraphicsApiOptions.row) else emptyList()

    /** Every console's rows (keys are unique across them). */
    val all: List<FrontendOption> =
        NdsOptions.table + N3dsOptions.newControlsRow + WiiOptions.table + GraphicsApiOptions.row + PadOptions.all
}
