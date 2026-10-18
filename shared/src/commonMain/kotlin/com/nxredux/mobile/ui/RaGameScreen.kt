package com.nxredux.mobile.ui

import com.nxredux.mobile.shared.res.nav_icon_filter
import com.nxredux.mobile.shared.res.Res
import com.nxredux.mobile.ui.nx.HintTouch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.achievements.RaController
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxRichListBody
import com.nxredux.mobile.ui.nx.NxRichRow

/**
 * One game's achievements with their badges and state; Y shows only the locked ones. The list alone (no description
 * area; the rows use the space), A opens the highlighted one's detail page: on its own page, or on a split window to the
 * right of this list ([RaSplitScreen]). [split]: a pane of the column browser, titled by the game alone; [focused] =
 * false: its dimmed left pane beside an achievement, no hints.
 */
@Composable
fun RaGameScreen(
    screen: Screen.RaGame, controller: RaController, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier,
    onHint: ((NxButton) -> Unit)? = null,
    split: Boolean = false,
    focused: Boolean = true,
) {
    // Built once per list, not per d-pad move (the selection recomposes this screen).
    val visible = remember(screen.achievements, screen.lockedOnly) { visibleAchievements(screen) }
    val filter = raGameFilterHint(screen.lockedOnly)
    NxRichListBody(
        title = toolPageTitleHead(screen, split).orEmpty(), titleSuffix = toolPageCount(screen),
        rows = remember(visible, screen.badges) { visible.map { a -> raAchievementSubtitle(a).let { NxRichRow(a.title, it.text, thumbnail = raBadgeModel(screen.badges, a), subtitleTone = it.tone.toNxTone()) } } },
        selectedIndex = selectedIndex,
        hints = if (focused) listOf(Hint(NxButton.B, "Back"), Hint(NxButton.A, "Details"), filter) else emptyList(),
        onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint,
        emptyText = if (screen.lockedOnly && screen.achievements.isNotEmpty()) "Everything unlocked." else null,
    )
}

/** Y on a game's achievements: Locked only / Show all; touch, a filter icon lit while Locked only is on. */
internal fun raGameFilterHint(lockedOnly: Boolean): Hint =
    Hint(NxButton.Y, if (lockedOnly) "Show all" else "Locked only", HintTouch.Icon(Res.drawable.nav_icon_filter, lit = lockedOnly))
