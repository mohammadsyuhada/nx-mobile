package com.nxredux.mobile.ui

import com.nxredux.mobile.ui.nx.HintTouch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.nxredux.mobile.scraper.JobPhase
import com.nxredux.mobile.ui.nx.Hint
import com.nxredux.mobile.ui.nx.ListSelection
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxDetailListBody
import com.nxredux.mobile.ui.nx.NxDialog
import com.nxredux.mobile.ui.nx.NxRow
import com.nxredux.mobile.ui.nx.NxSignInPrompt
import com.nxredux.mobile.ui.nx.NxTextPrompt

private val selectBack = listOf(Hint(NxButton.A, "Select"), Hint(NxButton.B, "Back"))

/**
 * Tools → Artwork and its pages, all in the tool-page shape: title, detail
 * lines, whole rows with scroll arrows. The state is in the screen ([ArtworkScreenController] republishes it). One page:
 * the body ([ArtworkPageBody]) with its dialogs ([ArtworkDialogs]) over it; the column browser draws them apart.
 */
@Composable
fun ArtworkScreen(
    screen: Screen, state: AppState, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onHint: (NxButton) -> Unit, modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        ArtworkPageBody(screen, selectedIndex, onSelect, onOpen, onHint)
        ArtworkDialogs(screen, state, onHint)
    }
}

/**
 * An Artwork page without its dialogs. [split]: a pane of the column browser ([ArtworkSplitScreen]), titled without
 * the tool's name; [focused] = false: its dimmed left pane, with no hints.
 */
@Composable
internal fun ArtworkPageBody(
    screen: Screen, selectedIndex: Int, onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onHint: (NxButton) -> Unit,
    modifier: Modifier = Modifier, split: Boolean = false, focused: Boolean = true,
) {
    fun hints(h: List<Hint>) = if (focused) h else emptyList()
    when (screen) {
        is Screen.Artwork -> NxDetailListBody(
            title = "Artwork", details = screen.details, rows = screen.rows.map { NxRow(it.label) },
            selectedIndex = selectedIndex, hints = hints(selectBack), onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint,
        )
        is Screen.ArtworkConsoles -> NxDetailListBody(
            title = toolPageTitle(screen, split).orEmpty(), details = emptyList(),
            rows = screen.rows.map { NxRow(it.name, secondary = "${it.missing} missing") },
            selectedIndex = selectedIndex, hints = hints(listOf(Hint(NxButton.A, "Fetch"), Hint(NxButton.B, "Back"))),
            onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint, rowsAtTop = true,
        )
        is Screen.ArtworkProgress -> ArtworkProgressBody(screen, selectedIndex, onSelect, onOpen, onHint, modifier, split)
        is Screen.ArtworkSettings -> NxDetailListBody(
            title = toolPageTitle(screen, split).orEmpty(), details = emptyList(),
            rows = screen.rows.map(::artworkSettingNxRow),
            selectedIndex = selectedIndex, hints = hints(selectBack), onSelect = onSelect, onOpen = onOpen, modifier = modifier,
            onHint = onHint, rowKind = listRowKind(screen),
        )
        is Screen.ArtworkReset -> NxDetailListBody(
            title = toolPageTitle(screen, split).orEmpty(), details = listOf("Deletes only art this app fetched. Art you added yourself is kept."),
            rows = screen.scopes.map { NxRow(it.label) }, selectedIndex = selectedIndex, headline = false,
            hints = hints(selectBack), onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint,
        )
        else -> Unit
    }
}

/**
 * An Artwork page's open dialog or prompt (Settings' sign-in, key prompt and logout confirm; Reset's and Progress's
 * confirms): over the page, or in a split over both panes ([SplitPanes]'s over).
 */
@Composable
internal fun ArtworkDialogs(screen: Screen, state: AppState, onHint: (NxButton) -> Unit) {
    when (screen) {
        is Screen.ArtworkSettings -> {
            if (screen.signingIn) {
                NxSignInPrompt(
                    screen.draftUser, screen.draftPassword, state::artworkDraftUser, state::artworkDraftPassword,
                    onConfirm = { onHint(NxButton.A) }, onCancel = { onHint(NxButton.B) }, caption = "Sign in to ScreenScraper",
                )
            }
            if (screen.confirmLogout) {
                NxDialog(
                    "Log out of ScreenScraper?", "Art fetches use the anonymous quota until you log in again.",
                    onConfirm = { onHint(NxButton.A) }, confirmLabel = "Logout", onCancel = { onHint(NxButton.B) },
                )
            }
            if (screen.editingKey) {
                NxTextPrompt(
                    "${screen.keyFor.label} API key", screen.draftKey, state::artworkDraftKey,
                    onConfirm = { onHint(NxButton.A) }, onCancel = { onHint(NxButton.B) },
                )
            }
        }
        is Screen.ArtworkReset -> screen.confirm?.let { c ->
            NxDialog(
                "Reset artwork?", "Delete fetched art for ${c.label}? Art you added yourself is kept.",
                onConfirm = { onHint(NxButton.A) }, confirmLabel = "Delete", onCancel = { onHint(NxButton.B) },
            )
        }
        is Screen.ArtworkProgress -> if (screen.confirmCancel) {
            NxDialog(
                "Cancel art fetch?", "Games already fetched keep their art.",
                onConfirm = { onHint(NxButton.A) }, confirmLabel = "Stop", onCancel = { onHint(NxButton.B) },
            )
        }
        else -> Unit
    }
}

