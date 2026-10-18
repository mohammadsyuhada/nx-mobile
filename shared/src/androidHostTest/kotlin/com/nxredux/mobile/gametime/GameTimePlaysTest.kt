package com.nxredux.mobile.gametime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A launch's foreground slices (one per return from the background) count as one play everywhere plays are shown. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameTimePlaysTest {
    private var clock = 5_000_000L
    private val pair = testStore({ clock })
    private val store = pair.first
    private val q = pair.second.gameTimeQueries

    /** 10 min, Home for 3 h, 5 min more, quit: one play, not two. */
    private fun TestScope.playWithABreak(rec: GameTimeRecorder, path: String = "/Roms/GB/Tetris.gb", name: String = "Tetris") {
        val start = clock
        rec.start(path, "GB", name); advanceUntilIdle()
        clock += 600_000; rec.pause(); advanceUntilIdle()
        clock += 10_800_000; rec.resume(); advanceUntilIdle()
        clock += 300_000; rec.end(); advanceUntilIdle()
        clock += 1_000
        check(clock - start == 11_701_000L)
    }

    @Test fun aReturnFromTheBackgroundIsTheSamePlay() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        val first = clock
        playWithABreak(rec)
        val t = store.totals().single()
        assertEquals(900_000L, t.totalMs)
        assertEquals(1L, t.plays)
        assertEquals(900_000L, t.averageMs)
        assertEquals(1L, store.members(t.gameId).single().plays)
        assertEquals(1L, store.window(first - 1, clock).top!!.plays)
        // The sessions list: one row for the play, from its first start to its last end, with the time actually played.
        val s = store.sessions(t.gameId).sessions.single()
        assertEquals(first, s.startedAt)
        assertEquals(first + 600_000 + 10_800_000 + 300_000, s.endedAt)
        assertEquals(900_000L, s.durationMs)
    }

    @Test fun twoLaunchesAreTwoPlays() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        playWithABreak(rec); playWithABreak(rec)
        val t = store.totals().single()
        assertEquals(2L, t.plays)
        assertEquals(900_000L, t.averageMs)
        assertEquals(listOf(900_000L, 900_000L), store.sessions(t.gameId).sessions.map { it.durationMs })
    }

    @Test fun everySliceLinksToTheFirstOfItsLaunch() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        playWithABreak(rec)
        val first = q.sessionByUuid("s-0").executeAsOne()
        assertEquals("s-0", first.play_uuid)
        assertEquals("s-0", q.sessionByUuid("s-1").executeAsOne().play_uuid)
    }

    @Test fun aDroppedFirstSliceStillHoldsThePlayTogether() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 500; rec.pause(); advanceUntilIdle()            // under 1 s: the first slice is deleted
        clock += 60_000; rec.resume(); advanceUntilIdle()
        clock += 60_000; rec.pause(); advanceUntilIdle()
        clock += 60_000; rec.resume(); advanceUntilIdle()
        clock += 60_000; rec.end(); advanceUntilIdle()
        val t = store.totals().single()
        assertEquals(1L, t.plays)
        assertEquals(120_000L, t.totalMs)
        assertEquals(1, store.sessions(t.gameId).sessions.size)
    }

    @Test fun aMergedGroupCountsEachMembersPlaysOnce() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        playWithABreak(rec, "/Roms/GB/Tetris.gb", "Tetris")
        playWithABreak(rec, "/Roms/GB/Tetris (Europe).gb", "Tetris (Europe)")
        val ids = store.totals().associate { it.name to it.gameId }
        store.merge(ids.getValue("Tetris (Europe)"), ids.getValue("Tetris"), "Tetris")
        val t = store.totals().single()
        assertEquals(2L, t.plays)
        assertEquals(1_800_000L, t.totalMs)
        assertEquals(listOf(1L, 1L), store.members(t.gameId).map { it.plays })
        assertEquals(2, store.sessions(t.gameId).sessions.size)
    }

    @Test fun byPathCountsPlays() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        playWithABreak(rec)
        val data = StoreGameTimeData(store, UnconfinedTestDispatcher(testScheduler))
        assertEquals(1L, data.byPath().getValue("/Roms/GB/Tetris.gb").plays)
    }
}
