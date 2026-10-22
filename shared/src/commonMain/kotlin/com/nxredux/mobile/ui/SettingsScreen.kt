package com.nxredux.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxDetailListBody
import com.nxredux.mobile.ui.nx.NxDialog
import com.nxredux.mobile.ui.nx.NxRow
import com.nxredux.mobile.ui.nx.NxRowKind
import com.nxredux.mobile.ui.nx.NxBanner
import com.nxredux.mobile.ui.nx.NxScreenScaffold

/**
 * One Settings page. Rows come from AppState; A acts on the highlighted row, B goes back to the section menu (from Library
 * the index is loaded first, and rescanned only when it no longer fits the settings) or, from a Library sub-page, to
 * Library. The detailed pages ([SettingsSection.detailed]) draw a title, their details and the rows; the rest draw
 * plain rows under their name. A page of only navigation rows uses list rows, one with a value row NX Redux's options rows
 * ([listRowKind]). [titled]: the two-pane Settings' right pane (the split draws the remove dialog); [hints] (null: its
 * own) the pane's.
 */
@Composable
fun SettingsScreen(
    screen: Screen.Settings, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier,
    onHint: ((NxButton) -> Unit)? = null,
    titled: Boolean = false,
    hints: List<Hint>? = null,
    dialog: Boolean = true,   // false: the two-pane Settings draws the remove dialog over both panes
) {
    val shownHints = hints ?: settingsHints(screen.rows, selectedIndex)
    Box(modifier.fillMaxSize()) {
        val rows = remember(screen.rows) { screen.rows.map(::settingsNxRow) }   // not per d-pad move
        val rowKind = listRowKind(screen)
        if (screen.section.layoutGroup) {
            // A Layouts group: its values above a picture of them (B goes back).
            LayoutPreviewPage(
                screen.section.label, emptyList(), rows, screen.shot?.let(::layoutShotPicture), LAYOUT_PICTURE_AR, selectedIndex,
                shownHints, onSelect, onOpen, onHint ?: {}, landscapeWidth = 1f, titleAction = false,   // the rows across the page, as the other Settings pages'
            )
        } else if (screen.section.detailed) {
            NxDetailListBody(
                title = screen.section.label, details = screen.details, rows = rows, selectedIndex = selectedIndex,
                hints = shownHints, onSelect = onSelect, onOpen = onOpen, onHint = onHint,
                headline = screen.section != SettingsSection.DEFAULT_EMULATORS,   // its lines explain, they are not a status
                headlineStartEllipsis = screen.section == SettingsSection.LIBRARY,   // the home's path: keep its end
                rowKind = rowKind,
            )
        } else {
            NxScreenScaffold(
                rows = rows,
                selectedIndex = selectedIndex, showArt = false,
                hints = shownHints, title = screen.section.label,   // its name over its rows, as the Settings menu's
                onSelect = onSelect, onOpen = onOpen, onHint = onHint, rowKind = rowKind,
                header = screen.banner?.let { text -> { NxBanner(text) } },
            )
        }
        if (dialog && screen.confirmRemove != null) SettingsRemoveDialog(onHint)
    }
}

/** "Remove folder?" over a ROM folders page ([Screen.Settings.confirmRemove]): A removes, B keeps it. */
@Composable
internal fun SettingsRemoveDialog(onHint: ((NxButton) -> Unit)?) {
    NxDialog(
        "Remove folder", "Stop scanning this folder? Its ROMs stay where they are.",
        onConfirm = { onHint?.invoke(NxButton.A) }, confirmLabel = "Remove", onCancel = { onHint?.invoke(NxButton.B) },
    )
}

/**
 * The hint bar for the row at [selected]: no A on a read-only row (About's info rows, or a page of only those), "A Copy"
 * on About's Copy device info, "A Select" elsewhere.
 */
internal fun settingsHints(rows: List<SettingsRow>, selected: Int): List<Hint> {
    val back = Hint(NxButton.B, "Back")
    val kind = rows.getOrNull(selected)?.kind
    return when {
        kind == SettingsKind.COPY_INFO -> listOf(back, Hint(NxButton.A, "Copy"))
        kind == SettingsKind.INFO || rows.all { it.kind == SettingsKind.INFO } -> listOf(back)
        else -> listOf(back, Hint(NxButton.A, "Select"))
    }
}

/**
 * How a list page draws its rows: the "Label: Value" pages (the Settings pages with a value row, the RetroAchievements
 * settings, an emulator's options, the Artwork settings and account) use NX Redux's options row; navigation pages (the
 * Settings pages of only page and action rows, [SettingsSection.navigation]), category lists, pickers and game lists keep the pill.
 */
internal fun listRowKind(screen: Screen): NxRowKind = when (screen) {
    is Screen.Settings -> if (screen.section.navigation) NxRowKind.PILL else NxRowKind.OPTIONS
    is Screen.RaSettings, is Screen.EmulatorOptions, is Screen.ArtworkSettings -> NxRowKind.OPTIONS
    else -> NxRowKind.PILL
}

/** A Settings row as an options row: every value row steps on LEFT/RIGHT (NX Redux ITEM_CYCLE), so it shows `< value >` while selected and enabled. */
internal fun settingsNxRow(row: SettingsRow): NxRow = NxRow(
    row.label, enabled = row.enabled, secondary = row.secondary,
    cycleable = row.enabled && when (row.kind) {
        SettingsKind.PAD_THEME,
        SettingsKind.DEFAULT_TAG, SettingsKind.MENU_STYLE, SettingsKind.GAME_LIST_STYLE,
        SettingsKind.MENU_ORIENTATION, SettingsKind.GAME_LIST_ORIENTATION, SettingsKind.GAME_LIST_VERTICAL_ALIGNMENT,
        SettingsKind.CONTROLLER_ART, SettingsKind.TAB_SHOWN, SettingsKind.SHELF_SHOWN, SettingsKind.HINT_SHOWN, SettingsKind.LIST_ART, SettingsKind.BACKDROP_ART, SettingsKind.PAGE_TITLE, SettingsKind.BUTTON_HINTS, SettingsKind.EXTRA_INFO, SettingsKind.HOME_STATS, SettingsKind.LAYOUT_OPTIONS, SettingsKind.ACHIEVEMENTS_PANEL, SettingsKind.SWITCHER_LAYOUT, SettingsKind.PAD_VIBRATION, SettingsKind.GAME_RUMBLE, SettingsKind.MENU_PAD, SettingsKind.PAD_LAYOUT_CONSOLE, SettingsKind.ACCENT, SettingsKind.SECONDARY_ACCENT, SettingsKind.TEXT_SIZE, SettingsKind.DEBUG_LOG, SettingsKind.SECOND_SCREEN_ENABLE, SettingsKind.BOTTOM_SCREEN, SettingsKind.PAUSE_UNFOCUSED, SettingsKind.SAVE_FORMAT, SettingsKind.STATE_FORMAT, SettingsKind.EXTRACTED_NAME -> true
        else -> false
    },
)
