package com.nxredux.mobile.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TreeImportFilesTest {
    @get:Rule val tmp = TemporaryFolder()
    private val t0 = 1_700_000_000_000L

    private fun file(root: File, path: String, text: String) =
        File(root, path).apply { parentFile.mkdirs(); writeText(text); setLastModified(t0) }

    @Test fun a_file_tree_in_the_card_layout_imports_and_nothing_is_deleted() {
        val card = tmp.newFolder("card"); val sd = tmp.newFolder("sd")
        file(card, "Saves/GBA/Game #1 (100%).srm", "s")
        file(card, ".userdata/shared/GBA-gpsp/Game #1 (100%).state", "st")
        file(card, "Saves/GBA/Other.srm.part", "cut short")
        file(card, "Roms/GBA/Game #1 (100%).gba", "rom")
        val summary = SaveImporter(sd, now = { t0 }).run(TreeImportFiles(FileImportTree, FileRefs.of(card.absolutePath)))
        assertEquals(ImportSummary(saves = 1, states = 1, ignored = 1, stateEmulators = setOf("gpSP")), summary)   // the ROM is only counted
        assertEquals("s", File(sd, "Saves/GBA/Game #1 (100%).srm").readText())
        assertEquals("st", File(sd, ".userdata/shared/GBA-gpsp/Game #1 (100%).state").readText())
        assertFalse(File(sd, "Saves/GBA/Other.srm.part").exists())            // a part is never imported
        for (p in listOf("Saves/GBA/Game #1 (100%).srm", ".userdata/shared/GBA-gpsp/Game #1 (100%).state", "Saves/GBA/Other.srm.part", "Roms/GBA/Game #1 (100%).gba"))
            assertTrue(p, File(card, p).exists())                             // copy only
    }

    @Test fun tops_pick_the_folders_walked() {
        val card = tmp.newFolder("ra")
        file(card, "top.srm", "a"); file(card, "saves/mGBA/deep/x.srm", "b"); file(card, "states/mGBA/x.state", "c"); file(card, "config/x.cfg", "d")
        val ref = FileRefs.of(card.absolutePath)
        assertEquals(
            setOf("top.srm", "saves/mGBA/deep/x.srm", "states/mGBA/x.state", "config/x.cfg"),
            TreeImportFiles(FileImportTree, ref, listOf("")).entries().map { it.path }.toSet(),
        )
        assertEquals(
            setOf("saves/mGBA/deep/x.srm", "states/mGBA/x.state"),
            TreeImportFiles(FileImportTree, ref, listOf("saves", "states")).entries().map { it.path }.toSet(),
        )
    }
}
