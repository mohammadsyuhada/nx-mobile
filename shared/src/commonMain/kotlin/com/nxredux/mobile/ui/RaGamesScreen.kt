package com.nxredux.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxEmptyState
import com.nxredux.mobile.ui.nx.NxRichListBody
import com.nxredux.mobile.ui.nx.NxRichRow

/**
 * The achievement browser's first level: every game with cached achievement data, with its art and progress. [split]: a
 * pane of the column browser ([RaSplitScreen]), titled by its own name; [focused] = false: its dimmed left pane, no hints.
 */
@Composable
fun RaGamesScreen(
    screen: Screen.RaGames, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier,
    onHint: (NxButton) -> Unit,
    split: Boolean = false,
    focused: Boolean = true,
) {
    if (screen.games.isEmpty()) {
        NxEmptyState("No cached games", "Play online once or download game data in Settings", listOf(Hint(NxButton.B, "Back")), onHint, modifier)
        return
    }
    NxRichListBody(
        title = toolPageTitle(screen, split).orEmpty(),
        rows = remember(screen.games) { screen.games.map { g -> raGameSubtitle(g).let { NxRichRow(g.title, it.text, thumbnail = g.art, subtitleTone = it.tone.toNxTone()) } } },   // not per d-pad move
        selectedIndex = selectedIndex,
        hints = if (focused) listOf(Hint(NxButton.B, "Back"), Hint(NxButton.A, "Open")) else emptyList(),
        onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint,
    )
}
