package com.nxredux.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxDialog
import com.nxredux.mobile.ui.nx.NxRow
import com.nxredux.mobile.ui.nx.NxScreenScaffold
import com.nxredux.mobile.ui.nx.NxSignInPrompt

/**
 * Tools -> RetroAchievements -> Settings: the account (Sign in or Sign out, status), game data download, notification
 * and sort preferences, and the destructive resets. Sign in opens the sign-in modal (username and password); Sign out
 * and the destructive resets ask first ([RaConfirm]). [split]: the focused right pane of the column browser ([RaSplitScreen]): its own name as
 * the title ([subPageTitle]), and no dialogs (the split draws them over both panes).
 */
@Composable
fun RaSettingsScreen(
    screen: Screen.RaSettings, state: AppState, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, modifier: Modifier = Modifier,
    onHint: (NxButton) -> Unit,
    split: Boolean = false,
) {
    Box(modifier.fillMaxSize()) {
        NxScreenScaffold(
            title = toolPageTitle(screen, split).orEmpty(),
            rows = remember(screen.rows) { screen.rows.map(::raSettingsNxRow) },   // not per d-pad move
            selectedIndex = selectedIndex,
            showArt = false,
            hints = raSettingsHints(screen.rows.getOrNull(selectedIndex)),
            onSelect = onSelect, onOpen = onOpen, onHint = onHint, rowKind = listRowKind(screen),
        )
        if (!split) RaSettingsDialogs(screen, state, onHint)
    }
}

/** The settings page's sign-in prompt and confirm, whichever is open. */
@Composable
internal fun RaSettingsDialogs(screen: Screen.RaSettings, state: AppState, onHint: (NxButton) -> Unit) {
    if (screen.signingIn) {
        NxSignInPrompt(
            screen.draftUser, screen.draftPassword, state::raDraftUser, state::raDraftPassword,
            onConfirm = { onHint(NxButton.A) }, onCancel = { onHint(NxButton.B) },
        )
    }
    screen.confirm?.let { c ->
        NxDialog(c.title, c.text, onConfirm = { onHint(NxButton.A) }, confirmLabel = c.confirmLabel, onCancel = { onHint(NxButton.B) })
    }
}

/** An RA settings row as an options row: the value rows (LEFT/RIGHT step them, NX Redux ITEM_CYCLE) show `< value >` while selected. */
internal fun raSettingsNxRow(row: RaSettingsRow): NxRow = NxRow(
    row.label, enabled = row.enabled, secondary = row.secondary,
    cycleable = row.enabled && row.kind in RA_CYCLE_KINDS,
)

private val RA_CYCLE_KINDS = setOf(
    RaSettingKind.ENABLED, RaSettingKind.NOTIFICATIONS, RaSettingKind.NOTIFICATION_SECONDS, RaSettingKind.PROGRESS_SECONDS, RaSettingKind.SORT_ORDER,
)
