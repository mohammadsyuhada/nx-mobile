package com.nxredux.mobile.gametime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GameTimeByPathTest {
    private var clock = 10_000_000L
    private val store = testStore({ clock }).first

    private fun play(path: String, ms: Long, name: String = path.substringAfterLast('/').substringBeforeLast('.'), tag: String = "GBA"): String {
        val id = store.start(path, tag, name); clock += ms; store.end(id); clock += 1_000; return id
    }
    private fun idOf(name: String) = store.totals().first { it.name == name }.gameId

    @Test fun byPathMapsEveryMemberOfAMergedGroupToTheGroupTotal() = runTest {
        play("/Roms/Game Boy Advance (GBA)/A.gba", 60_000); play("/Roms/Game Boy Advance (GBA)/B.gba", 30_000)
        store.merge(from = idOf("B"), into = idOf("A"), name = "A")
        val data = StoreGameTimeData(store, UnconfinedTestDispatcher(testScheduler))
        val byPath = data.byPath()
        assertEquals(90_000L, byPath.getValue("/Roms/Game Boy Advance (GBA)/A.gba").totalMs)
        assertEquals(byPath.getValue("/Roms/Game Boy Advance (GBA)/A.gba"), byPath.getValue("/Roms/Game Boy Advance (GBA)/B.gba"))
    }
}
