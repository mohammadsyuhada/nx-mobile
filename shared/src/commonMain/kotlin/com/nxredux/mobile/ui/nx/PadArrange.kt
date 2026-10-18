package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.toSize
import com.nxredux.mobile.settings.PadGroup
import com.nxredux.mobile.settings.PadLayout

/** Told where each group of a pad sits by default (root px), with its pad's bounds and unit: the editor's input. */
fun interface PadGroupBoundsListener {
    fun onGroup(group: PadGroup, defaultInRoot: Rect, padInRoot: Rect, unitPx: Float)
}

/**
 * One pad's arrangement. The pad's own Row / Column layout places every group ([defaults], pad px, measured by
 * [PadGroupBox]); a group with a tweak in [layout] is drawn moved and scaled from there ([placed]), inside [bounds].
 */
internal class PadArrange {
    var layout by mutableStateOf(PadLayout.EMPTY)
    var unitPx by mutableFloatStateOf(1f)
    var bounds by mutableStateOf(Rect.Zero)
    var listener: PadGroupBoundsListener? = null
    var root: LayoutCoordinates? = null
    val defaults = mutableStateMapOf<PadGroup, Rect>()

    /** Where [group] is drawn, pad px: its default, or [placeGroup] of it with its tweak; null before it is measured. */
    fun placed(group: PadGroup): Rect? {
        val d = defaults[group] ?: return null
        val t = layout.tweaks[group] ?: return d
        return placeGroup(d, t, unitPx, bounds)
    }
}

internal val LocalPadArrange = staticCompositionLocalOf<PadArrange?> { null }

/**
 * A pad's [PadArrange], kept across recompositions: built with [layout] / [unitPx] / [listener] so a new pad's first
 * frame already draws the layout, and updated to them after each recomposition. The pad's root box records its
 * coordinates and bounds (the pad's padded content box) with [padArrangeRoot].
 */
@Composable
internal fun rememberPadArrange(layout: PadLayout, unitPx: Float, listener: PadGroupBoundsListener?): PadArrange {
    val arrange = remember {
        PadArrange().apply {
            this.layout = layout
            this.unitPx = unitPx
            this.listener = listener
        }
    }
    SideEffect {
        arrange.layout = layout
        arrange.unitPx = unitPx
        arrange.listener = listener
    }
    return arrange
}

/** [onPlaced] for a pad's root box: its coordinates and bounds for [PadArrange]. */
internal fun Modifier.padArrangeRoot(arrange: PadArrange): Modifier = onPlaced { c ->
    arrange.root = c
    arrange.bounds = Rect(androidx.compose.ui.geometry.Offset.Zero, c.size.toSize())
}

/**
 * One button group. The outer box sits where the pad's own layout puts the group ([modifier] carries its padding
 * and alignment) and measures that as the default; the inner layer draws it moved and scaled per [PadArrange.placed].
 * Compose hit-tests through the layer, so touches follow the drawn group, and with no tweak there is no transform. A
 * group that leaves composition drops its default, so it no longer hides the NX mark or answers a hit test.
 */
@Composable
internal fun PadGroupBox(group: PadGroup, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val arrange = LocalPadArrange.current
    if (arrange == null) { Box(modifier) { content() }; return }
    DisposableEffect(arrange, group) { onDispose { arrange.defaults.remove(group) } }
    Box(
        modifier.onPlaced { c ->
            val root = arrange.root ?: return@onPlaced
            if (!root.isAttached || !c.isAttached) return@onPlaced
            val local = root.localBoundingBoxOf(c, clipBounds = false)
            arrange.defaults[group] = local
            arrange.listener?.let { l ->
                val window = root.findRootCoordinates()
                l.onGroup(group, window.localBoundingBoxOf(c, false), window.localBoundingBoxOf(root, false), arrange.unitPx)
            }
        },
    ) {
        Box(
            Modifier.graphicsLayer {
                val d = arrange.defaults[group] ?: return@graphicsLayer
                if (d.width <= 0f || d.height <= 0f) return@graphicsLayer   // not measured yet: no scale to divide by
                if (arrange.layout.tweaks[group] == null) return@graphicsLayer
                val p = arrange.placed(group) ?: return@graphicsLayer
                translationX = p.center.x - d.center.x
                translationY = p.center.y - d.center.y
                scaleX = p.width / d.width
                scaleY = p.height / d.height
            },
        ) { content() }
    }
}

/** Provides [arrange] to the pad's [PadGroupBox]es. */
@Composable
internal fun ProvidePadArrange(arrange: PadArrange, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalPadArrange provides arrange, content = content)
