package com.nxredux.mobile.runner

import com.nxredux.mobile.netplay.NetplayRole
import com.nxredux.mobile.netplay.NetplaySession
import com.nxredux.mobile.netplay.NetplayText
import com.swordfish.libretrodroid.LibretroNetplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetplayGameTest {
    private fun notice(state: Int, seconds: Int = 0, reason: Int = LibretroNetplay.REASON_NONE, onCopy: Boolean = false, connectingMs: Long = 0) =
        netplayNotice(state, seconds, reason, onCopy, connectingMs)

    @Test fun countdowns_show_as_the_pill() {
        assertEquals(NetplayNotice.Pill(NetplayText.waiting(12)), notice(LibretroNetplay.STATE_STALLED, 12))
        assertEquals(NetplayNotice.Pill(NetplayText.peerPaused(7)), notice(LibretroNetplay.STATE_PEER_PAUSED, 7))
    }

    @Test fun a_running_session_or_a_local_pause_shows_nothing() {
        // A local pause reports RUNNING with its own countdown: only its eventual ENDED is shown.
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.STATE_RUNNING, 25))
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.STATE_SYNCING))
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.STATE_IDLE))
    }

    @Test fun ended_says_whether_progress_is_kept() {
        assertEquals(NetplayNotice.Ended("${NetplayText.ENDED}\n${NetplayText.ENDED_DETAIL}"),
            notice(LibretroNetplay.STATE_ENDED, reason = LibretroNetplay.REASON_PEER_LEFT))
        assertEquals(NetplayNotice.Ended("${NetplayText.ENDED_COPY}\n${NetplayText.ENDED_DETAIL}"),
            notice(LibretroNetplay.STATE_ENDED, reason = LibretroNetplay.REASON_TIMEOUT, onCopy = true))
    }

    @Test fun failed_says_why() {
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_MISMATCH),
            notice(LibretroNetplay.STATE_FAILED, reason = LibretroNetplay.REASON_STATE_MISMATCH))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_CONNECT), notice(LibretroNetplay.STATE_FAILED, reason = LibretroNetplay.REASON_CONNECT))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_TIMEOUT), notice(LibretroNetplay.STATE_FAILED, reason = LibretroNetplay.REASON_TIMEOUT))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_IO), notice(LibretroNetplay.STATE_FAILED, reason = LibretroNetplay.REASON_IO))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED), notice(LibretroNetplay.STATE_FAILED, reason = LibretroNetplay.REASON_NONE))
        assertEquals("Could not reach the other device.", NetplayText.FAILED_CONNECT)
        assertEquals("The other device stopped responding.", NetplayText.FAILED_TIMEOUT)
        assertEquals("The connection was lost.", NetplayText.FAILED_IO)
    }

    @Test fun no_peer_within_the_connect_timeout_fails() {
        assertEquals(45_000L, NETPLAY_CONNECT_TIMEOUT_MS)
        assertEquals(NetplayNotice.Clear, notice(LibretroNetplay.STATE_CONNECTING, connectingMs = NETPLAY_CONNECT_TIMEOUT_MS - 1))
        assertEquals(NetplayNotice.Failed(NetplayText.FAILED_CONNECT), notice(LibretroNetplay.STATE_CONNECTING, connectingMs = NETPLAY_CONNECT_TIMEOUT_MS))
    }

    @Test fun the_session_comes_from_the_extras() {
        assertEquals(NetplaySession(NetplayRole.HOST, "10.0.0.9", "picodrive", "genesis_plus_gx"),
            netplaySessionFrom("HOST", "10.0.0.9", "picodrive", "genesis_plus_gx"))
        assertEquals(NetplaySession(NetplayRole.CLIENT, "10.0.0.2", "snes9x", "snes9x"),
            netplaySessionFrom("CLIENT", "10.0.0.2", "snes9x", "snes9x"))
        assertNull(netplaySessionFrom(null, null, "snes9x", null))          // every other launch
        assertNull(netplaySessionFrom("BOGUS", "10.0.0.2", "snes9x", "snes9x"))
        assertNull(netplaySessionFrom("CLIENT", "", "snes9x", "snes9x"))   // a client needs the host
        assertNull(netplaySessionFrom("HOST", "10.0.0.2", "snes9x", null))
    }

    @Test fun both_roles_need_the_peer_as_ipv4() {
        // The host's engine accepts only this address: without one the session fails closed.
        assertNull(netplaySessionFrom("HOST", "", "snes9x", "snes9x"))
        assertNull(netplaySessionFrom("HOST", null, "snes9x", "snes9x"))
        assertNull(netplaySessionFrom("HOST", "fe80::1", "snes9x", "snes9x"))
        assertNull(netplaySessionFrom("CLIENT", "not an ip", "snes9x", "snes9x"))
        assertEquals("10.0.0.2", netplaySessionFrom("HOST", "::ffff:10.0.0.2", "snes9x", "snes9x")?.peerIp)
    }

    @Test fun empty_saves_travel_in_the_extras() {
        assertEquals(NetplaySession(NetplayRole.HOST, "10.0.0.9", "snes9x", "snes9x", emptySaves = true),
            netplaySessionFrom("HOST", "10.0.0.9", "snes9x", "snes9x", emptySaves = true))
        assertEquals(false, netplaySessionFrom("CLIENT", "10.0.0.2", "snes9x", "snes9x")?.emptySaves)
    }

    @Test fun over_usb_travels_in_the_extras() {
        assertEquals(true, netplaySessionFrom("HOST", "10.99.0.2", "snes9x", "snes9x", overUsb = true)?.overUsb)
        assertEquals(false, netplaySessionFrom("CLIENT", "10.0.0.2", "snes9x", "snes9x")?.overUsb)
    }

    @Test fun first_frame_notices() {
        val real = NetplaySession(NetplayRole.HOST, "10.0.0.2", "snes9x", "snes9x")
        assertEquals(emptyList<String>(), netplayFirstFrameNotices(real, "Snes9x", ownCopyFailed = false))
        val other = NetplaySession(NetplayRole.HOST, "10.0.0.2", "picodrive", "genesis_plus_gx")
        assertEquals(listOf(NetplayText.usingCopy("PicoDrive")), netplayFirstFrameNotices(other, "PicoDrive", ownCopyFailed = false))
        assertEquals(listOf(NetplayText.usingCopy("PicoDrive"), NetplayText.OWN_COPY_FAILED), netplayFirstFrameNotices(other, "PicoDrive", ownCopyFailed = true))
        assertEquals(listOf(NetplayText.SAVES_NOT_COPIED), netplayFirstFrameNotices(real.copy(emptySaves = true), "Snes9x", ownCopyFailed = false))
        val joiner = NetplaySession(NetplayRole.CLIENT, "10.0.0.1", "snes9x", "snes9x")
        assertEquals(listOf(NetplayText.SAVES_NOT_COPIED), netplayFirstFrameNotices(joiner.copy(emptySaves = true), "Snes9x", ownCopyFailed = false))
        assertEquals(emptyList<String>(), netplayFirstFrameNotices(joiner, "Snes9x", ownCopyFailed = false))
    }
}
