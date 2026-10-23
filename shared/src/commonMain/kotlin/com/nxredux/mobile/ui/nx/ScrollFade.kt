package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's "more below" cue for a vertical scroll: while [state] can scroll further down, the bottom [height] of what
 * this modifier wraps fades to transparent, so the cut-off line reads as continuing. Place it after the container's
 * background and before its `verticalScroll` (or on a lazy list), so only the scrolled content fades, not the surface.
 */
fun Modifier.nxScrollFade(state: ScrollableState, height: Dp = 32.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (!state.canScrollForward) return@drawWithContent
            val fade = height.toPx().coerceAtMost(size.height)
            drawRect(
                brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn,
            )
        }
