package com.nxredux.mobile.runner

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One game screen at a time owns the runner's native session, which is process-wide: a game screen created while another
 * is alive asks that one to quit (its normal quit path) and must not start its emulator until every earlier screen has
 * finished its teardown, so the session is never shared, nor torn down under the new game.
 *
 * Every game screen [claim]s when it is created and [release]s at the very end of its teardown (after its native destroy).
 * Pure bookkeeping, not thread-safe: the host calls it from one thread (Android: the main thread).
 */
class SessionHandoff {
    /** A game screen's hold on the session, from its [claim] until its [release]. */
    class Token internal constructor(internal val onQuitRequested: () -> Unit) {
        internal val released = CompletableDeferred<Unit>()
        internal var asked = false
    }

    /** What [claim] handed out: this screen's [token], and the earlier holders it must wait for before starting. */
    class Claim internal constructor(val token: Token, private val earlier: List<Deferred<Unit>>) {
        /** Some earlier game screen has not finished its teardown yet. */
        val mustWait: Boolean get() = earlier.any { !it.isCompleted }

        /** Suspends until every earlier holder has released; returns at once when there were none. */
        suspend fun awaitPredecessors() { earlier.awaitAll() }

        /**
         * [awaitPredecessors] with a watchdog that never shortens the wait: [onSlow] once it has taken [SLOW_AFTER_MS],
         * [onStuck] once it has taken [STUCK_AFTER_MS] (each at most once, on the caller's dispatcher). Returns whether
         * [onStuck] fired, so the host can stay out of the way of the dialog it showed instead of starting the game.
         */
        suspend fun awaitWithWatchdog(onSlow: () -> Unit, onStuck: () -> Unit): Boolean {
            if (!mustWait) return false
            var stuck = false
            coroutineScope {
                val watchdog = launch {
                    delay(SLOW_AFTER_MS)
                    onSlow()
                    delay(STUCK_AFTER_MS - SLOW_AFTER_MS)
                    stuck = true
                    onStuck()
                }
                awaitPredecessors()
                watchdog.cancel()
            }
            return stuck
        }
    }

    companion object {
        /** A handoff this long is slow: the host tells the player it is closing the previous game. */
        const val SLOW_AFTER_MS = 3_000L
        /** A handoff this long is stuck: the host says the previous game has not closed and offers Back. */
        const val STUCK_AFTER_MS = 15_000L
    }

    private val live = mutableListOf<Token>()

    /** Holders that have not released yet (a waiting newcomer counts). */
    val liveCount: Int get() = live.size

    /**
     * Registers a new game screen. Every live holder not yet asked is asked to quit once, through its own
     * [Token.onQuitRequested] (called here, synchronously); [onQuitRequested] is this screen's, for a later newcomer.
     */
    fun claim(onQuitRequested: () -> Unit): Claim {
        val earlier = live.toList()
        val token = Token(onQuitRequested)
        live += token
        for (t in earlier) if (!t.asked) { t.asked = true; t.onQuitRequested() }
        return Claim(token, earlier.map { it.released })
    }

    /** Ends [token]'s hold: its teardown is complete. Idempotent. */
    fun release(token: Token) {
        live.remove(token)
        token.released.complete(Unit)
    }
}
