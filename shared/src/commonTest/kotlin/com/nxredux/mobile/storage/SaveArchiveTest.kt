package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveArchiveTest {
    @Test fun the_three_areas() {
        assertEquals(SaveArea.SAVES, SaveArchive.areaOf("Saves/GBA/Game.srm"))
        assertEquals(SaveArea.SAVES, SaveArchive.areaOf("Saves/PSP/SAVEDATA/ULUS10466/DATA.BIN"))
        assertEquals(SaveArea.STATES, SaveArchive.areaOf(".userdata/shared/GBA-gpsp/Game.state.auto"))
        assertEquals(SaveArea.MARKERS, SaveArchive.areaOf(".userdata/shared/.minui/GBA/Game.gba.9.bmp"))
        assertEquals(SaveArea.MARKERS, SaveArchive.areaOf(".userdata/shared/.minui/GBA/Game.gba.txt"))
    }
    @Test fun private_and_unrelated_paths_are_outside() {
        for (p in listOf(
            ".userdata/android/ra/cache/login.json", ".userdata/android/GBA-gpsp/minarch.cfg",
            ".userdata/shared/.minui/recent.txt", ".userdata/shared/.minui/shortcuts.txt",
            ".userdata/shared/game_time.db", ".userdata/shared/.ra/cache/games/abc/sets.json",
            ".userdata/shared/noDash/x", "Saves/Game.srm", "Saves/GBA/", "Roms/GBA/Game.gba", "Bios/PS/scph5501.bin", "",
        )) assertNull(SaveArchive.areaOf(p), p)
    }
    @Test fun unsafe_entries_are_refused() {
        assertEquals("Saves/GBA/Game.srm", SaveArchive.safePath("Saves/GBA/Game.srm"))
        assertEquals("Saves/GBA/Game.srm", SaveArchive.safePath("Saves\\GBA\\Game.srm"))
        for (bad in listOf("../Saves/GBA/x.srm", "Saves/../../x", "/Saves/GBA/x.srm", "C:/Saves/x", "Saves/./GBA/x", "Saves//GBA/x", "Saves/GBA/"))
            assertNull(SaveArchive.safePath(bad), bad)
    }
    @Test fun new_identical_and_newer() {
        assertEquals(ImportAction.NEW, ImportRules.decide(null, 1_000) { error("not read") })
        assertEquals(ImportAction.IDENTICAL, ImportRules.decide(5_000, 1_000) { true })
        assertEquals(ImportAction.REPLACE, ImportRules.decide(1_000, 3_001) { false })
    }
    @Test fun older_or_equal_time_is_skipped() {
        assertEquals(ImportAction.SKIP_OLDER, ImportRules.decide(5_000, 1_000) { false })
        assertEquals(ImportAction.SKIP_OLDER, ImportRules.decide(1_000, 3_000) { false })   // within zip's 2 s precision
    }
    @Test fun an_unknown_incoming_time_never_replaces() {
        assertEquals(ImportAction.NEW, ImportRules.decide(null, null) { false })
        assertEquals(ImportAction.IDENTICAL, ImportRules.decide(1_000, null) { true })
        assertEquals(ImportAction.SKIP_OLDER, ImportRules.decide(1_000, null) { false })
    }
    @Test fun summaries() {
        assertEquals("Exported 2 saves and 3 states (1.5 MB)", ExportSummary(2, 3, 1_572_864).text())
        assertEquals("Nothing to export", ExportSummary(0, 0, 0).text())
        assertEquals(
            "Imported 37 saves and 52 states; 3 older files skipped (left in place); 2 replaced (kept as .bak); 4 other files ignored",
            ImportSummary(saves = 37, states = 52, markers = 9, replaced = 2, skippedOlder = 3, ignored = 4).text(moved = true),
        )
        assertEquals("Imported 1 save and 0 states; 3 older files skipped", ImportSummary(saves = 1, skippedOlder = 3).text(moved = false))
        assertEquals("Nothing to import", ImportSummary().text(moved = true))
        assertTrue(ImportSummary(saves = 1, failed = 2).text(moved = true).endsWith("; 2 could not be imported"))
        assertFalse(ImportSummary(identical = 2).text(moved = true) == "Nothing to import")
    }
    @Test fun summary_says_when_files_were_already_here_or_only_slot_files_came_in() {
        assertEquals("Imported 0 saves and 0 states; 12 already here", ImportSummary(identical = 12).text(moved = true))
        assertEquals("Imported 0 saves and 0 states; 3 slot files", ImportSummary(markers = 3).text(moved = false))
        assertEquals("Imported 1 save and 0 states; 1 already here", ImportSummary(saves = 1, markers = 2, identical = 1).text(moved = false))
    }
    @Test fun summary_says_when_the_zip_could_not_be_deleted() {
        assertEquals("Imported 1 save and 0 states; the zip could not be deleted", ImportSummary(saves = 1, zipNotDeleted = true).text(moved = true))
    }
}
