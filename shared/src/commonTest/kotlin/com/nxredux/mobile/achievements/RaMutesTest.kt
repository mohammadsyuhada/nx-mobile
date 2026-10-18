package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RaMutesTest {
    @Test fun toggle_persists_one_id_per_line() = runTest {
        val store = FakeRaStore(); val m = RaMutes(store); val h = "c".repeat(32)
        assertEquals(emptySet(), m.load(h))
        assertEquals(setOf(5), m.toggle(h, 5)); assertEquals(setOf(5, 7), m.toggle(h, 7)); assertEquals(setOf(7), m.toggle(h, 5))
        assertEquals("7\n", store.text(RaPaths.muted(h))); assertEquals(setOf(7), RaMutes(store).load(h))
    }
}
