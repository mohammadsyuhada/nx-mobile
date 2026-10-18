package com.nxredux.mobile.storage

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveExportTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(root: File, path: String, text: String, mtime: Long = 1_700_000_000_000) =
        File(root, path).apply { parentFile.mkdirs(); writeText(text); setLastModified(mtime) }

    @Test fun writes_the_three_areas_with_times_and_nothing_else() {
        val root = tmp.newFolder("sd")
        file(root, "Saves/GBA/Game.srm", "sram")
        file(root, "Saves/PSP/SAVEDATA/ULUS1/DATA.BIN", "psp")
        file(root, ".userdata/shared/GBA-gpsp/Game.state.auto", "state")
        file(root, ".userdata/shared/.minui/GBA/Game.gba.txt", "9")
        file(root, ".userdata/shared/.minui/recent.txt", "r")
        file(root, ".userdata/shared/game_time.db", "db")
        file(root, ".userdata/android/ra/cache/login.json", "token")
        val out = ByteArrayOutputStream()
        val summary = SaveExport.write(root, out)
        val entries = mutableMapOf<String, Long>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
            while (true) { val e = z.nextEntry ?: break; entries[e.name] = e.time; z.readBytes() }
        }
        assertEquals(
            setOf("Saves/GBA/Game.srm", "Saves/PSP/SAVEDATA/ULUS1/DATA.BIN", ".userdata/shared/GBA-gpsp/Game.state.auto", ".userdata/shared/.minui/GBA/Game.gba.txt"),
            entries.keys,
        )
        assertTrue(entries.values.all { kotlin.math.abs(it - 1_700_000_000_000) <= 2_000 })
        assertEquals(ExportSummary(saves = 2, states = 1, bytes = 4 + 3 + 5 + 1), summary)
    }

    @Test fun a_failed_write_reports_and_keeps_nothing() {
        val root = tmp.newFolder("sd")
        file(root, "Saves/GBA/Game.srm", "sram")
        val failing = object : OutputStream() { override fun write(b: Int) = throw IOException("full") }
        val thrown = runCatching { SaveExport.write(root, failing) }.exceptionOrNull()
        assertTrue(thrown is IOException)
        assertTrue(File(root, "Saves/GBA/Game.srm").readText() == "sram")
    }

    @Test fun nothing_to_export() {
        assertEquals(ExportSummary(0, 0, 0), SaveExport.write(tmp.newFolder("empty"), ByteArrayOutputStream()))
    }

    @Test fun suggested_name() {
        assertTrue(Regex("""nx-mobile-saves-\d{8}\.zip""").matches(SaveExport.suggestedName(1_700_000_000_000)))
    }

    @Test fun write_file_replaces_the_target_through_a_part() {
        val root = tmp.newFolder("sdroot")
        file(root, "Saves/GBA/Game.srm", "sram")
        val dir = tmp.newFolder("Exports")
        val target = File(dir, "nx-mobile-saves-20261015.zip").apply { writeText("old") }
        assertEquals(1, SaveExport.writeFile(root, target).saves)
        ZipInputStream(target.inputStream()).use { assertEquals("Saves/GBA/Game.srm", it.nextEntry!!.name) }
        assertEquals(listOf(target.name), dir.list()!!.toList())        // no part left
    }

    @Test fun an_empty_export_to_a_file_leaves_the_target_and_no_part() {
        val root = tmp.newFolder("empty")
        val dir = tmp.newFolder("Exports2")
        val target = File(dir, "a.zip").apply { writeText("old") }
        assertEquals(0, SaveExport.writeFile(root, target).saves)
        assertEquals("old", target.readText())
        assertEquals(listOf("a.zip"), dir.list()!!.toList())
    }

    @Test fun an_export_onto_a_folder_fails_and_leaves_it_alone() {
        val root = tmp.newFolder("sdroot3")
        file(root, "Saves/GBA/Game.srm", "sram")
        val dir = tmp.newFolder("Exports3")
        val target = File(dir, "x.zip").apply { mkdir() }
        File(target, "keep.txt").writeText("k")
        try { SaveExport.writeFile(root, target); org.junit.Assert.fail("expected IOException") } catch (_: IOException) {}
        assertEquals(listOf("keep.txt"), target.list()!!.toList())
        assertEquals(listOf("x.zip"), dir.list()!!.toList())
    }

    @Test fun console_folder_exports_as_the_default_emulators_folder() {
        val root = tmp.newFolder("sd")
        file(root, "Saves/GBA/Game.srm", "sram")
        file(root, "Saves/PSP/SAVEDATA/DATA.BIN", "psp")
        val out = ByteArrayOutputStream()
        SaveExport.write(root, out, exportTag = { if (it == "GBA") "MGBA" else it })
        val entries = mutableMapOf<String, Long>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
            while (true) { val e = z.nextEntry ?: break; entries[e.name] = e.time; z.readBytes() }
        }
        assertEquals(setOf("Saves/MGBA/Game.srm", "Saves/PSP/SAVEDATA/DATA.BIN"), entries.keys)
        assertTrue(kotlin.math.abs(entries.getValue("Saves/MGBA/Game.srm") - 1_700_000_000_000) <= 2_000)
    }

    /** Genesis and Master System both on Genesis Plus GX: their same-named saves keep their console folders, not one GPGX copy. */
    @Test fun two_consoles_meeting_in_the_gpgx_folder_keep_their_console_folders() {
        val root = tmp.newFolder("sd")
        file(root, "Saves/MD/Same.srm", "genesis")
        file(root, "Saves/SMS/Same.srm", "master system")
        file(root, "Saves/SMS/Alex Kidd.srm", "alex")
        val out = ByteArrayOutputStream()
        val summary = SaveExport.write(root, out, exportTag = { if (it == "MD" || it == "SMS") "GPGX" else it })
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
            while (true) { val e = z.nextEntry ?: break; entries[e.name] = z.readBytes().decodeToString() }
        }
        assertEquals(mapOf("Saves/GPGX/Alex Kidd.srm" to "alex", "Saves/MD/Same.srm" to "genesis", "Saves/SMS/Same.srm" to "master system"), entries)
        assertEquals(3, summary.saves)
    }

    /** A leftover `Saves/MGBA/x` and `Saves/GBA/x` map to one entry: the newest goes, the console folder on a tie. */
    @Test fun two_folders_mapping_to_one_entry_keep_the_newest() {
        val root = tmp.newFolder("sd")
        file(root, "Saves/GBA/Game.srm", "console", mtime = 1_700_000_000_000)
        file(root, "Saves/MGBA/Game.srm", "leftover", mtime = 1_700_000_100_000)
        file(root, "Saves/GBA/Tie.srm", "console", mtime = 1_700_000_000_000)
        file(root, "Saves/MGBA/Tie.srm", "leftover", mtime = 1_700_000_000_000)
        val out = ByteArrayOutputStream()
        val summary = SaveExport.write(root, out, exportTag = { if (it == "GBA" || it == "MGBA") "MGBA" else it })
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
            while (true) { val e = z.nextEntry ?: break; entries[e.name] = z.readBytes().decodeToString() }
        }
        assertEquals(mapOf("Saves/MGBA/Game.srm" to "leftover", "Saves/MGBA/Tie.srm" to "console"), entries)
        assertEquals(2, summary.saves)
    }
}
