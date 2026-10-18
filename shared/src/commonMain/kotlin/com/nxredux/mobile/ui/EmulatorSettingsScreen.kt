package com.nxredux.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.options.CoreOptionFilter
import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxRow
import com.nxredux.mobile.ui.nx.NxScreenScaffold

private val LIST_HINTS = listOf(Hint(NxButton.B, "Back"), Hint(NxButton.A, "Select"))

/**
 * Settings -> Emulators -> Settings: one row per core, its tags as the secondary text. [split]: the two-pane Settings'
 * right pane, titled "Settings" (the left pane already shows Emulators); a single page keeps "Emulator Settings".
 */
@Composable
fun EmulatorListScreen(
    screen: Screen.EmulatorList, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier, onHint: ((NxButton) -> Unit)? = null,
    split: Boolean = false,
) {
    NxScreenScaffold(
        rows = remember(screen.rows) { screen.rows.map { NxRow(it.emulator, secondary = emulatorListTags(it)) } },   // not per d-pad move
        selectedIndex = selectedIndex, showArt = false,
        hints = LIST_HINTS, onSelect = onSelect, onOpen = onOpen, onHint = onHint, title = emulatorListTitle(split), modifier = modifier,
    )
}

/** A core row's tags as the handheld names them, each once ("GPGX" for Genesis Plus GX on every Sega console). */
internal fun emulatorListTags(row: EmulatorListRow): String = row.tags.map(CoreRegistry::handheldTag).distinct().joinToString(" · ")

/** The core list's title: "Settings" in the two-pane right pane (under Emulators on the left), else "Emulator Settings". */
internal fun emulatorListTitle(split: Boolean): String = if (split) "Settings" else "Emulator Settings"

/** The console picker of a core serving several tags. */
@Composable
fun EmulatorConsolesScreen(
    screen: Screen.EmulatorConsoles, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier, onHint: ((NxButton) -> Unit)? = null,
) {
    NxScreenScaffold(
        rows = screen.tags.map { NxRow(CoreRegistry.consoleOf(it)?.displayName ?: it, secondary = CoreRegistry.handheldTag(it)) },
        selectedIndex = selectedIndex, showArt = false,
        hints = LIST_HINTS, onSelect = onSelect, onOpen = onOpen, onHint = onHint, title = screen.emulator, modifier = modifier,
    )
}

/** One tag's (or game's) settings: Console settings, then Core settings; LEFT/RIGHT change a value, written at once. */
@Composable
fun EmulatorOptionsScreen(
    screen: Screen.EmulatorOptions, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier, onHint: ((NxButton) -> Unit)? = null,
) {
    NxScreenScaffold(
        rows = remember(screen.rows) { screen.rows.map(::emulatorNxRow) },   // not per d-pad move
        selectedIndex = selectedIndex, showArt = false,
        hints = emulatorOptionsHints(screen.rows.getOrNull(selectedIndex)),
        onSelect = { i -> if (screen.rows.getOrNull(i)?.selectable == true) onSelect(i) },   // headers and notices take no tap
        onOpen = { i -> if (screen.rows.getOrNull(i)?.selectable == true) onOpen(i) }, onHint = onHint, title = emulatorOptionsTitle(screen), modifier = modifier, rowKind = listRowKind(screen),
    )
}

internal fun emulatorNxRow(row: EmulatorRow): NxRow = when (row) {
    is EmulatorRow.Header -> NxRow(row.label, section = true)   // a group's label: grey, never selected
    EmulatorRow.UseEmulatorSettings -> NxRow("Use emulator settings")
    is EmulatorRow.Console -> NxRow(row.row.option.label, secondary = row.row.option.labelOf(row.row.value), cycleable = true)
    is EmulatorRow.Core -> NxRow(CoreOptionFilter.rowLabel(row.option, row.inCategory), secondary = CoreOptionFilter.valueLabel(row.option), cycleable = true)
    is EmulatorRow.Category -> NxRow(row.label, secondary = "›")
    is EmulatorRow.Notice -> NxRow(row.text)
    EmulatorRow.Gap -> NxRow("", spacer = true)
}

/** "<emulator> — <console> (<TAG>)", or the game's name for Game Settings; an open category is appended. */
internal fun emulatorOptionsTitle(s: Screen.EmulatorOptions): String {
    val p = s.page
    val base = s.game?.game?.shownName ?: "${p.emulator} — ${CoreRegistry.consoleOf(p.tag)?.displayName ?: p.tag} (${CoreRegistry.handheldTag(p.tag)})"
    val category = s.category?.let { key -> p.core.orEmpty().firstOrNull { it.category == key }?.categoryLabel?.ifEmpty { null } ?: key }
    return if (category == null) base else "$base › $category"
}

internal fun emulatorOptionsHints(row: EmulatorRow?): List<Hint> = when (row) {
    is EmulatorRow.Console, is EmulatorRow.Core -> listOf(Hint(NxButton.LEFT, "Change"), Hint(NxButton.B, "Back"), Hint(NxButton.A, "Select"))
    is EmulatorRow.Category, EmulatorRow.UseEmulatorSettings -> LIST_HINTS
    else -> listOf(Hint(NxButton.B, "Back"))
}
