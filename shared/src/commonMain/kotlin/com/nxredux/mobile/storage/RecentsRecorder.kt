package com.nxredux.mobile.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Adds a started game to recent.txt. Meant to live as long as the process: the runner reports a start from the game
 * screen, possibly after the menu (and its UI scope) is gone, and the write must still land. It holds only the store
 * and its own [scope], so a start callback that captures it retains nothing of the UI.
 */
class RecentsRecorder(private val userData: UserDataStore, private val scope: CoroutineScope) {
    /** Serialises every recent.txt read-modify-write of the process (each AppState takes it too). */
    val lock = Mutex()

    private val _written = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits after each [record] write, so a live main menu can bring in its Recently Played row. */
    val written: SharedFlow<Unit> = _written.asSharedFlow()

    fun record(recent: Recent) {
        scope.launch {
            lock.withLock { userData.writeRecents(Recents.add(userData.readRecents(), recent)) }
            _written.tryEmit(Unit)
        }
    }
}
