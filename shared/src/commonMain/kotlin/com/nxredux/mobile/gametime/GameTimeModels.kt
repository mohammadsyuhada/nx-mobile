package com.nxredux.mobile.gametime

/** One row of the tracker: a merge group (its target game) or, from [GameTimeStore.members], one game on its own. */
data class GameTotal(
    val gameId: Long,
    val name: String,
    val tag: String,
    val romPath: String,
    val totalMs: Long,
    val plays: Long,
    val lastPlayedAt: Long,
    /** Games merged into this one (0 for a plain game or a member row). */
    val members: Int = 0,
) {
    val averageMs: Long get() = if (plays == 0L) 0L else totalMs / plays
}

/**
 * One play of a game group ([GameTimeStore.sessions]): a launch, its closed slices (one per return from the background)
 * together. Where it ran, from the first slice's start to the last one's end, and the time played ([durationMs], the
 * slices' sum: time in the background left out).
 */
data class GameSession(
    /** The play's key: its first slice's uuid. */
    val uuid: String,
    val gameId: Long,
    val deviceId: String,
    val deviceName: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
)

/** A group's plays, newest first; [multiDevice]: they ran on more than one device (the rows then name theirs). */
data class GameSessions(val sessions: List<GameSession>) {
    val multiDevice: Boolean get() = sessions.map { it.deviceId }.distinct().size > 1
}

/**
 * Play time inside a time window, and the group played most in it (its [GameTotal] figures are the window's); null when
 * nothing was played. [tops]: the (up to) 3 groups played most, most first ([top] is the first).
 */
data class WindowTotals(val totalMs: Long, val top: GameTotal?, val tops: List<GameTotal> = listOfNotNull(top))
