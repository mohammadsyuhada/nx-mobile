package com.nxredux.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxRow
import com.nxredux.mobile.ui.nx.NxScreenScaffold

/**
 * Tools -> Settings: one row per section, drawn like the Tools page, titled [SETTINGS_TITLE]. A opens the section, B goes
 * back to Tools. [hints] (null: its own) the two-pane Settings' left pane's.
 */
@Composable
fun SettingsMenuScreen(
    screen: Screen.SettingsMenu, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier,
    onHint: ((NxButton) -> Unit)? = null,
    hints: List<Hint>? = null,
) {
    NxScreenScaffold(
        rows = screen.sections.map { NxRow(it.label) },
        selectedIndex = selectedIndex, showArt = false,
        hints = hints ?: listOf(Hint(NxButton.A, "Open"), Hint(NxButton.B, "Back")),
        onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint, title = SETTINGS_TITLE,
    )
}
