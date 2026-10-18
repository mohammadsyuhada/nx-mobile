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

    @Test fun a_file_tree_in_the_card_layout_imports_and_a_move_removes_only_what_it_emptied() {
        val card = tmp.newFolder("card"); val sd = tmp.newFolder("sd")
        file(card, "Saves/GBA/Game #1 (100%).srm", "s")
        file(card, ".userdata/shared/GBA-gpsp/Game #1 (100%).state", "st")
        file(card, "Saves/GBA/Other.srm.part", "cut short")
        file(card, "Roms/GBA/Game #1 (100%).gba", "rom")
        val summary = SaveImporter(sd, now = { t0 }).run(TreeImportFiles(FileImportTree, FileRefs.of(card.absolutePath)), move = true)
        assertEquals(ImportSummary(saves = 1, states = 1), summary)
        assertEquals("s", File(sd, "Saves/GBA/Game #1 (100%).srm").readText())
        assertEquals("st", File(sd, ".userdata/shared/GBA-gpsp/Game #1 (100%).state").readText())
        assertFalse(File(sd, "Saves/GBA/Other.srm.part").exists())            // a part is never imported
        assertTrue(File(card, "Saves/GBA/Other.srm.part").exists())           // nor deleted, so its folder stays
        assertFalse(File(card, ".userdata/shared").exists())                  // emptied by the move, up to .userdata/shared itself
        assertTrue(File(card, ".userdata").isDirectory)                       // never .userdata
        assertTrue(File(card, "Roms/GBA/Game #1 (100%).gba").exists())        // outside the save areas
    }

    @Test fun a_copy_deletes_nothing() {
        val card = tmp.newFolder("card"); val sd = tmp.newFolder("sd")
        file(card, "Saves/GBA/Game.srm", "s")
        SaveImporter(sd, now = { t0 }).run(TreeImportFiles(FileImportTree, FileRefs.of(card.absolutePath)), move = false)
        assertTrue(File(card, "Saves/GBA/Game.srm").exists())
    }
}
