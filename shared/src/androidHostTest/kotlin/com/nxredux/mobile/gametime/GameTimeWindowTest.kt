package com.nxredux.mobile.gametime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameTimeWindowTest {
    private val day = 24 * 3600_000L
    private val hour = 3600_000L
    private val now = 30 * day
    private var clock = 0L
    private val store = testStore({ clock }).first

    private fun name(path: String) = path.substringAfterLast('/').substringBeforeLast('.')

    /** One closed session on [path] from [start] to [end], stamped through the store's clock. */
    private fun GameTimeStore.recordClosed(path: String, start: Long, end: Long) {
        clock = start
        val id = start(path, "GBA", name(path))
        clock = end
        end(id)
    }

    /** One session on [path] opened at [start] and last touched at [lastSeen], never ended. */
    private fun GameTimeStore.recordOpen(path: String, start: Long, lastSeen: Long) {
        clock = start
        val id = start(path, "GBA", name(path))
        clock = lastSeen
        touch(id)
    }

    private fun idOf(path: String) = store.totals().first { it.romPath == path }.gameId

    @Test fun sumsOnlyTheOverlapWithTheWindow() {
        val s = store
        s.recordClosed("roms/a.gba", start = now - 8 * day, end = now - 8 * day + hour) // before the window
        s.recordClosed("roms/a.gba", start = now - 7 * day - 1800_000, end = now - 7 * day + 1800_000) // half inside
        s.recordClosed("roms/b.gba", start = now - day, end = now - day + 7200_000)
        val w = s.window(now - 7 * day, now)
        assertEquals(1800_000L + 7200_000L, w.totalMs)
        assertEquals("roms/b.gba", w.top?.romPath)
        assertEquals(7200_000L, w.top?.totalMs)
        assertEquals("b", w.top?.name)
    }

    @Test fun mergedGamesCountAsTheirTarget() {
        val s = store
        s.recordClosed("roms/a.gba", start = now - 3 * day, end = now - 3 * day + 2 * hour)
        s.recordClosed("roms/b.gba", start = now - 2 * day, end = now - 2 * day + 2 * hour)
        s.recordClosed("roms/c.gba", start = now - day, end = now - day + 3 * hour)
        s.merge(from = idOf("roms/b.gba"), into = idOf("roms/a.gba"), name = "A Merged")
        val w = s.window(now - 7 * day, now)
        assertEquals(7 * hour, w.totalMs)
        assertEquals("roms/a.gba", w.top?.romPath)
        assertEquals("A Merged", w.top?.name)
        assertEquals(4 * hour, w.top?.totalMs)
        assertEquals(1, w.top?.members)
    }

    @Test fun anOpenSessionCountsUpToItsLastSeen() {
        val s = store
        s.recordOpen("roms/a.gba", start = now - 2 * hour, lastSeen = now - 1800_000)
        // Counted up to its last touch, not up to now.
        val w = s.window(now - 7 * day, now)
        assertEquals(1800_000L * 3, w.totalMs)
        assertEquals("roms/a.gba", w.top?.romPath)
        // A last touch after the window's end is clamped to the window's end.
        val earlier = s.window(now - 7 * day, now - hour)
        assertEquals(hour, earlier.totalMs)
    }

    @Test fun nothingInTheWindowGivesZeroAndNoTop() {
        val s = store
        assertEquals(WindowTotals(0L, null), s.window(now - 7 * day, now))
        s.recordClosed("roms/a.gba", start = now - 10 * day, end = now - 10 * day + hour)
        val w = s.window(now - 7 * day, now)
        assertEquals(0L, w.totalMs)
        assertNull(w.top)
    }

    @Test fun aTieGoesToTheMostRecentlyPlayed() {
        val s = store
        // The older game is recorded first, so it has the lower id: id order and recency disagree.
        s.recordClosed("roms/old.gba", start = now - 3 * day, end = now - 3 * day + hour)
        s.recordClosed("roms/new.gba", start = now - 2 * day, end = now - 2 * day + hour)
        assertEquals("roms/new.gba", s.window(now - 7 * day, now).top?.romPath)
    }

    @Test fun aSessionCoveringTheWholeWindowCountsTheWindow() {
        val s = store
        s.recordClosed("roms/a.gba", start = now - 5 * hour, end = now + hour)
        val w = s.window(now - 3 * hour, now - hour)
        assertEquals(2 * hour, w.totalMs)
        assertEquals(2 * hour, w.top?.totalMs)
    }

    @Test fun anOpenSessionCountsAtMostTheCloseBound() {
        val s = store
        val start = now - 3 * day
        s.recordOpen("roms/a.gba", start = start, lastSeen = start + 2 * day)
        val w = s.window(now - 7 * day, now)
        assertEquals(GameTimeStore.MAX_MS, w.totalMs)
        assertEquals(start + GameTimeStore.MAX_MS, w.top?.lastPlayedAt)
    }

    @Test fun playedPathsAreEveryMemberOfEachGroupWithOverlap() {
        val s = store
        s.recordClosed("roms/a.gba", start = now - 3 * day, end = now - 3 * day + hour)           // inside
        s.recordClosed("roms/b.gba", start = now - 20 * day, end = now - 20 * day + hour)         // outside, merged into a
        s.recordClosed("roms/c.gba", start = now - 10 * day, end = now - 10 * day + hour)         // outside, alone
        s.recordClosed("roms/d.gba", start = now - 7 * day - 1800_000, end = now - 7 * day + 60_000)   // overlaps the start
        s.recordOpen("roms/e.gba", start = now - hour, lastSeen = now - 1800_000)                 // open, inside
        s.merge(from = idOf("roms/b.gba"), into = idOf("roms/a.gba"), name = "A")
        assertEquals(setOf("roms/a.gba", "roms/b.gba", "roms/d.gba", "roms/e.gba"), s.playedPaths(now - 7 * day, now))
        assertEquals(emptySet(), s.playedPaths(now - hour / 2, now - hour / 2 + 1))   // e's last touch is before this window
    }

    @Test fun gameTimeDataPassesPlayedPathsThrough() = runTest {
        store.recordClosed("roms/a.gba", start = now - day, end = now - day + hour)
        val data = StoreGameTimeData(store, UnconfinedTestDispatcher(testScheduler))
        assertEquals(setOf("roms/a.gba"), data.playedPaths(now - 7 * day, now))
    }

    @Test fun gameTimeDataPassesTheWindowThrough() = runTest {
        store.recordClosed("roms/a.gba", start = now - day, end = now - day + hour)
        val data = StoreGameTimeData(store, UnconfinedTestDispatcher(testScheduler))
        val w = data.window(now - 7 * day, now)
        assertEquals(hour, w.totalMs)
        assertEquals("roms/a.gba", w.top?.romPath)
    }
    // The window lists its top 3 games: most time first, a tie to the most recent play; fewer when
    // fewer were played. [WindowTotals.top] is the first of them.
    @Test fun theTopThreeGamesByTimeInTheWindow() {
        val s = store
        s.recordClosed("roms/a.gba", start = now - 5 * day, end = now - 5 * day + 3 * hour)
        s.recordClosed("roms/b.gba", start = now - 4 * day, end = now - 4 * day + hour)
        s.recordClosed("roms/c.gba", start = now - 3 * day, end = now - 3 * day + 2 * hour)
        s.recordClosed("roms/d.gba", start = now - 2 * day, end = now - 2 * day + hour)          // ties b, played later
        s.recordClosed("roms/e.gba", start = now - 20 * day, end = now - 20 * day + 9 * hour)    // before the window
        val w = s.window(now - 7 * day, now)
        assertEquals(listOf("roms/a.gba", "roms/c.gba", "roms/d.gba"), w.tops.map { it.romPath })
        assertEquals(listOf(3 * hour, 2 * hour, hour), w.tops.map { it.totalMs })
        assertEquals(w.tops.first(), w.top)
        assertEquals(listOf("roms/d.gba"), s.window(now - 2 * day - hour, now).tops.map { it.romPath })   // one played
        assertEquals(emptyList(), s.window(now - hour, now).tops)
    }
}
