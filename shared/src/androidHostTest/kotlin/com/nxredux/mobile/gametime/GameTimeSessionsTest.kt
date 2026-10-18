package com.nxredux.mobile.gametime

import com.nxredux.mobile.gametime.db.GameTimeDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameTimeSessionsTest {
    private var clock = 10_000_000L
    private val pair = testStore({ clock })
    private val store = pair.first
    private val db: GameTimeDb = pair.second
    private var n = 0
    /** The same database seen from a second device (its sessions as a sync would bring them). */
    private val flip = GameTimeStore(db, "dev-2", "Flip", { clock }) { "f-${n++}" }.also { it.prepare() }

    private fun play(s: GameTimeStore, path: String, ms: Long, name: String = path.substringAfterLast('/').substringBeforeLast('.')): String {
        val id = s.start(path, "GB", name); clock += ms; s.end(id); clock += 1_000; return id
    }
    private fun idOf(name: String) = store.totals().first { it.name == name }.gameId

    @Test fun aGamesSessionsNewestFirstWithTheirDevice() {
        val first = play(store, "/Roms/GB/Tetris.gb", 60_000)
        play(store, "/Roms/GB/Kirby.gb", 30_000)
        val second = play(store, "/Roms/GB/Tetris.gb", 120_000)
        val s = store.sessions(idOf("Tetris"))
        assertEquals(listOf(second, first), s.sessions.map { it.uuid })
        assertEquals(listOf(120_000L, 60_000L), s.sessions.map { it.durationMs })
        assertEquals(listOf("Fold", "Fold"), s.sessions.map { it.deviceName })
        assertEquals(s.sessions[0].startedAt + 120_000L, s.sessions[0].endedAt)
        assertFalse(s.multiDevice)
    }

    @Test fun aMergedGroupListsEveryMembersSessions() {
        val a = play(store, "/Roms/GB/Tetris.gb", 60_000)
        val b = play(store, "/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        val c = play(store, "/Roms/GB/Tetris (Japan).gb", 20_000, name = "Tetris (Japan)")
        store.merge(idOf("Tetris (Europe)"), idOf("Tetris"), "Tetris")
        store.merge(idOf("Tetris"), idOf("Tetris (Japan)"), "Tetris")   // a group into a group: the members follow
        val target = store.totals().single().gameId
        assertEquals(listOf(c, b, a), store.sessions(target).sessions.map { it.uuid })
        assertEquals(store.totals().single().totalMs, store.sessions(target).sessions.sumOf { it.durationMs })
    }

    @Test fun anUnmergedGameTakesItsSessionsBack() {
        play(store, "/Roms/GB/Tetris.gb", 60_000)
        val eu = play(store, "/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        val euId = idOf("Tetris (Europe)"); val target = idOf("Tetris")
        store.merge(euId, target, "Tetris")
        store.unmerge(euId)
        assertEquals(1, store.sessions(target).sessions.size)
        assertEquals(listOf(eu), store.sessions(euId).sessions.map { it.uuid })
    }

    @Test fun sessionsFromTwoDevicesAreFlaggedAndNamed() {
        play(store, "/Roms/GB/Tetris.gb", 60_000)
        play(flip, "/Roms/GB/Tetris.gb", 90_000)
        val s = store.sessions(idOf("Tetris"))
        assertTrue(s.multiDevice)
        assertEquals(listOf("Flip", "Fold"), s.sessions.map { it.deviceName })
    }

    @Test fun anOpenSessionIsLeftOutAsTheTotalsDo() {
        play(store, "/Roms/GB/Tetris.gb", 60_000)
        store.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); clock += 60_000
        assertEquals(1, store.sessions(idOf("Tetris")).sessions.size)
    }

    @Test fun anUnknownGameHasNoSessions() {
        assertTrue(store.sessions(42L).sessions.isEmpty())
    }
}
