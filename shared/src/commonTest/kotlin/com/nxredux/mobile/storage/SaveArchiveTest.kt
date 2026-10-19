package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    }
    @Test fun import_summary_lines() {
        val skipped = listOf(SkippedFile("Zelda.srm", SkipReason.NO_GAME), SkippedFile("Old.srm", SkipReason.PHONE_NEWER))
        assertEquals(
            listOf("Imported 37 saves and 1 state", "12 already here", "2 replaced (kept as .bak)", "2 skipped", "4 other files ignored"),
            ImportSummary(saves = 37, states = 1, markers = 9, identical = 12, replaced = 2, ignored = 4, skipped = skipped).lines(),
        )
        assertEquals(listOf("Imported 0 saves and 0 states", "1 other file ignored"), ImportSummary(ignored = 1).lines())
        assertEquals(listOf("Nothing to import"), ImportSummary().lines())
    }
    @Test fun import_summaries_add_up() {
        val a = ImportSummary(saves = 1, states = 2, markers = 3, identical = 4, replaced = 5, ignored = 6, skipped = listOf(SkippedFile("A.srm", SkipReason.NO_GAME)))
        val b = ImportSummary(saves = 10, states = 20, markers = 30, identical = 40, replaced = 50, ignored = 60, skipped = listOf(SkippedFile("B.srm", SkipReason.UNREADABLE)))
        assertEquals(
            ImportSummary(11, 22, 33, 44, 55, 66, listOf(SkippedFile("A.srm", SkipReason.NO_GAME), SkippedFile("B.srm", SkipReason.UNREADABLE))),
            a + b,
        )
        assertEquals(a, ImportSummary() + a)
    }
    @Test fun skipped_file_text() {
        assertEquals(
            "Tetris.srm: matches Game Boy and Game Boy Advance",
            SkippedFile("Tetris.srm", SkipReason.AMBIGUOUS, "Game Boy and Game Boy Advance").text(),
        )
        assertEquals("Zelda.srm: no matching game", SkippedFile("Zelda.srm", SkipReason.NO_GAME).text())
        assertEquals("Bad.srm: could not be read", SkippedFile("Bad.srm", SkipReason.UNREADABLE).text())
    }
}
