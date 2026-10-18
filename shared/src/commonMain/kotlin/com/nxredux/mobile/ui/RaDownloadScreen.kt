package com.nxredux.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxredux.mobile.achievements.RaController
import com.nxredux.mobile.achievements.RaDownloadPhase
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxDetailListBody
import com.nxredux.mobile.ui.nx.NxDialog
import com.nxredux.mobile.ui.nx.NxRow

/**
 * The game data download's progress ([RaController.download]), as Artwork's fetch progress: its count and time left (or
 * phase), what the finished games came to, and a row per game still to do (the one in flight first) followed by the
 * skipped and failed; a game that finished well leaves the list. Y pauses or resumes, X cancels, A (Done, once it has
 * ended) clears it, B goes back (the download runs on); "Cancel download?" over it when [Screen.RaDownload.confirmCancel].
 */
@Composable
fun RaDownloadScreen(
    screen: Screen.RaDownload, controller: RaController, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onHint: (NxButton) -> Unit, modifier: Modifier = Modifier,
) {
    val download by controller.download.collectAsState()
    Box(modifier.fillMaxSize()) {
        val d = download
        val rows = remember(d?.items) { d?.items.orEmpty().map { NxRow(it.name, secondary = raDownloadStatus(it)) } }   // not per d-pad move
        NxDetailListBody(
            title = subPageTitle(ToolKind.RETRO_ACHIEVEMENTS, "Progress"),
            details = if (d == null) listOf("No download") else listOf(raDownloadHeader(d), raDownloadSummary(d)),
            rows = rows, selectedIndex = selectedIndex, hints = raDownloadHints(d?.phase ?: RaDownloadPhase.DONE),
            onSelect = onSelect, onOpen = onOpen, onHint = onHint,
        )
        if (screen.confirmCancel) NxDialog(
            "Cancel download?", "Games already downloaded keep their data.",
            onConfirm = { onHint(NxButton.A) }, confirmLabel = "Stop", onCancel = { onHint(NxButton.B) },
        )
    }
}
