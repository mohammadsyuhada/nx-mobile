package com.nxredux.mobile.runner

import com.nxredux.mobile.storage.FileRefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RealPathLaunchTest {
    private val dir = "/storage/emulated/0/NX/Roms/Sega CD (SEGACD)"

    @Test fun a_file_ref_opens_in_place_with_its_tracks_beside_it() {
        val cue = FileRefs.of("$dir/Sonic CD.cue")
        val tracks = listOf("Sonic CD (Track 1).bin" to FileRefs.of("$dir/Sonic CD (Track 1).bin"))
        assertEquals("$dir/Sonic CD.cue", RealPathLaunch.inPlace(cue, "Sonic CD.cue", tracks))
        assertEquals("$dir/Sonic CD.chd", RealPathLaunch.inPlace(FileRefs.of("$dir/Sonic CD.chd"), "Sonic CD.chd", emptyList()))
    }

    @Test fun a_saf_game_or_a_mismatch_is_copied() {
        assertNull(RealPathLaunch.inPlace(ContentRef("content://tree/doc/Sonic.md"), "Sonic.md", emptyList()))
        // A track reached through SAF, or elsewhere, or a name the core must see otherwise (an archive in lowercase).
        val cue = FileRefs.of("$dir/Sonic CD.cue")
        assertNull(RealPathLaunch.inPlace(cue, "Sonic CD.cue", listOf("Sonic CD (Track 1).bin" to ContentRef("content://x"))))
        assertNull(RealPathLaunch.inPlace(cue, "Sonic CD.cue", listOf("Sonic CD (Track 1).bin" to FileRefs.of("/elsewhere/Sonic CD (Track 1).bin"))))
        assertNull(RealPathLaunch.inPlace(FileRefs.of("$dir/MSlug.zip"), "mslug.zip", emptyList()))
    }
}
