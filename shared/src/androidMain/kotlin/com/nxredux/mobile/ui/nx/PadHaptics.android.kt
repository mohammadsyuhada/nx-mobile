package com.nxredux.mobile.ui.nx

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nxredux.mobile.settings.PadVibration

@Composable
internal actual fun rememberPadTick(level: PadVibration): () -> Unit {
    val context = LocalContext.current
    return remember(context, level) { padTick(context, level) }
}

/**
 * [level]'s tick on the default vibrator, its effect and attributes built here once. API 29+: the predefined TICK (Light),
 * CLICK (Medium) and HEAVY_CLICK (Strong), which the device tunes to its actuator; below, short one-shots. Played as a
 * touch vibration (USAGE_TOUCH on API 33+, sonification before), so the system's touch vibration intensity applies and
 * turning touch feedback off silences it.
 */
@SuppressLint("MissingPermission")   // VIBRATE is in the app's manifest
private fun padTick(context: Context, level: PadVibration): () -> Unit {
    if (level == PadVibration.OFF) return {}
    val vibrator = defaultVibrator(context)?.takeIf { it.hasVibrator() } ?: return {}
    val effect = padEffect(level, vibrator)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        ({ vibrator.vibrate(effect, attributes) })
    } else {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        ({ vibrator.vibrate(effect, attributes) })
    }
}

private fun defaultVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

private fun padEffect(level: PadVibration, vibrator: Vibrator): VibrationEffect =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(
            when (level) {
                PadVibration.MEDIUM -> VibrationEffect.EFFECT_CLICK
                PadVibration.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
                else -> VibrationEffect.EFFECT_TICK
            },
        )
    } else {
        val (ms, amplitude) = when (level) {
            PadVibration.MEDIUM -> 15L to 150
            PadVibration.STRONG -> 25L to 255
            else -> 8L to 80
        }
        VibrationEffect.createOneShot(ms, if (vibrator.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE)
    }
