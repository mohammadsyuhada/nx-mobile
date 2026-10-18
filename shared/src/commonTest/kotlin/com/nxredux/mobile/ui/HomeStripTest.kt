package com.nxredux.mobile.ui

import com.nxredux.mobile.gametime.GameTotal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeStripTest {
    private fun total(name: String, ms: Long) =
        GameTotal(gameId = 1L, name = name, tag = "X", romPath = "Roms/X/$name.zip", totalMs = ms, plays = 1L, lastPlayedAt = 0L)
    private val played = HomeStats(playedMs = 45_480_000L, unlocks = 2, signedIn = true, top = total("The Legend of Zelda - The Minish Cap (USA)", 14_100_000L), computed = true)

    @Test fun playedMonth() {
        val s = homeStrip(played)
        assertEquals(HomeStrip("12h 38m", false, "2", HomeStripMost("The Legend of Zelda - The Minish Cap", "3h 55m")), s)
        assertEquals(2, homeStripLines(s))
    }
    @Test fun signedOut() = assertNull(homeStrip(played.copy(signedIn = false, unlocks = null)).achievements)
    @Test fun nothingPlayed() {
        val s = homeStrip(HomeStats(0L, 0, signedIn = true, top = null, computed = true))
        assertEquals(HomeStrip(NO_PLAY_YET, true, "0", null), s)
        assertEquals(1, homeStripLines(s))
    }
    @Test fun pending() {
        val s = homeStrip(HomeStats(null, null, signedIn = true, top = null, computed = false))
        assertEquals(HomeStrip(STAT_PENDING, false, STAT_PENDING, HomeStripMost(STAT_PENDING, null)), s)
        assertEquals(2, homeStripLines(s))
    }
    @Test fun pendingSignedOut() = assertNull(homeStrip(HomeStats(null, null, signedIn = false, top = null, computed = false)).achievements)
    @Test fun noStripNoLines() {
        assertEquals(0, homeStripLines(null))
    }
    @Test fun titleCleaning() {
        assertEquals("Shin Megami Tensei - Devil Children", homeStripTitle("Shin Megami Tensei - Devil Children (Japan) (Rev 1) [T-En by Example v1.2]"))
        assertEquals("Alleyway", homeStripTitle("Alleyway (World)"))
        assertEquals("OutRun", homeStripTitle("OutRun"))
        assertEquals("(Demo)", homeStripTitle("(Demo)"))
    }
}
