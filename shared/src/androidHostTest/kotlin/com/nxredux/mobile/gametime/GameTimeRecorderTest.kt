package com.nxredux.mobile.gametime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameTimeRecorderTest {
    private var clock = 5_000_000L
    private val store = testStore({ clock }).first

    @Test fun startTouchEndRecordsOneSession() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        assertTrue(rec.isOpen)
        clock += 30_000; rec.touch(); clock += 30_000; rec.end(); advanceUntilIdle()
        assertFalse(rec.isOpen)
        assertEquals(60_000L, store.totals().single().totalMs)
    }

    @Test fun callsRunInOrder() = runTest {
        var t = 0L
        val ticking = testStore({ t += 10_000; t }).first   // every clock read is 10 s later
        val rec = GameTimeRecorder(ticking, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); rec.end()   // queued back to back
        advanceUntilIdle()
        // end ran after start: one closed session. Had end run first, start's session would still be open (not counted).
        assertEquals(1, ticking.totals().size)
    }

    @Test fun touchAndEndWithoutStartDoNothing() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.touch(); rec.end(); advanceUntilIdle()
        assertTrue(store.totals().isEmpty())
    }

    @Test fun nullStoreRecordsNothing() = runTest {
        val rec = GameTimeRecorder(null, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); rec.end(); advanceUntilIdle()
        assertFalse(rec.isOpen)
    }

    @Test fun storeErrorsGoToOnErrorNotTheCaller() = runTest {
        val errors = mutableListOf<Throwable>()
        val broken = GameTimeStore(testStore({ clock }).second, "dev-1", "Fold", { clock }) { error("no ids") }
        val rec = GameTimeRecorder(broken, this, onError = { errors += it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); rec.touch(); rec.end(); advanceUntilIdle()
        assertEquals(1, errors.size)
        assertFalse(rec.isOpen)
    }

    @Test fun staleSessionFromAKilledProcessClosesAtLastTouch() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 60_000; rec.touch(); advanceUntilIdle()
        clock += 7_200_000               // process killed; a new process prepares the store
        store.prepare()
        assertEquals(60_000L, store.totals().single().totalMs)
    }
    @Test fun backgroundTimeIsNotPlay() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 600_000; rec.pause(); advanceUntilIdle()      // 10 min, then Home
        assertFalse(rec.isOpen)
        clock += 10_800_000; rec.resume(); advanceUntilIdle()   // back 3 h later
        assertTrue(rec.isOpen)
        clock += 300_000; rec.end(); advanceUntilIdle()         // 5 min more, quit
        assertEquals(900_000L, store.totals().single().totalMs)
    }

    @Test fun aDayInTheBackgroundKeepsThePlay() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 600_000; rec.pause(); advanceUntilIdle()
        clock += 90_000_000; rec.resume(); advanceUntilIdle()   // over 24 h away: no slice is implausible
        clock += 60_000; rec.end(); advanceUntilIdle()
        assertEquals(660_000L, store.totals().single().totalMs)
    }

    @Test fun shortSlicesAddUp() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 120_000; rec.pause(); advanceUntilIdle()
        clock += 30_000; rec.resume(); advanceUntilIdle()
        clock += 120_000; rec.end(); advanceUntilIdle()
        assertEquals(240_000L, store.totals().single().totalMs)
    }

    @Test fun killedInTheBackgroundKeepsPlayUpToThePause() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 90_000; rec.pause(); advanceUntilIdle()
        clock += 7_200_000               // process killed in the background; a new process prepares the store
        store.prepare()
        assertEquals(90_000L, store.totals().single().totalMs)
    }

    @Test fun resumeWithoutPauseAndPauseWithoutStartDoNothing() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.pause(); rec.resume(); advanceUntilIdle()
        assertFalse(rec.isOpen)
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 30_000; rec.resume(); advanceUntilIdle()   // already open: keeps the one session
        clock += 30_000; rec.end(); advanceUntilIdle()
        assertEquals(1L, store.totals().single().plays)
        assertEquals(60_000L, store.totals().single().totalMs)
    }

    @Test fun endWhilePausedForgetsTheGame() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 30_000; rec.pause(); rec.end(); advanceUntilIdle()
        clock += 30_000; rec.resume(); advanceUntilIdle()   // a stray resume after the quit opens nothing
        assertFalse(rec.isOpen)
        assertEquals(30_000L, store.totals().single().totalMs)
    }

    @Test fun startAfterAPauseIsANewGame() = runTest {
        val rec = GameTimeRecorder(store, this, onError = { throw it })
        rec.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); advanceUntilIdle()
        clock += 30_000; rec.pause(); advanceUntilIdle()
        rec.start("/Roms/GB/Kirby.gb", "GB", "Kirby"); advanceUntilIdle()
        clock += 20_000; rec.pause(); advanceUntilIdle()
        clock += 20_000; rec.resume(); advanceUntilIdle()   // resumes Kirby, not Tetris
        clock += 10_000; rec.end(); advanceUntilIdle()
        assertEquals(mapOf("Tetris" to 30_000L, "Kirby" to 30_000L), store.totals().associate { it.name to it.totalMs })
    }
}
