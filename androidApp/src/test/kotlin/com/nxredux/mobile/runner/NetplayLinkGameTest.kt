package com.nxredux.mobile.runner

import com.nxredux.mobile.netplay.NetplayText
import com.swordfish.libretrodroid.LibretroNetplay
import org.junit.Assert.assertEquals
import org.junit.Test

class NetplayLinkGameTest {
    private fun notice(state: Int, seconds: Int = 0, reason: Int = LibretroNetplay.REASON_NONE, connectingMs: Long = 0) =
        linkNotice(state, seconds, reason, connectingMs)

    @Test fun a_link_that_runs_or_waits_shows_nothing() {
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.LINK_LINKED))
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.LINK_CONNECTING, connectingMs = NETPLAY_CONNECT_TIMEOUT_MS - 1))
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.LINK_IDLE))
    }

    @Test fun a_host_without_its_joiner_ends_after_45_s() {
        // The game already runs: it plays on alone, no failure dialog over it.
        assertEquals(NetplayNotice.Ended("${NetplayText.ENDED}\n${NetplayText.ENDED_DETAIL}"),
            notice(LibretroNetplay.LINK_CONNECTING, connectingMs = NETPLAY_CONNECT_TIMEOUT_MS))
    }

    @Test fun the_45_s_wait_counts_from_the_latest_entry_into_connecting() {
        val clock = LinkConnectingClock(startedAt = 1_000)
        assertEquals(500L, clock.connectingMs(LibretroNetplay.LINK_CONNECTING, 1_500))
        assertEquals(40_000L, clock.connectingMs(LibretroNetplay.LINK_CONNECTING, 41_000))
        clock.connectingMs(LibretroNetplay.LINK_LINKED, 42_000)
        // The handheld joiner reloads for the host's mode: the host waits again, a full 45 s from now.
        assertEquals(0L, clock.connectingMs(LibretroNetplay.LINK_CONNECTING, 43_000))
        assertEquals(30_000L, clock.connectingMs(LibretroNetplay.LINK_CONNECTING, 73_000))
    }

    @Test fun the_peers_pause_is_the_pill() {
        assertEquals(NetplayNotice.Pill(NetplayText.peerPaused(17)), notice(LibretroNetplay.LINK_PEER_PAUSED, 17))
    }

    @Test fun ended_never_says_progress_is_lost() {
        // Each player plays on their own real save: progress is always kept.
        assertEquals(NetplayNotice.Ended("${NetplayText.ENDED}\n${NetplayText.ENDED_DETAIL}"),
            notice(LibretroNetplay.LINK_ENDED, reason = LibretroNetplay.REASON_PEER_LEFT))
        assertEquals(NetplayNotice.Ended("${NetplayText.ENDED}\n${NetplayText.ENDED_DETAIL}"),
            notice(LibretroNetplay.LINK_ENDED, reason = LibretroNetplay.REASON_TIMEOUT))
    }

    @Test fun failed_says_why() {
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_CONNECT), notice(LibretroNetplay.LINK_FAILED, reason = LibretroNetplay.REASON_CONNECT))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_TIMEOUT), notice(LibretroNetplay.LINK_FAILED, reason = LibretroNetplay.REASON_TIMEOUT))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_IO), notice(LibretroNetplay.LINK_FAILED, reason = LibretroNetplay.REASON_IO))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED), notice(LibretroNetplay.LINK_FAILED, reason = LibretroNetplay.REASON_NONE))
    }

    @Test fun link_states_match_the_native_enum() {
        // np_link.h nl_state; the reasons reuse REASON_* (nl_reason skips 3, the lockstep-only state mismatch).
        assertEquals(listOf(0, 1, 2, 3, 4, 5), listOf(LibretroNetplay.LINK_IDLE, LibretroNetplay.LINK_CONNECTING, LibretroNetplay.LINK_LINKED,
            LibretroNetplay.LINK_PEER_PAUSED, LibretroNetplay.LINK_ENDED, LibretroNetplay.LINK_FAILED))
    }
}
