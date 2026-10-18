package com.nxredux.mobile.cheats.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheatDbManifestTest {
    private val manifest = CheatDbManifest(
        stamp = "Wed, 21 Aug 2024 10:00:00 GMT", installedAt = 1758750000000L,
        files = listOf("Cheats/GB/Tetris (World).cht", "Cheats/GB/Alleyway (World).cht", "Cheats/GBA/Advance Wars (USA).cht", "Cheats/MGBA/Advance Wars (USA).cht"),
    )

    @Test fun render_then_parse_round_trips() {
        val text = manifest.render()
        assertTrue(text.startsWith("# nx-mobile cheatdb 1\nstamp = Wed, 21 Aug 2024 10:00:00 GMT\ninstalled = 1758750000000\nCheats/GB/Tetris (World).cht\n"))
        assertEquals(manifest, CheatDbManifest.parse(text))
    }

    @Test fun tags_round_trip_and_default_to_null() {
        val tagged = manifest.copy(tags = listOf("GB", "GBA", "MGBA"))
        assertTrue(tagged.render().contains("\ntags = GB,GBA,MGBA\n"))
        assertEquals(tagged, CheatDbManifest.parse(tagged.render()))
        assertNull(CheatDbManifest.parse(manifest.render())!!.tags)
    }

    @Test fun counts_group_by_tag() {
        assertEquals(mapOf("GB" to 2, "GBA" to 1, "MGBA" to 1), manifest.countsByTag)
        assertEquals(3, manifest.systems)
        assertEquals(4, manifest.fileCount)
        assertTrue(manifest.installed)
    }

    @Test fun crlf_blank_and_comment_lines_are_tolerated() {
        val text = "# nx-mobile cheatdb 1\r\n\r\nstamp = installed\r\n# note\r\ninstalled = 5\r\nCheats/FC/x.cht\r\n"
        assertEquals(CheatDbManifest("installed", 5L, listOf("Cheats/FC/x.cht")), CheatDbManifest.parse(text))
    }

    @Test fun unknown_keys_are_ignored_and_missing_ones_default() {
        val text = "# nx-mobile cheatdb 1\nfuture = yes\nCheats/FC/x.cht\n"
        assertEquals(CheatDbManifest(CheatDbManifest.UNKNOWN_STAMP, 0L, listOf("Cheats/FC/x.cht")), CheatDbManifest.parse(text))
    }

    @Test fun unknown_version_is_null() {
        assertNull(CheatDbManifest.parse("# nx-mobile cheatdb 2\nCheats/FC/x.cht\n"))
        assertNull(CheatDbManifest.parse(""))
        assertNull(CheatDbManifest.parse("Cheats/FC/x.cht\n"))
    }

    @Test fun no_files_parses_but_is_not_installed() {
        val m = CheatDbManifest.parse("# nx-mobile cheatdb 1\nstamp = x\ninstalled = 1\n")
        assertEquals(emptyList(), m!!.files)
        assertFalse(m.installed)
        assertEquals(0, m.systems)
    }

    @Test fun a_path_containing_equals_is_still_a_path() {
        val m = CheatDbManifest.parse("# nx-mobile cheatdb 1\nCheats/GB/A = B (USA).cht\n")
        assertEquals(listOf("Cheats/GB/A = B (USA).cht"), m!!.files)
    }
}
