package com.nxredux.mobile.storage

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveImportTest {
    @get:Rule val tmp = TemporaryFolder()
    private val t0 = 1_700_000_000_000L

    private fun file(root: File, path: String, text: String, mtime: Long = t0) =
        File(root, path).apply { parentFile.mkdirs(); writeText(text); setLastModified(mtime) }

    private fun zipOf(vararg entries: Triple<String, String, Long>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> for ((n, text, time) in entries) { z.putNextEntry(ZipEntry(n).apply { this.time = time }); z.write(text.toByteArray()); z.closeEntry() } }
        return out.toByteArray()
    }

    @Test fun new_identical_replace_and_skip_from_a_folder_with_move() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/New.srm", "new")
        file(src, "Saves/GBA/Same.srm", "same"); file(sd, "Saves/GBA/Same.srm", "same", t0 - 50_000)
        file(src, "Saves/GBA/Newer.srm", "newer", t0); file(sd, "Saves/GBA/Newer.srm", "old", t0 - 60_000)
        file(src, "Saves/GBA/Older.srm", "older", t0 - 60_000); file(sd, "Saves/GBA/Older.srm", "mine", t0)
        file(src, ".userdata/shared/GBA-gpsp/New.state", "st")
        file(src, ".userdata/shared/game_time.db", "db")
        val summary = SaveImporter(sd, now = { t0 }).run(DirImportFiles(src), move = true)
        assertEquals("new", File(sd, "Saves/GBA/New.srm").readText())
        assertEquals(t0, File(sd, "Saves/GBA/New.srm").lastModified() / 1000 * 1000)
        assertEquals("newer", File(sd, "Saves/GBA/Newer.srm").readText())
        assertTrue(File(sd, "Saves/GBA").list()!!.any { SaveCopyRules.isBackupOf(it, "Newer.srm") })
        assertEquals("mine", File(sd, "Saves/GBA/Older.srm").readText())
        assertFalse(File(sd, ".userdata/shared/game_time.db").exists())
        // Move: taken files gone from the source, the skipped one and the ignored one stay.
        assertFalse(File(src, "Saves/GBA/New.srm").exists()); assertFalse(File(src, "Saves/GBA/Same.srm").exists()); assertFalse(File(src, "Saves/GBA/Newer.srm").exists())
        assertTrue(File(src, "Saves/GBA/Older.srm").exists()); assertTrue(File(src, ".userdata/shared/game_time.db").exists())
        assertEquals(ImportSummary(saves = 2, states = 1, identical = 1, replaced = 1, skippedOlder = 1, ignored = 1), summary)
    }

    /** `Saves/GPGX` holds several consoles' saves: each goes to its game's console by the library's ROM names, else Genesis. */
    @Test fun a_gpgx_save_goes_to_its_games_console() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GPGX/Alex Kidd.srm", "sms")
        file(src, "Saves/GPGX/Shinobi.srm", "gg")
        file(src, "Saves/GPGX/Sonic.srm", "md")
        file(src, "Saves/GPGX/Both.srm", "both")
        file(src, "Saves/GPGX/Unknown.srm", "unknown")
        val games = mapOf(
            "SMS" to setOf("Alex Kidd.sms", "Both.sms"), "GG" to setOf("Shinobi.gg"), "MD" to setOf("Sonic.md", "Both.md"),
            "GBA" to setOf("Unknown.gba"),   // not a console the GPGX folder serves
        )
        SaveImporter(sd, format = SaveFormat.RETROARCH, games = games).run(DirImportFiles(src), move = false)
        assertEquals("sms", File(sd, "Saves/SMS/Alex Kidd.srm").readText())
        assertEquals("gg", File(sd, "Saves/GG/Shinobi.srm").readText())
        assertEquals("md", File(sd, "Saves/MD/Sonic.srm").readText())
        assertEquals("both", File(sd, "Saves/MD/Both.srm").readText())         // two consoles hold it: today's fallback
        assertEquals("unknown", File(sd, "Saves/MD/Unknown.srm").readText())   // held by none
    }

    @Test fun move_leaves_skipped_and_failed_sources() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/Older.srm", "older", t0 - 60_000); file(sd, "Saves/GBA/Older.srm", "mine", t0)
        SaveImporter(sd).run(DirImportFiles(src), move = true)
        assertTrue(File(src, "Saves/GBA/Older.srm").exists())
    }

    @Test fun copy_never_deletes() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/New.srm", "new")
        SaveImporter(sd).run(DirImportFiles(src), move = false)
        assertTrue(File(src, "Saves/GBA/New.srm").exists())
        assertEquals("new", File(sd, "Saves/GBA/New.srm").readText())
    }

    @Test fun a_zip_never_writes_outside_the_areas() {
        val sd = tmp.newFolder("sd")
        file(sd, ".userdata/android/ra/cache/login.json", "token")
        val bytes = zipOf(
            Triple("../escape.srm", "x", t0), Triple("/abs/x.srm", "x", t0),
            Triple(".userdata/android/ra/cache/login.json", "evil", t0), Triple("Saves/GBA/Ok.srm", "ok", t0),
        )
        var zipDeleted = false
        val summary = SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { zipDeleted = true; true }, move = true)
        assertEquals("token", File(sd, ".userdata/android/ra/cache/login.json").readText())
        assertFalse(File(sd.parentFile, "escape.srm").exists())
        assertEquals("ok", File(sd, "Saves/GBA/Ok.srm").readText())
        assertEquals(3, summary.ignored)
        assertFalse(zipDeleted)   // a zip goes only when every entry it holds was taken; ignored ones keep it
    }

    @Test fun a_zip_whose_entries_were_all_taken_is_deleted_on_move() {
        val sd = tmp.newFolder("sd"); file(sd, "Saves/GBA/Same.srm", "same", t0)
        val bytes = zipOf(Triple("Saves/GBA/Ok.srm", "ok", t0), Triple("Saves/GBA/Same.srm", "same", t0))
        var zipDeleted = false
        val summary = SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { zipDeleted = true; true }, move = true)
        assertTrue(zipDeleted)
        assertFalse(summary.zipNotDeleted)
    }

    @Test fun a_failed_entry_blocks_deleting_the_zip() {
        val sd = tmp.newFolder("sd")
        val bytes = zipOf(Triple("Saves/GBA/Ok.srm", "ok", t0), Triple("Saves/GBA/Bad.srm", "bad", t0))
        var zipDeleted = false
        val importer = SaveImporter(sd, rename = { a, b -> if (b.name == "Bad.srm") false else a.renameTo(b) })
        val summary = importer.run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { zipDeleted = true; true }, move = true)
        assertEquals(1, summary.failed)
        assertFalse(zipDeleted)
    }

    @Test fun a_zip_that_could_not_be_deleted_is_reported() {
        val sd = tmp.newFolder("sd")
        val bytes = zipOf(Triple("Saves/GBA/Ok.srm", "ok", t0))
        val summary = SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { false }, move = true)
        assertTrue(summary.zipNotDeleted)
        assertFalse(SaveImporter(tmp.newFolder("sd2")).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { false }, move = false).zipNotDeleted)
    }

    @Test fun a_zip_entry_is_streamed_not_buffered() {
        val bytes = zipOf(Triple("Saves/GBA/A.srm", "a", t0), Triple("Saves/GBA/B.srm", "b", t0))
        val entries = ZipImportFiles({ ByteArrayInputStream(bytes) }) { true }.entries().iterator()
        val first = entries.next()
        assertEquals("b", entries.next().open().readBytes().decodeToString())
        assertTrue(runCatching { first.open().read() }.exceptionOrNull() is java.io.IOException)   // valid only until the next entry
    }

    @Test fun a_large_zip_entry_round_trips() {
        val sd = tmp.newFolder("sd")
        val big = ByteArray(20 * 1024 * 1024).also { java.util.Random(7).nextBytes(it) }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry("Saves/PS/Big.mcd").apply { time = t0 }); z.write(big); z.closeEntry() }
        val summary = SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(out.toByteArray()) }) { true }, move = false)
        assertEquals(1, summary.saves)
        assertTrue(big.contentEquals(File(sd, "Saves/PS/Big.mcd").readBytes()))
    }

    /** One entry that offers [size] but whose stream gives [data]; [deleted] records a Move's delete. */
    private class OneEntry(val data: ByteArray, val size: Long?, val mtime: Long?) : ImportFiles {
        var deleted = false
        override fun entries() = sequenceOf(object : ImportEntry {
            override val path = "Saves/GBA/Short.srm"
            override val mtime = this@OneEntry.mtime
            override val size = this@OneEntry.size
            override fun open() = ByteArrayInputStream(data)
            override fun delete(): Boolean { deleted = true; return true }
        })
        override fun finish(moved: Boolean, allTaken: Boolean) = true
    }

    @Test fun a_copy_shorter_than_the_source_fails_and_keeps_the_source() {
        val sd = tmp.newFolder("sd")
        val source = OneEntry("12345".toByteArray(), size = 10, mtime = t0)
        val summary = SaveImporter(sd).run(source, move = true)
        assertEquals(1, summary.failed)
        assertFalse(source.deleted)
        assertFalse(File(sd, "Saves/GBA/Short.srm").exists())
        assertEquals(emptyList<String>(), File(sd, "Saves/GBA").list()!!.toList())   // no temp file left
    }

    @Test fun a_source_without_a_time_lands_now_and_never_replaces() {
        val sd = tmp.newFolder("sd"); val now = t0 + 86_400_000
        SaveImporter(sd, now = { now }).run(OneEntry("new".toByteArray(), size = 3, mtime = null), move = false)
        assertEquals(now / 1000, File(sd, "Saves/GBA/Short.srm").lastModified() / 1000)
        val other = OneEntry("other".toByteArray(), size = null, mtime = null)
        val summary = SaveImporter(sd, now = { now + 60_000 }).run(other, move = true)
        assertEquals(1, summary.skippedOlder)
        assertEquals("new", File(sd, "Saves/GBA/Short.srm").readText())
        assertFalse(other.deleted)
    }

    @Test fun a_move_removes_an_emptied_saves_folder_but_never_the_root_or_userdata() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/x.srm", "x"); file(src, ".userdata/shared/GBA-gpsp/x.state", "s")
        SaveImporter(sd).run(DirImportFiles(src), move = true)
        assertFalse(File(src, "Saves").exists())
        assertFalse(File(src, ".userdata/shared").exists())
        assertTrue(File(src, ".userdata").isDirectory)
        assertTrue(src.isDirectory)
    }

    @Test fun a_move_keeps_a_saves_folder_it_did_not_empty() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        File(src, "Saves").mkdirs()                                        // untouched by this run
        file(src, ".userdata/shared/GBA-gpsp/x.state", "s"); file(src, ".userdata/shared/game_time.db", "db")   // not emptied
        SaveImporter(sd).run(DirImportFiles(src), move = true)
        assertTrue(File(src, "Saves").isDirectory)
        assertTrue(File(src, ".userdata/shared").isDirectory)
        assertFalse(File(src, ".userdata/shared/GBA-gpsp").exists())
    }

    @Test fun a_zip_with_a_skipped_entry_is_kept_on_move() {
        val sd = tmp.newFolder("sd"); file(sd, "Saves/GBA/Older.srm", "mine", t0)
        val bytes = zipOf(Triple("Saves/GBA/Older.srm", "older", t0 - 60_000))
        var zipDeleted = false
        SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { zipDeleted = true; true }, move = true)
        assertFalse(zipDeleted)
    }

    @Test fun a_zip_with_only_ignored_entries_is_kept_on_move() {
        val sd = tmp.newFolder("sd")
        val bytes = zipOf(Triple("Pokemon.gb", "rom", t0))
        var zipDeleted = false
        SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { zipDeleted = true; true }, move = true)
        assertFalse(zipDeleted)
    }

    @Test fun a_file_that_is_not_a_zip_is_kept_on_move() {
        val sd = tmp.newFolder("sd")
        var zipDeleted = false
        SaveImporter(sd).run(ZipImportFiles({ ByteArrayInputStream("not a zip".toByteArray()) }) { zipDeleted = true; true }, move = true)
        assertFalse(zipDeleted)
    }

    @Test fun a_failed_replace_puts_the_private_save_back() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/Newer.srm", "newer", t0); file(sd, "Saves/GBA/Newer.srm", "old", t0 - 60_000)
        val summary = SaveImporter(sd, { t0 }, rename = { _, _ -> false }).run(DirImportFiles(src), move = true)
        assertEquals("old", File(sd, "Saves/GBA/Newer.srm").readText())
        assertEquals(listOf("Newer.srm"), File(sd, "Saves/GBA").list()!!.toList())
        assertTrue(File(src, "Saves/GBA/Newer.srm").exists())
        assertEquals(1, summary.failed)
    }

    @Test fun move_prunes_only_the_folders_it_emptied() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/x.srm", "x"); File(src, "Saves/GBC").mkdirs(); File(src, ".userdata/shared/.ra").mkdirs()
        SaveImporter(sd).run(DirImportFiles(src), move = true)
        assertFalse(File(src, "Saves/GBA").exists())
        assertTrue(File(src, "Saves/GBC").isDirectory)
        assertTrue(File(src, ".userdata/shared/.ra").isDirectory)
        assertTrue(File(src, "Saves").isDirectory)
    }

    @Test fun export_then_import_keeps_times_across_time_zones() {
        val a = tmp.newFolder("a"); val b = tmp.newFolder("b")
        val t = 1_700_000_001_000L
        file(a, "Saves/GBA/Game.srm", "sram", t)
        val saved = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            val zip = ByteArrayOutputStream().also { SaveExport.write(a, it) }.toByteArray()
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            SaveImporter(b).run(ZipImportFiles({ ByteArrayInputStream(zip) }) { true }, move = false)
        } finally {
            java.util.TimeZone.setDefault(saved)
        }
        assertEquals(t, File(b, "Saves/GBA/Game.srm").lastModified())
    }

    @Test fun export_then_import_round_trips() {
        val a = tmp.newFolder("a"); val b = tmp.newFolder("b")
        file(a, "Saves/GBC/Pokemon - Crystal.srm", "sram", t0)
        file(a, "Saves/GBC/Pokemon - Crystal.gbc.rtc", "rtc", t0)
        file(a, ".userdata/shared/GBC-gambatte/Pokemon - Crystal.state1", "state", t0)
        file(a, ".userdata/shared/.minui/GBC/Pokemon - Crystal.gbc.1.bmp", "png", t0)
        val zip = ByteArrayOutputStream().also { SaveExport.write(a, it) }.toByteArray()
        SaveImporter(b).run(ZipImportFiles({ ByteArrayInputStream(zip) }) { true }, move = false)
        for (p in listOf("Saves/GBC/Pokemon - Crystal.srm", "Saves/GBC/Pokemon - Crystal.gbc.rtc", ".userdata/shared/GBC-gambatte/Pokemon - Crystal.state1", ".userdata/shared/.minui/GBC/Pokemon - Crystal.gbc.1.bmp")) {
            assertEquals(p, File(a, p).readText(), File(b, p).readText())
            assertTrue(p, kotlin.math.abs(File(a, p).lastModified() - File(b, p).lastModified()) <= 2_000)
        }
    }

    @Test fun both_emulator_folders_merge_into_the_console_folder() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/A.srm", "a"); file(src, "Saves/MGBA/B.srm", "b")
        SaveImporter(sd).run(DirImportFiles(src), move = false)
        assertEquals("a", File(sd, "Saves/GBA/A.srm").readText())
        assertEquals("b", File(sd, "Saves/GBA/B.srm").readText())
        assertFalse(File(sd, "Saves/MGBA").exists())
    }

    @Test fun a_newer_save_under_another_format_replaces_and_backs_up() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "old", t0 - 60_000)
        file(src, "Saves/MGBA/Game.gba.sav", "new", t0)
        val summary = SaveImporter(sd, format = SaveFormat.RETROARCH).run(DirImportFiles(src), move = false)
        assertEquals("new", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(1, File(sd, "Saves/GBA").list()!!.count { SaveCopyRules.isBackupOf(it, "Game.srm") })
        assertEquals(1, summary.replaced)
    }

    @Test fun an_older_save_under_another_format_is_skipped() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "mine", t0)
        file(src, "Saves/MGBA/Game.gba.sav", "older", t0 - 60_000)
        val summary = SaveImporter(sd, format = SaveFormat.RETROARCH).run(DirImportFiles(src), move = false)
        assertEquals("mine", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(listOf("Game.srm"), File(sd, "Saves/GBA").list()!!.toList())
        assertEquals(1, summary.skippedOlder)
    }

    @Test fun a_save_under_another_name_is_renamed_to_the_phone_format_and_the_old_name_backed_up() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.gba.sav", "old", t0 - 60_000)
        file(src, "Saves/GBA/Game.srm", "new", t0)
        SaveImporter(sd, format = SaveFormat.RETROARCH).run(DirImportFiles(src), move = false)
        assertEquals("new", File(sd, "Saves/GBA/Game.srm").readText())
        assertFalse(File(sd, "Saves/GBA/Game.gba.sav").exists())
        assertEquals(1, File(sd, "Saves/GBA").list()!!.count { SaveCopyRules.isBackupOf(it, "Game.gba.sav") })
    }

    @Test fun a_compressed_phone_format_encodes_imported_saves() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/MGBA/Game.srm", "sram")
        SaveImporter(sd, format = SaveFormat.RETROARCH_COMPRESSED).run(DirImportFiles(src), move = false)
        val bytes = File(sd, "Saves/GBA/Game.srm").readBytes()
        assertTrue(String(bytes, 0, 6) == "#RZIPv")
        assertEquals("sram", String(Rzip.decode(bytes)!!))
    }

    @Test fun a_malformed_rzip_is_counted_failed_and_keeps_the_phone_save() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "mine", t0 - 60_000)
        val bad = Rzip.encode(ByteArray(1000) { it.toByte() }).copyOf(30)
        File(src, "Saves/GBA").mkdirs()
        File(src, "Saves/GBA/Game.srm").apply { writeBytes(bad); setLastModified(t0) }
        val summary = SaveImporter(sd).run(DirImportFiles(src), move = false)
        assertEquals("mine", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(1, summary.failed)
    }

    /** Follow-up 6: a Generic name under the MinUI format takes the ROM name from the phone's copy of the game. */
    @Test fun a_name_without_the_rom_extension_borrows_it_from_the_phones_copy() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "older", t0 - 120_000)
        file(sd, "Saves/GBA/Game.gba.sav", "old", t0 - 60_000)
        file(src, "Saves/MGBA/Game.sav", "new", t0)
        file(src, "Saves/GBA/Other.sav", "other", t0)
        SaveImporter(sd, format = SaveFormat.MINUI).run(DirImportFiles(src), move = false)
        assertEquals("new", File(sd, "Saves/GBA/Game.gba.sav").readText())
        assertFalse(File(sd, "Saves/GBA/Game.sav").exists())
        assertFalse(File(sd, "Saves/GBA/Game.srm").exists())
        // No phone copy carries a ROM name: the incoming name stays.
        assertEquals("other", File(sd, "Saves/GBA/Other.sav").readText())
    }

    /** Follow-up 6: folding two tag folders, the second name finds the ROM name the first one landed with. */
    @Test fun a_fold_of_tag_folders_lands_on_the_first_ones_rom_name() {
        val sd = tmp.newFolder("sd")
        val bytes = zipOf(Triple("Saves/GBA/Game.gba.sav", "old", t0 - 60_000), Triple("Saves/MGBA/Game.sav", "new", t0))
        SaveImporter(sd, format = SaveFormat.MINUI).run(ZipImportFiles({ ByteArrayInputStream(bytes) }) { true }, move = false)
        assertEquals("new", File(sd, "Saves/GBA/Game.gba.sav").readText())
        assertFalse(File(sd, "Saves/GBA/Game.sav").exists())
    }

    /** `Emerald.gba.sav` may be ROM `Emerald.gba.zip`'s Generic name: kept unless a phone copy tells the ROM. */
    @Test fun a_save_named_after_an_unknown_rom_file_keeps_its_name() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/Emerald.gba.sav", "new", t0)
        SaveImporter(sd, format = SaveFormat.GENERIC).run(DirImportFiles(src), move = false)
        assertEquals(listOf("Emerald.gba.sav"), File(sd, "Saves/GBA").list()!!.toList())

        val src2 = tmp.newFolder("home2"); val sd2 = tmp.newFolder("sd2")
        file(sd2, "Saves/GBA/Emerald.gba.zip.sav", "old", t0 - 60_000)
        file(src2, "Saves/MGBA/Emerald.gba.sav", "new", t0)
        SaveImporter(sd2, format = SaveFormat.RETROARCH).run(DirImportFiles(src2), move = false)
        assertEquals("new", File(sd2, "Saves/GBA/Emerald.gba.srm").readText())
        assertFalse(File(sd2, "Saves/GBA/Emerald.gba.zip.sav").exists())
    }
}
