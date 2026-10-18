package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.nxredux.mobile.settings.PadVibration

/*
 * The on-screen pad's press tick (Controls → Pad vibration). Only the pad's own touch handlers call it, after their
 * Down went out, so a controller's or the in-game menu's input never vibrates and a press never waits on the vibrator.
 */

/** The platform tick at [level]: cheap to call (its effect built once), a no-op for [PadVibration.OFF] or no vibrator. */
@Composable
internal expect fun rememberPadTick(level: PadVibration): () -> Unit

/** The pad vibration in effect; [PadVibration.DEFAULT] unless a [PadVibrationProvider] says otherwise. */
internal val LocalPadVibration = staticCompositionLocalOf { PadVibration.DEFAULT }

/** Gives the pads in [content] (portrait and landscape) the user's [level] (Controls → Pad vibration). */
@Composable
fun PadVibrationProvider(level: PadVibration, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPadVibration provides level, content = content)
}

/** The tick for the pad in effect, stable across recompositions: a gesture that outlives one calls the latest level's. */
@Composable
internal fun rememberPadHaptic(): () -> Unit {
    val tick by rememberUpdatedState(rememberPadTick(LocalPadVibration.current))
    return remember { { tick() } }
}
