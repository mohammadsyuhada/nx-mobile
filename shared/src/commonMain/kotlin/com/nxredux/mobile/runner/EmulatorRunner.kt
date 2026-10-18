package com.nxredux.mobile.runner

/** The only door to the native emulator. Platform modules implement it; shared code never sees what is behind it. */
interface EmulatorRunner {
    /**
     * Starts [session]. [onStarted] runs at most once, on the main thread, when the core has actually loaded the game;
     * never when the runner refuses it (a bad archive, a missing BIOS, a core load error) or the process dies first.
     */
    fun launch(session: GameSession, onStarted: () -> Unit)
}
