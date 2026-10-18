package com.nxredux.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nxredux.mobile.ui.nx.LocalNxColors
import com.nxredux.mobile.ui.nx.LocalNxFont
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.NxHintBar
import com.nxredux.mobile.ui.nx.NxModalCardColor
import com.nxredux.mobile.ui.nx.NxModalPanelColor
import com.nxredux.mobile.ui.nx.NxModalScrimColor
import com.nxredux.mobile.ui.nx.NxOptionsList
import com.nxredux.mobile.ui.nx.LocalNxOptionText
import com.nxredux.mobile.ui.nx.nxOptionsRowHeight
import com.nxredux.mobile.ui.nx.NxListPitch
import com.nxredux.mobile.ui.nx.NxTextSizes
import com.nxredux.mobile.ui.nx.nxOptionsFitHeight

/** The panel's widest, and its tallest share of the window. */
private val GAME_SETTINGS_MAX_WIDTH = 560.dp
private const val GAME_SETTINGS_MAX_HEIGHT = 0.85f

/**
 * A game's own Game Settings ([gameSettingsModal]) as a modal over the game list it came from: the modal frame's scrim,
 * panel and card, "Game Settings" with the game's name (and an open category) under it, the page's options rows
 * scrolling inside, and the page's hints at the panel's foot. At most 560 dp wide (the full width less 24 dp margins
 * on a narrow window) and 85% of the window tall; the same on every window. Rows act as on the page (headers and
 * notices take no tap); a tap on the scrim calls [onClose].
 */
@Composable
internal fun GameSettingsModal(
    screen: Screen.EmulatorOptions, selectedIndex: Int,
    onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onHint: (NxButton) -> Unit, onClose: () -> Unit,
) {
    val colors = LocalNxColors.current
    val font = LocalNxFont.current
    val listPitch = NxListPitch
    val rowHeight = nxOptionsRowHeight(listPitch, LocalNxOptionText.current)   // Appearance → Text size
    val rows = remember(screen.rows) { screen.rows.map(::emulatorNxRow) }   // not per d-pad move
    BoxWithConstraints(
        Modifier.fillMaxSize().background(NxModalScrimColor).pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).widthIn(max = GAME_SETTINGS_MAX_WIDTH).fillMaxWidth()
                .heightIn(max = maxHeight * GAME_SETTINGS_MAX_HEIGHT)
                .background(NxModalPanelColor, RoundedCornerShape(24.dp))
                .pointerInput(Unit) { detectTapGestures { } }   // taps inside stay inside
                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
        ) {
            Column(
                Modifier.weight(1f, fill = false).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NxModalCardColor)
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    "Game Settings", color = colors.listText, fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = NxTextSizes.modalText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp),
                )
                Text(
                    emulatorOptionsTitle(screen), color = colors.hint, fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = NxTextSizes.modalSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                NxOptionsList(
                    rows = rows, selectedIndex = selectedIndex,
                    onSelect = { i -> if (screen.rows.getOrNull(i)?.selectable == true) onSelect(i) },   // headers and notices take no tap
                    onOpen = { i -> if (screen.rows.getOrNull(i)?.selectable == true) onOpen(i) }, rowHeight = rowHeight, listText = NxTextSizes.listLabel(rowHeight),
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = nxOptionsFitHeight(rows, rowHeight).coerceAtLeast(rowHeight)),
                )
            }
            // Clear of the panel's 24 dp corner: the panel's own 8 dp side inset below it too.
            NxHintBar(emulatorOptionsHints(screen.rows.getOrNull(selectedIndex)), Modifier.padding(bottom = 8.dp), onHint = onHint, alpha = 0f)
        }
    }
}
