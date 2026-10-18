package com.nxredux.mobile.gametime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTimeStoreQueryTest {
    private var clock = 10_000_000L
    private val store = testStore({ clock }).first

    private fun play(path: String, ms: Long, name: String = path.substringAfterLast('/').substringBeforeLast('.'), tag: String = "GB"): String {
        val id = store.start(path, tag, name); clock += ms; store.end(id); clock += 1_000; return id
    }
    private fun idOf(name: String) = store.totals().first { it.name == name }.gameId

    @Test fun totalsSumPlaysAverageAndSortByTotal() {
        play("/Roms/GB/Tetris.gb", 60_000); play("/Roms/GB/Tetris.gb", 120_000); play("/Roms/GB/Kirby.gb", 600_000)
        val t = store.totals()
        assertEquals(listOf("Kirby", "Tetris"), t.map { it.name })
        val tetris = t[1]
        assertEquals(180_000L, tetris.totalMs); assertEquals(2L, tetris.plays); assertEquals(90_000L, tetris.averageMs)
        assertEquals(780_000L, store.grandTotal())
    }

    @Test fun openSessionsAreNotCounted() {
        play("/Roms/GB/Tetris.gb", 60_000)
        store.start("/Roms/GB/Tetris.gb", "GB", "Tetris"); clock += 60_000
        assertEquals(60_000L, store.totals().single().totalMs)
    }

    @Test fun mergeSumsKeepsChosenNameAndLeavesSessionsAlone() {
        play("/Roms/GB/Tetris.gb", 60_000); play("/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        store.merge(idOf("Tetris"), idOf("Tetris (Europe)"), "Tetris")
        val row = store.totals().single()
        assertEquals("Tetris", row.name); assertEquals(90_000L, row.totalMs); assertEquals(2L, row.plays); assertEquals(1, row.members)
    }

    @Test fun laterPlayOfMergedAwayRomCountsTowardTheGroup() {
        play("/Roms/GB/Tetris.gb", 60_000); play("/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        store.merge(idOf("Tetris (Europe)"), idOf("Tetris"), "Tetris")
        play("/Roms/GB/Tetris (Europe).gb", 15_000, name = "Tetris (Europe)")
        assertEquals(105_000L, store.totals().single().totalMs)
    }

    @Test fun mergeGroupIntoGroupRepointsMembers() {
        play("/Roms/GB/A.gb", 10_000); play("/Roms/GB/B.gb", 20_000); play("/Roms/GB/C.gb", 40_000)
        store.merge(idOf("A"), idOf("B"), "B")
        val b = idOf("B"); val c = idOf("C")
        store.merge(b, c, "C")
        val row = store.totals().single()
        assertEquals(70_000L, row.totalMs); assertEquals(2, row.members)
        assertEquals(listOf("C", "B", "A"), store.members(c).map { it.name })
    }

    @Test fun mergeRefusesNonTargetsAndSelf() {
        play("/Roms/GB/A.gb", 10_000); play("/Roms/GB/B.gb", 20_000); play("/Roms/GB/C.gb", 40_000)
        val a = idOf("A"); val b = idOf("B")
        store.merge(a, b, "B")
        assertFailsWith<IllegalArgumentException> { store.merge(a, idOf("C"), "C") }
        assertFailsWith<IllegalArgumentException> { store.merge(b, b, "B") }
    }

    @Test fun unmergeRestoresOwnTotalAndClearsEmptiedTitle() {
        play("/Roms/GB/Tetris.gb", 60_000); play("/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        val eu = idOf("Tetris (Europe)"); val us = idOf("Tetris")
        store.merge(us, eu, "Tetris")   // target eu shown as "Tetris"
        store.unmerge(us)
        val names = store.totals().map { it.name to it.totalMs }
        assertEquals(listOf("Tetris" to 60_000L, "Tetris (Europe)" to 30_000L), names)
    }

    @Test fun deleteRemovesTheWholeGroup() {
        play("/Roms/GB/A.gb", 10_000); play("/Roms/GB/B.gb", 20_000); play("/Roms/GB/C.gb", 40_000)
        store.merge(idOf("A"), idOf("B"), "B")
        store.delete(idOf("B"))
        assertEquals(listOf("C"), store.totals().map { it.name })
        play("/Roms/GB/A.gb", 5_000)   // a deleted ROM starts fresh
        assertEquals(5_000L, store.totals().single { it.romPath == "/Roms/GB/A.gb" }.totalMs)
    }

    @Test fun deleteOfAGroupWithMembersLeavesNoRows() {
        var t = 10_000_000L
        val (s, db) = testStore({ t })
        val q = db.gameTimeQueries
        fun run(path: String): String { val id = s.start(path, "GB", path.substringAfterLast('/')); t += 10_000; s.end(id); t += 1_000; return id }
        val uuids = listOf(run("/Roms/GB/A.gb"), run("/Roms/GB/B.gb"), run("/Roms/GB/C.gb"))
        val ids = listOf("/Roms/GB/A.gb", "/Roms/GB/B.gb", "/Roms/GB/C.gb").map { q.gameByPath(it).executeAsOne().id }
        s.merge(ids[0], ids[2], "C"); s.merge(ids[1], ids[2], "C")
        s.delete(ids[2])   // target first in the group: its members' game_merge rows still point at it
        uuids.forEach { assertNull(q.sessionByUuid(it).executeAsOneOrNull()) }
        ids.forEach { assertNull(q.gameById(it).executeAsOneOrNull()); assertNull(q.mergeTarget(it).executeAsOneOrNull()) }
        assertTrue(q.memberIds(ids[2]).executeAsList().isEmpty())
    }

    @Test fun mergeCandidatesPutSameBaseNameFirst() {
        play("/Roms/GB/Tetris (USA).gb", 10_000, name = "Tetris (USA)")
        play("/Roms/GB/Kirby.gb", 90_000)
        play("/Roms/GB/Tetris (Europe) (Rev 1).gb", 20_000, name = "Tetris (Europe) (Rev 1)")
        val c = store.mergeCandidates(idOf("Tetris (USA)")).map { it.name }
        assertEquals(listOf("Tetris (Europe) (Rev 1)", "Kirby"), c)
    }

    @Test fun aLaterPlayRefreshesTheGamesNameAndTag() {
        play("/Roms/GB/Tetris.gb", 60_000, name = "Tetris", tag = "GB")
        play("/Roms/GB/Tetris.gb", 30_000, name = "Tetris DX", tag = "GBC")   // renamed, run under another emulator
        val row = store.totals().single()
        assertEquals("Tetris DX", row.name); assertEquals("GBC", row.tag); assertEquals(90_000L, row.totalMs)
    }

    @Test fun aRenamedTargetKeepsItsGroupsChosenTitle() {
        play("/Roms/GB/Tetris.gb", 60_000); play("/Roms/GB/Tetris (Europe).gb", 30_000, name = "Tetris (Europe)")
        val eu = idOf("Tetris (Europe)")
        store.merge(eu, idOf("Tetris"), "Tetris (Europe)")
        play("/Roms/GB/Tetris.gb", 10_000, name = "Tetris (USA)")   // the target's own ROM renamed
        assertEquals("Tetris (Europe)", store.totals().single().name)
        store.unmerge(eu)   // no members left: the title goes, the refreshed name shows
        assertEquals(setOf("Tetris (USA)", "Tetris (Europe)"), store.totals().map { it.name }.toSet())
    }
}