/**
 * The Artwork column browser on a split window ([artworkSplitView]): the page the user is on focused on the right (its
 * title without the tool's name); on the left, dimmed, with no hints, the page it came from ([AppState.artworkParent])
 * with the row that opened it highlighted. A tap there opens that row in place of the right pane
 * ([AppState.artworkParentTap]); the left pane's select does nothing, so a tap acts once. The dialogs cover both panes.
 */
@Composable
internal fun ArtworkSplitScreen(state: AppState, s: Screen, selection: State<ListSelection>) {
    // Keyed on the job too: beside a running fetch the right pane stays the same screen, the left pane's counts move on.
    val job = state.artworkJob?.collectAsState()?.value
    val parent = remember(s, job) { state.artworkParent(s) }
    SplitPanes(
        left = {
            parent?.let { p ->
                val tap: (Int) -> Unit = remember(p.screen) { { i -> state.artworkParentTap(p.screen, i) } }
                ArtworkPageBody(
                    p.screen, p.row, onSelect = {}, onOpen = tap, onHint = {},
                    modifier = Modifier.alpha(UNFOCUSED_ALPHA), split = true, focused = false,
                )
            }
        },
        right = { WithIndex(selection) { ArtworkPageBody(s, it, state::select, { state.openSelected() }, state::onButton, split = true) } },
        over = { ArtworkDialogs(s, state, state::onButton) },
    )
}

@Composable
private fun ArtworkProgressBody(
    screen: Screen.ArtworkProgress, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onHint: (NxButton) -> Unit, modifier: Modifier, split: Boolean,
) {
    val phase = screen.job.phase
    val hints = artworkProgressHints(phase)
    NxDetailListBody(
        title = toolPageTitle(screen, split).orEmpty(),
        details = listOfNotNull(progressHeader(screen.job, screen.starting), artworkSummary(screen.job).takeIf { it.isNotEmpty() && !screen.starting }),
        // An ended job's list only keeps the games to look at; the summary says the rest.
        rows = remember(screen.job) { artworkProgressRows(screen.job).map { NxRow(it.name, secondary = itemStatusText(it.status, it.reason)) } },   // not per d-pad move
        selectedIndex = selectedIndex, hints = hints, onSelect = onSelect, onOpen = onOpen, modifier = modifier, onHint = onHint,
    )
}

/**
 * A Settings row as an options row: the On/Off rows (Replace existing art, 2D box art, Wheel, Generate mix) step on
 * LEFT/RIGHT, so they show `< value >` (not a locked one); the account rows' values open the sign-in or key prompt or only
 * read, so no arrows.
 */
internal fun artworkSettingNxRow(row: ArtworkSettingRow): NxRow = NxRow(
    row.label, enabled = row.enabled, secondary = row.secondary,
    cycleable = row.enabled && row.kind.toggle,
)

/**
 * The fetch progress page's hints: Y Pause / Resume and X Cancel while it runs, A Done once it has ended (it clears the
 * job), B Back. On touch Pause, Cancel and Done are buttons under the details, above the list.
 */
internal fun artworkProgressHints(phase: JobPhase): List<Hint> = buildList {
    when (phase) {
        JobPhase.RUNNING -> add(Hint(NxButton.Y, "Pause", HintTouch.Button(primary = true)))
        JobPhase.PAUSED -> add(Hint(NxButton.Y, "Resume", HintTouch.Button(primary = true)))
        JobPhase.DONE, JobPhase.CANCELLED -> add(Hint(NxButton.A, "Done", HintTouch.Button(primary = true)))
        JobPhase.IDLE -> Unit
    }
    if (phase == JobPhase.RUNNING || phase == JobPhase.PAUSED) add(Hint(NxButton.X, "Cancel", HintTouch.Button(primary = false)))
    add(Hint(NxButton.B, "Back"))
}
