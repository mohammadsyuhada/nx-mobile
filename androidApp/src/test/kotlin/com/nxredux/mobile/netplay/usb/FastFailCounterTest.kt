package com.nxredux.mobile.netplay.usb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastFailCounterTest {
    @Test fun three_fast_failures_in_a_row_mean_gone() {
        val c = FastFailCounter(limit = 3, fastMs = 50)
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(-1, 1))
        assertTrue(c.gone(-1, 49))
    }

    @Test fun a_timeout_or_a_frame_starts_the_count_again() {
        val c = FastFailCounter(limit = 3, fastMs = 50)
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(-1, 200))   // waited out the timeout: a quiet cable
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(4, 0))      // a real frame
        assertFalse(c.gone(-1, 0))
        assertFalse(c.gone(-1, 0))
        assertTrue(c.gone(-1, 0))
    }

    @Test fun a_failure_at_the_limit_is_not_fast() {
        val c = FastFailCounter(limit = 3, fastMs = 50)
        repeat(5) { assertFalse(c.gone(-1, 50)) }
    }
}
