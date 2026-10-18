package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationQueueTest {
    @Test fun keeps_at_most_four_newest_and_expires_by_time() {
        val q = NotificationQueue(max = 4)
        for (i in 1..5) q.push("t$i", null, durationMs = 3_000, now = 1_000L + i)
        assertEquals(listOf("t2", "t3", "t4", "t5"), q.toasts.map { it.text })
        assertTrue(!q.tick(2_000L)); assertTrue(q.tick(4_003L))
        assertEquals(listOf("t4", "t5"), q.toasts.map { it.text })
    }
    @Test fun progress_pill_updates_in_place_and_hides() {
        val q = NotificationQueue()
        q.progress("A", "1/3", "b", 1_000, now = 0L); q.progress("A", "2/3", "b", 1_000, now = 500L)
        assertEquals("2/3", q.pill!!.value); assertEquals(1_500L, q.pill!!.until)
        assertTrue(q.tick(1_500L)); assertNull(q.pill)
        q.progress("A", "3/3", null, 1_000, now = 2_000L); q.hideProgress(); assertNull(q.pill)
    }
    @Test fun zero_duration_shows_nothing() {
        val q = NotificationQueue()
        q.progress("A", "1", null, 0, now = 0L); assertNull(q.pill)
        q.push("x", null, 0, now = 0L); assertTrue(q.toasts.isEmpty())
    }
}
