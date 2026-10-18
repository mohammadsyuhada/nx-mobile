package com.nxredux.mobile.scraper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrapeJobCodecTest {
    private val half = ScrapeState(
        phase = JobPhase.PAUSED, pauseReason = "No connection",
        items = listOf(
            ScrapeItem("/Roms/GB/Tetris.gb", "GB", "Tetris", ItemStatus.DONE),
            ScrapeItem("/Roms/GB/Bad.gb", "GB", "Bad", ItemStatus.ERROR, "Couldn't make the art"),
            ScrapeItem("/Extra/x1/Genesis/Sonic.md", "MD", "Sonic"),
        ),
    )

    @Test fun roundTrip() {
        val (back, replace) = ScrapeJobCodec.decode(ScrapeJobCodec.encode(half, replace = true))!!
        assertEquals(half, back)
        assertTrue(replace)
        assertEquals(2, back.done)
        assertEquals(3, back.total)
    }

    @Test fun runningJobComesBackPausedWithInFlightItemWaiting() {
        val running = half.copy(phase = JobPhase.RUNNING, pauseReason = null, items = half.items.take(2) + half.items[2].copy(status = ItemStatus.DOWNLOADING))
        val (back, replace) = ScrapeJobCodec.decode(ScrapeJobCodec.encode(running, replace = false))!!
        assertEquals(JobPhase.PAUSED, back.phase)
        assertEquals(ItemStatus.WAITING, back.items[2].status)
        assertFalse(replace)
    }

    @Test fun notesRoundTrip() {
        val noted = half.copy(notes = mapOf(SourceId.SS to ScreenScraperSource.DAILY_LIMIT, SourceId.TGDB to "TheGamesDB: monthly limit reached"))
        assertEquals(noted.notes, ScrapeJobCodec.decode(ScrapeJobCodec.encode(noted, replace = false))!!.first.notes)
        // A job saved before notes existed decodes without any.
        val old = """{"version":1,"phase":"PAUSED","replace":false,"items":[{"path":"/Roms/GB/Tetris.gb","tag":"GB","name":"Tetris","status":"WAITING"}]}"""
        assertEquals(emptyMap(), ScrapeJobCodec.decode(old)!!.first.notes)
    }

    @Test fun garbageIsNull() {
        assertNull(ScrapeJobCodec.decode("not json"))
        assertNull(ScrapeJobCodec.decode(""))
    }

    @Test fun writtenArtLogRoundTrip() {
        val log = WrittenArtLog.decode(null)
        assertTrue(log.add("Roms/GB/.media/Tetris.png", null))
        assertTrue(log.add("Roms/MD/.media/Sonic.png", ArtOrigin("screenscraper", "1", "box-3D/us")))
        assertFalse(log.add("Roms/GB/.media/Tetris.png", null))
        val back = WrittenArtLog.decode(log.encode())
        assertEquals(log.entries, back.entries)
        back.remove(listOf("Roms/GB/.media/Tetris.png"))
        assertEquals(setOf("Roms/MD/.media/Sonic.png"), back.entries)
    }
}
