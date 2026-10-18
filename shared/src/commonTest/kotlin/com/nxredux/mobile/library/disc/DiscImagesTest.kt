package com.nxredux.mobile.library.disc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscImagesTest {
    @Test fun an_iso_and_a_raw_bin() {
        assertEquals(DataTrack("a.iso", 0, 2048, 0), DiscImages.rawTrack("a.iso", ByteArray(16)))
        val mode2 = ByteArray(16).also { b -> b[0] = 0; for (i in 1..10) b[i] = 0xFF.toByte(); b[11] = 0; b[15] = 2 }
        assertEquals(DataTrack("a.bin", 0, 2352, 24), DiscImages.rawTrack("a.bin", mode2))
        mode2[15] = 1
        assertEquals(DataTrack("a.bin", 0, 2352, 16), DiscImages.rawTrack("a.bin", mode2))
        assertEquals(DataTrack("a.bin", 0, 2048, 0), DiscImages.rawTrack("a.bin", ByteArray(16) { 7 }))   // a cartridge .bin
    }

    @Test fun a_cue_names_its_first_data_track_and_where_it_starts() {
        val multi = "FILE \"G (Track 1).bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\nFILE \"G (Track 2).bin\" BINARY\n  TRACK 02 AUDIO\n"
        assertEquals(DataTrack("G (Track 1).bin", 0, 2352, 24), DiscImages.cueTrack(multi))
        // PC Engine CD in one bin: audio track 1, data track 2 at 00:02:10 (2*75+10 = 160 frames).
        val pce = "FILE \"P.bin\" BINARY\n TRACK 01 AUDIO\n  INDEX 01 00:00:00\n TRACK 02 MODE1/2352\n  INDEX 00 00:00:10\n  INDEX 01 00:02:10\n"
        assertEquals(DataTrack("P.bin", 160L * 2352, 2352, 16), DiscImages.cueTrack(pce))
        assertNull(DiscImages.cueTrack("FILE \"a.wav\" WAVE\n TRACK 01 AUDIO\n"))
    }

    @Test fun a_gdi_points_at_track_three() {
        val gdi = "3\n1 0 4 2352 track01.bin 0\n2 600 0 2352 track02.raw 0\n3 45000 4 2352 track03.bin 0\n"
        assertEquals(DataTrack("track03.bin", 0, 2352, 16), DiscImages.gdiTrack(gdi))
    }
}

