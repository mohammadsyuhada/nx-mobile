package com.nxredux.mobile.netplay.usb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbCableGenerationTest {
    @Test fun a_screen_with_the_current_token_stops_the_cable() {
        val g = UsbCableGeneration()
        g.bump()
        val token = g.current
        assertTrue(g.isCurrent(token))
    }

    @Test fun a_newer_start_leaves_an_older_screens_stop_a_no_op() {
        val g = UsbCableGeneration()
        g.bump()
        val old = g.current
        g.bump()   // the sheet started the cable again for a newer session
        assertFalse(g.isCurrent(old))
        assertTrue(g.isCurrent(g.current))
    }

    @Test fun an_early_return_start_still_counts() {
        val g = UsbCableGeneration()
        g.bump()
        val old = g.current
        g.bump()   // start() while the job runs: bumped before the early return
        assertFalse(g.isCurrent(old))
    }
}
