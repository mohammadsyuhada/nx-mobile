package com.nxredux.mobile.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RaDownloadNotificationTextTest {
    private val status = RaStatus(user = "u", points = 0, pending = 0, lastSync = null, cachedGames = 0)

    @Test fun body_is_the_count_and_the_game_once_the_total_is_known() {
        assertEquals("3/40 · Sonic the Hedgehog", RaDownloadNotificationText.body(RaState.Prefetching(3, 40, "Sonic the Hedgehog", status)))
        assertEquals("3/40", RaDownloadNotificationText.body(RaState.Prefetching(3, 40, "", status)))
    }

    @Test fun body_is_starting_before_the_library_is_read() {
        assertEquals("Starting…", RaDownloadNotificationText.body(RaState.Prefetching(0, 0, "", status)))
    }

    @Test fun progress_is_done_over_total_clamped_and_none_while_unknown() {
        assertEquals(3 to 40, RaDownloadNotificationText.progressOf(RaState.Prefetching(3, 40, "x", status)))
        assertEquals(40 to 40, RaDownloadNotificationText.progressOf(RaState.Prefetching(41, 40, "x", status)))
        assertNull(RaDownloadNotificationText.progressOf(RaState.Prefetching(0, 0, "", status)))
    }
}
