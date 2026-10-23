package com.nxredux.mobile.storage

/**
 * When [DataRoot]'s one-time move may run: never while a game session or Device Sync uses the data root ([begin] to
 * [end]), and one at a time ([tryStart] to [finish]). A background move asked for while the data is in use is
 * postponed: the last [end] says to start it.
 */
internal class MoveGate {
    private var users = 0
    private var running = false
    private var postponed = false

    /** A game session or a Device Sync run starts using the data root. */
    @Synchronized fun begin() { users++ }

    /** A use ended; true when it was the last and a move was postponed for it (the caller starts it now). */
    @Synchronized fun end(): Boolean {
        if (users > 0) users--
        if (users > 0 || running || !postponed) return false
        postponed = false
        return true
    }

    /**
     * True when a move may start now (it runs until [finish]). False while one runs, or while the data is in use: then,
     * with [postpone], the last [end] asks for it again.
     */
    @Synchronized fun tryStart(postpone: Boolean): Boolean {
        if (running) return false
        if (users > 0) {
            if (postpone) postponed = true
            return false
        }
        running = true
        return true
    }

    @Synchronized fun finish() { running = false }

    /** Nothing uses the data root now. */
    val idle: Boolean @Synchronized get() = users == 0
}
