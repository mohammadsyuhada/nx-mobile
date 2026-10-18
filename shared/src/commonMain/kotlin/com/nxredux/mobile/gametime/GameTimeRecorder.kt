package com.nxredux.mobile.gametime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What GameActivity calls: fire-and-forget, never blocks or throws into the caller. [scope] must run one coroutine at
 * a time (Android: `Dispatchers.IO.limitedParallelism(1)`), so calls apply in order. A null [store] (DB unavailable)
 * records nothing.
 *
 * Time in the background is not play: [pause] (the activity stopped) closes the session there, [resume] (started again)
 * opens a new one on the same game, linked to the launch's first, so the slices count as one play. Each slice is a
 * closed session of its own, so a kill in the background loses nothing and a long absence never makes one implausible.
 */
class GameTimeRecorder(
    private val store: GameTimeStore?,
    private val scope: CoroutineScope,
    private val onError: (Throwable) -> Unit,
) {
    @kotlin.concurrent.Volatile private var current: String? = null
    /** The game [pause] left, for [resume]; set and read on [scope] only. */
    private var game: Triple<String, String, String>? = null
    /** The launch's first session uuid, which [resume]'s sessions link to; set and read on [scope] only. */
    private var play: String? = null
    val isOpen: Boolean get() = current != null

    fun start(romPath: String, tag: String, name: String) {
        scope.launch {
            val s = store ?: return@launch
            current?.let { runCatching { s.end(it) }.onFailure(onError) }
            game = Triple(romPath, tag, name)
            current = runCatching { s.start(romPath, tag, name) }.onFailure(onError).getOrNull()
            play = current
        }
    }

    /** Ends the open session now and keeps its game for [resume]. */
    fun pause() {
        scope.launch {
            val s = store ?: return@launch
            current?.let { runCatching { s.end(it) }.onFailure(onError) }
            current = null
        }
    }

    /** A new session on the game [pause] left; nothing when one is open or none was started (or it was ended). */
    fun resume() {
        scope.launch {
            val s = store ?: return@launch
            if (current != null) return@launch
            val (romPath, tag, name) = game ?: return@launch
            current = runCatching { s.start(romPath, tag, name, play) }.onFailure(onError).getOrNull()
            if (play == null) play = current   // the launch's first start failed: this slice heads it
        }
    }

    fun touch() {
        scope.launch { val s = store ?: return@launch; current?.let { runCatching { s.touch(it) }.onFailure(onError) } }
    }

    fun end() {
        scope.launch {
            val s = store ?: return@launch
            current?.let { runCatching { s.end(it) }.onFailure(onError) }
            current = null
            game = null
            play = null
        }
    }
}
