package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether one tap on item `index` that is not highlighted also opens it (acts as A on it). What launches a game takes
 * two taps (a tap highlights, a tap on the highlighted item plays): a game list's and the game switcher's rows
 * ([NxTapNeverOpens]) and Home's Continue, pinned games and pinned tools ([com.nxredux.mobile.ui.mainRowTakesTwoTaps]).
 * The in-game menu has its own ([inGameRowTakesTwoTaps]). Everything else opens on one tap. The d-pad is unaffected
 * either way (move, then A).
 */
val LocalNxTapOpens = staticCompositionLocalOf<(Int) -> Boolean> { { true } }

/** A list whose every item launches a game: each takes two taps ([LocalNxTapOpens]). */
val NxTapNeverOpens: (Int) -> Boolean = { false }

/**
 * The one tap path of every list, grid and row: a tap on item [index] ([selected]: it is the highlighted one). A
 * highlighted item opens; another is highlighted ([onSelect]) and, when [opens] ([LocalNxTapOpens] for [index]), opened at once.
 * [onOpen] must act on [index] only if [onSelect] took it (a list whose header rows take no tap guards both).
 */
fun nxTap(index: Int, selected: Boolean, opens: Boolean, onSelect: (Int) -> Unit, onOpen: (Int) -> Unit) {
    if (!selected) {
        onSelect(index)
        if (!opens) return
    }
    onOpen(index)
}
