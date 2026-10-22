package com.nxredux.mobile.gametime

/**
 * One game's play time in the game panel's header: [todayMs] (its plays started today, plus the running slice's part
 * since midnight) and [totalMs] (its group's total, plus the running slice).
 */
data class PanelPlayTime(val todayMs: Long, val totalMs: Long) {
    companion object {
        /**
         * From the store: [total] is the game's group row ([GameTimeStore.byPath]; null: never played), [plays] its closed
         * plays ([GameTimeStore.sessions]), [dayStart] today's local 00:00. The open slice is never in the store's figures:
         * [sliceStartedAt] (null: none running) to [now] is added here.
         */
        fun of(total: GameTotal?, plays: List<GameSession>, dayStart: Long, sliceStartedAt: Long?, now: Long): PanelPlayTime {
            val running = sliceStartedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L
            val runningToday = sliceStartedAt?.let { (now - maxOf(it, dayStart)).coerceAtLeast(0L) } ?: 0L
            val today = plays.filter { it.startedAt >= dayStart }.sumOf { it.durationMs }
            return PanelPlayTime(today + runningToday, (total?.totalMs ?: 0L) + running)
        }
    }
}
