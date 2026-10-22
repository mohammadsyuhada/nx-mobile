package com.nxredux.mobile.gametime

import kotlin.test.Test
import kotlin.test.assertEquals

class PanelPlayTimeTest {
    private val day = 1_000_000_000L          // today's local 00:00
    private val hour = 3_600_000L

    private fun play(start: Long, ms: Long) = GameSession("p$start", 1, "d", "Phone", start, start + ms, ms)
    private fun total(ms: Long) = GameTotal(1, "Game", "GBA", "x/Game.gba", ms, 3, day)

    @Test fun never_played_and_not_running_is_zero() =
        assertEquals(PanelPlayTime(0L, 0L), PanelPlayTime.of(null, emptyList(), day, sliceStartedAt = null, now = day + hour))

    @Test fun today_counts_plays_started_today_only() {
        val plays = listOf(play(day + 2 * hour, 20 * 60_000L), play(day - 2 * hour, hour), play(day + 10_000L, 60_000L))
        val t = PanelPlayTime.of(total(5 * hour), plays, day, sliceStartedAt = null, now = day + 5 * hour)
        assertEquals(21 * 60_000L, t.todayMs)
        assertEquals(5 * hour, t.totalMs)
    }

    @Test fun running_slice_adds_to_both() {
        val plays = listOf(play(day + hour, 10 * 60_000L))
        val t = PanelPlayTime.of(total(2 * hour), plays, day, sliceStartedAt = day + 3 * hour, now = day + 3 * hour + 5 * 60_000L)
        assertEquals(15 * 60_000L, t.todayMs)
        assertEquals(2 * hour + 5 * 60_000L, t.totalMs)
    }

    @Test fun running_slice_across_midnight_counts_today_from_midnight() {
        val t = PanelPlayTime.of(null, emptyList(), day, sliceStartedAt = day - 30 * 60_000L, now = day + 10 * 60_000L)
        assertEquals(10 * 60_000L, t.todayMs)
        assertEquals(40 * 60_000L, t.totalMs)
    }

    @Test fun clock_behind_the_slice_start_adds_nothing() =
        assertEquals(PanelPlayTime(0L, 0L), PanelPlayTime.of(null, emptyList(), day, sliceStartedAt = day + hour, now = day + 1_000L))
}
