package com.nxredux.mobile.runner

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import com.nxredux.mobile.settings.GameRumble

/**
 * Game rumble (Controls → Game rumble) on a vibrator: the controller in use when it has one ([useDevice]), else the
 * phone's. One port's latched strengths come in through [onRumble]; [RumbleSchedule] decides when the motor starts,
 * is re-issued and stops. Main thread only.
 *
 * Played as media (USAGE_MEDIA on API 33+, USAGE_GAME before), not as touch: turning touch feedback off must not
 * silence a game's rumble.
 */
@SuppressLint("MissingPermission")   // VIBRATE is in the app's manifest
internal class RumbleMotor(
    private val context: Context,
    private val level: GameRumble,
    /** False for players 2–4: their rumble goes to their own pad or nowhere, never the phone in P1's hands. */
    private val phoneFallback: Boolean = true,
) {
    private val schedule = RumbleSchedule()
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = Runnable { apply(schedule.tick(SystemClock.uptimeMillis())) }
    private var target: Vibrator? = null
    private var deviceId: Int? = null       // the controller driven; null: the phone
    private var amplitude = 0               // the core's latched amplitude, kept while [paused]
    private var paused = false
    private val phone: Vibrator? by lazy { if (phoneFallback) defaultVibrator(context)?.takeIf { it.hasVibrator() } else null }

    val enabled: Boolean get() = level != GameRumble.OFF

    /** The core's strengths for this motor's port (0..1 each) changed. */
    fun onRumble(weak: Float, strong: Float) {
        if (!enabled) return
        // A vibrator without amplitude control is on or off: any strength is full, so a change never re-issues it.
        amplitude = rumbleAmplitude(level, weak, strong)
            .let { if (it > 0 && (target ?: phone)?.hasAmplitudeControl() == false) 255 else it }
        if (!paused) apply(schedule.set(amplitude, SystemClock.uptimeMillis()))
    }

    /**
     * The input device the player last used: a controller with a vibrator takes the rumble, anything else (the
     * on-screen pad, a pad without one: [id] null) gives it to the phone when [phoneFallback], else silences it. A
     * change mid-rumble moves it over.
     */
    fun useDevice(id: Int?) {
        if (!enabled || id == deviceId) return
        deviceId = id
        val next = id?.let(::deviceVibrator) ?: phone
        if (next === target) return
        val running = schedule.stop() != null
        target?.cancel()
        target = next
        if (running && !paused) apply(schedule.set(amplitude, SystemClock.uptimeMillis()))
    }

    /** [id] went away: if it was the one driven, back to the phone when [phoneFallback], else silent. */
    fun deviceRemoved(id: Int) { if (id == deviceId) useDevice(null) }

    /** Stops the motor and keeps the core's state for [resume] (the in-game menu, the activity paused). */
    fun pause() {
        paused = true
        apply(schedule.stop())
    }

    /** Back to the core's state since [pause], if any. Only while the core runs. */
    fun resume() {
        if (!paused) return
        paused = false
        if (amplitude > 0) apply(schedule.set(amplitude, SystemClock.uptimeMillis()))
    }

    /**
     * Stops and forgets the core's state: a state load or reset may leave the core's last rumble latched without
     * reporting it again, and re-applying it would rumble with nothing to stop it.
     */
    fun forget() {
        amplitude = 0
        apply(schedule.stop())
    }

    /** Stops for good (quit, destroy). */
    fun release() {
        paused = true
        amplitude = 0
        apply(schedule.stop())
        handler.removeCallbacks(tickRunnable)
    }

    private fun apply(command: RumbleCommand?) {
        val vibrator = target ?: phone.also { target = it }
        when (command) {
            is RumbleCommand.Run -> vibrator?.let { play(it, command.amplitude, command.durationMs) }
            RumbleCommand.Stop -> vibrator?.cancel()
            null -> Unit
        }
        handler.removeCallbacks(tickRunnable)
        schedule.nextDeadline()?.let { handler.postAtTime(tickRunnable, it) }
    }

    private fun deviceVibrator(id: Int): Vibrator? {
        val device = InputDevice.getDevice(id) ?: return null
        if (device.isVirtual) return null
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device.vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            device.vibrator
        }
        return vibrator.takeIf { it.hasVibrator() }
    }
}

/** Settings → Controls → Game rumble changed: one short pulse at [level]'s floor on the phone (nothing for Off). */
@SuppressLint("MissingPermission")
internal fun playRumblePreview(context: Context, level: GameRumble) {
    val amplitude = rumblePreviewAmplitude(level)
    if (amplitude <= 0) return
    val vibrator = defaultVibrator(context)?.takeIf { it.hasVibrator() } ?: return
    vibrator.cancel()
    play(vibrator, amplitude, PREVIEW_MS)
}

private const val PREVIEW_MS = 140L

@SuppressLint("MissingPermission")
private fun play(vibrator: Vibrator, amplitude: Int, durationMs: Long) {
    val effect = VibrationEffect.createOneShot(
        durationMs,
        if (vibrator.hasAmplitudeControl()) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
    }
}

private fun defaultVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
