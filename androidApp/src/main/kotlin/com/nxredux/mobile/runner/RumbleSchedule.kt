package com.nxredux.mobile.runner

import com.nxredux.mobile.settings.GameRumble
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Game rumble's pure side (Controls → Game rumble): how a core's strength becomes a vibrator amplitude, and when the
 * motor is started, re-issued and stopped. [RumbleMotor] carries the commands out on a vibrator.
 */

/** [level]'s felt range as fractions of full amplitude: any nonzero strength lands between them. */
private fun feltRange(level: GameRumble): ClosedFloatingPointRange<Float>? = when (level) {
    GameRumble.OFF -> null
    GameRumble.LIGHT -> 0.45f..0.80f
    GameRumble.MEDIUM -> 0.60f..1.00f
    GameRumble.STRONG -> 0.80f..1.00f
}

/**
 * The amplitude (1..255, 0 = still) for a core's two motors at [level]: the stronger of [weak] and [strong] (0..1), any
 * nonzero one mapped into the level's felt range, so a faint rumble is still felt.
 */
internal fun rumbleAmplitude(level: GameRumble, weak: Float, strong: Float): Int {
    val range = feltRange(level) ?: return 0
    val s = maxOf(weak, strong).coerceAtMost(1f)
    if (s.isNaN() || s <= 0f) return 0
    val felt = range.start + s * (range.endInclusive - range.start)
    return (felt * 255f).roundToInt().coerceIn(1, 255)
}

/** The preview pulse's amplitude when the level is changed in Settings: the level's floor (0 for Off). */
internal fun rumblePreviewAmplitude(level: GameRumble): Int =
    feltRange(level)?.let { (it.start * 255f).roundToInt().coerceIn(1, 255) } ?: 0

internal sealed interface RumbleCommand {
    /** Vibrate at [amplitude] (1..255) for [durationMs], replacing whatever is playing. */
    data class Run(val amplitude: Int, val durationMs: Long) : RumbleCommand
    data object Stop : RumbleCommand
}

/**
 * When to drive the motor, given the core's latched amplitude (it reports a change only). A nonzero amplitude starts a
 * [RUN_MS] one-shot, re-issued [REISSUE_MARGIN_MS] before it runs out while the rumble lasts; a change of less than
 * [MIN_STEP] from the issued amplitude is not re-issued (each re-issue restarts the motor's ramp). A zero stops it only
 * after [OFF_DEBOUNCE_MS] (3 frames), so a core pulsing on and off frame by frame does not chatter. Times are any
 * monotonic milliseconds; the caller runs [tick] at [nextDeadline].
 */
internal class RumbleSchedule {
    private var issued = 0          // the amplitude playing; 0 = idle
    private var issuedAt = 0L
    private var offAt: Long? = null

    fun set(amplitude: Int, now: Long): RumbleCommand? {
        if (amplitude <= 0) {
            if (issued > 0 && offAt == null) offAt = now + OFF_DEBOUNCE_MS
            return null
        }
        offAt = null
        if (issued > 0 && abs(amplitude - issued) < MIN_STEP) return null
        return start(amplitude, now)
    }

    /** The pending off-debounce or re-issue, if due at [now]. */
    fun tick(now: Long): RumbleCommand? {
        offAt?.let { if (now >= it) return stop() }
        if (issued > 0 && now >= issuedAt + RUN_MS - REISSUE_MARGIN_MS) return start(issued, now)
        return null
    }

    /** When [tick] has something to do next; null while idle. */
    fun nextDeadline(): Long? {
        if (issued <= 0) return null
        val reissue = issuedAt + RUN_MS - REISSUE_MARGIN_MS
        return offAt?.let { minOf(it, reissue) } ?: reissue
    }

    /** Stops at once (menu, pause, background); null when already idle. */
    fun stop(): RumbleCommand? {
        offAt = null
        if (issued <= 0) return null
        issued = 0
        return RumbleCommand.Stop
    }

    private fun start(amplitude: Int, now: Long): RumbleCommand {
        issued = amplitude
        issuedAt = now
        return RumbleCommand.Run(amplitude, RUN_MS)
    }

    companion object {
        const val RUN_MS = 5_000L
        const val REISSUE_MARGIN_MS = 500L
        const val OFF_DEBOUNCE_MS = 50L
        const val MIN_STEP = 5
    }
}
