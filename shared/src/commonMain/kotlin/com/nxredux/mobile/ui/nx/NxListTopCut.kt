package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * A page's top-right icons ([NxTopIcons]) over a List that runs up under them (Page title Off): their box in root px,
 * measured by [nxListTopCut]. An [NxList] row level with them stops short of their left ([nxRowTopCutPx]), so the
 * list gains the title row's room without a row under the icons. NaN until measured.
 */
class NxListTopCut {
    var left by mutableFloatStateOf(Float.NaN)
    var bottom by mutableFloatStateOf(Float.NaN)
}

@Composable
fun rememberNxListTopCut(): NxListTopCut = remember { NxListTopCut() }

/** Records this element's (the icons') box as [cut]. */
fun Modifier.nxListTopCut(cut: NxListTopCut): Modifier = onGloballyPositioned { c ->
    val b = c.boundsInRoot()
    cut.left = b.left
    cut.bottom = b.bottom
}

/** The [NxListTopCut] the [NxList]s under it keep clear of; null (the default) for none. */
val LocalNxListTopCut = staticCompositionLocalOf<NxListTopCut?> { null }

/**
 * How far (px) a row whose top is at [rowTop] keeps from the list's right edge [listRight] to stay clear of icons
 * reaching down to [cutBottom] from [cutLeft]: their whole width for a row starting above their bottom, else 0.
 */
internal fun nxRowTopCutPx(rowTop: Float, cutLeft: Float, cutBottom: Float, listRight: Float): Float =
    if (cutLeft.isNaN() || cutBottom.isNaN() || rowTop >= cutBottom) 0f else maxOf(0f, listRight - cutLeft)

/** Row [index]'s top in root px: the list's [listTop], [pitch] px a row from the first visible one scrolled by [firstOffset]. */
internal fun nxRowTopPx(index: Int, listTop: Float, firstVisible: Int, firstOffset: Int, pitch: Int): Float =
    listTop + (index - firstVisible) * pitch - firstOffset
