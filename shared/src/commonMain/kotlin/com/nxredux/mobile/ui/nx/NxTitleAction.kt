package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A page's action rows (Continue) at its title's right, where a landscape page has no height to spare under its
 * content: each a word on the pill while [selected] is its index (the list's own ink and pill), the title row's height.
 * [first] is the first action's index among the page's rows; a tap selects it and opens it.
 */
@Composable
internal fun NxTitleActions(
    labels: List<String>, first: Int, selected: Int, onSelect: (Int) -> Unit, onOpen: (Int) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalNxColors.current
    val label = NxTextSizes.listLabel(NxListPitch) * nxTextScale()   // Appearance → Text size
    Row(verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, text ->
            val index = first + i
            val lit = index == selected
            Box(
                Modifier.padding(start = 8.dp)
                    .then(if (lit) Modifier.background(nxHighlight(), RoundedCornerShape(50)) else Modifier.nxButtonRing(colors))
                    .clickable { onSelect(index); onOpen(index) }
                    .padding(horizontal = nxListInset(), vertical = 4.dp),
            ) {
                Text(
                    text, color = if (lit) colors.onAccent else colors.listText, fontFamily = LocalNxFont.current,
                    fontWeight = FontWeight.SemiBold, fontSize = label, maxLines = 1,
                )
            }
        }
        trailing?.invoke()
    }
}
