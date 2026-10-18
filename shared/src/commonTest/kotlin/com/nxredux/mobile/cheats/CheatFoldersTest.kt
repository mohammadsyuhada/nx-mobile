package com.nxredux.mobile.cheats

import com.nxredux.mobile.cheats.db.CheatDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheatFoldersTest {
    @Test fun an_unrouted_game_reads_its_own_tag() {
        assertEquals(listOf("MD"), CheatFolders.tagsFor("MD", "Sonic.md"))
        assertEquals(listOf("MD"), CheatFolders.tagsFor("MD", "Sonic.bin"))   // bin is any system's
        assertEquals(listOf("GBA"), CheatFolders.tagsFor("gba", "Golden Sun.gba"))
        assertEquals(listOf("PSX"), CheatFolders.tagsFor("PSX", "Vagrant Story.chd"))
    }

    @Test fun a_routed_game_reads_only_its_system() {
        assertEquals(listOf("SEGACD"), CheatFolders.tagsFor("MD", "Sonic CD.CHD"))
        assertEquals(listOf("FDS"), CheatFolders.tagsFor("FC", "Zelda.fds"))
        assertEquals(listOf("PCECD"), CheatFolders.tagsFor("PCE", "Ys.cue"))
        assertEquals(listOf("BS"), CheatFolders.tagsFor("SUPA", "BS Zelda.bs"))
    }

    @Test fun game_boy_tags_fall_back_to_the_partner_library() {   // dual-mode carts are filed under Game Boy only
        assertEquals(listOf("GBC", "GB"), CheatFolders.tagsFor("GB", "Pokemon - Yellow.gbc"))
        assertEquals(listOf("GB", "GBC"), CheatFolders.tagsFor("GB", "Tetris.gb"))
        assertEquals(listOf("GBC", "GB"), CheatFolders.tagsFor("GBC", "Pokemon - Yellow.gbc"))
        assertEquals(listOf("GB", "GBC"), CheatFolders.tagsFor("GBC", "Tetris.gb"))
        assertEquals(listOf("GBC", "SGB"), CheatFolders.tagsFor("SGB", "Pokemon - Yellow.gbc"))
        assertEquals(listOf("SGB", "GBC"), CheatFolders.tagsFor("SGB", "Tetris.gb"))
        assertEquals("Cheats/GBC", CheatFolders.primary("GB", "Pokemon - Yellow.gbc"))
    }

    @Test fun gpgx_reads_its_own_folder_then_the_system() {
        assertEquals(listOf("GPGX", "GG"), CheatFolders.tagsFor("GPGX", "Aladdin (Japan).gg"))
        assertEquals(listOf("GPGX", "SMS"), CheatFolders.tagsFor("GPGX", "Alex Kidd.sms"))
        assertEquals(listOf("GPGX", "SEGACD"), CheatFolders.tagsFor("GPGX", "Lunar.m3u"))
        assertEquals(listOf("GPGX", "MD"), CheatFolders.tagsFor("GPGX", "Aladdin (Japan).md"))
        assertEquals(listOf("GPGX", "MD"), CheatFolders.tagsFor("GPGX", "noext"))
    }

    @Test fun primary_names_the_database_folder() {
        assertEquals("Cheats/GG", CheatFolders.primary("GPGX", "x.gg"))
        assertEquals("Cheats/MD", CheatFolders.primary("GPGX", "x.md"))
        assertEquals("Cheats/SEGACD", CheatFolders.primary("MD", "x.cue"))
        assertEquals("Cheats/PSP", CheatFolders.primary("PSP", "x.iso"))
    }

    @Test fun every_route_target_has_a_database_folder() {
        for (tag in CheatFolders.ROUTES.keys + CheatFolders.PARTNERS.keys) for (target in CheatFolders.targets(tag)) {
            assertTrue(target in CheatDatabase.FOLDERS, "$tag routes to $target, which the database never installs")
        }
    }
}
