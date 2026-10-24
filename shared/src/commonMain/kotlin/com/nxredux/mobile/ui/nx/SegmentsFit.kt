package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** The most of a touch row's width a segmented control may take on the row's right; wider, it goes under the text. */
internal const val SEGMENTS_RIGHT_SHARE = 0.4f

/**
 * Whether a segmented control of choices fits on the right of a touch row [rowWidth] wide: its natural width (each label
 * at 0.9 × [valueSize] with its 10 dp sides, the track's 2 dp inset and 1 dp border) at most [SEGMENTS_RIGHT_SHARE] of it.
 */
@Composable
internal fun rememberSegmentsFitRight(valueSize: TextUnit, rowWidth: Dp): (List<String>) -> Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val font = LocalNxFont.current
    return remember(valueSize, rowWidth, measurer, density, font) {
        val style = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = valueSize * 0.9f)
        val cache = HashMap<List<String>, Boolean>()
        val fits: (List<String>) -> Boolean = { choices ->
            cache.getOrPut(choices) {
                val text = with(density) { choices.sumOf { measurer.measure(it, style).size.width }.toDp() }
                text + 20.dp * choices.size + 6.dp <= rowWidth * SEGMENTS_RIGHT_SHARE
            }
        }
        fits
    }
}
