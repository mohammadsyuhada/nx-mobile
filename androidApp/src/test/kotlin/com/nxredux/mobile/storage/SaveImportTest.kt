package com.nxredux.mobile.storage

import com.nxredux.mobile.library.IndexedGame
import com.nxredux.mobile.library.IndexedSystem
import com.nxredux.mobile.library.LibraryIndex
import com.nxredux.mobile.runner.CoreRegistry
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

    /** A library of [systems]: console tag to its ROM file names, all in the home folder. */
    private fun index(vararg systems: Pair<String, List<String>>) = LibraryIndex(
        2, "home", emptyList(), emptyMap(),
        systems.map { (tag, roms) -> IndexedSystem(tag, tag, games = roms.map { IndexedGame(Recents.HOME, "Roms", it, "ref/$it", "", it) }) },
    )

    private val library = index("GBA" to listOf("Pokemon - Lazarus.gba", "Tetris.gba"), "GB" to listOf("Tetris.gb"))
    private val gb = CoreRegistry.consoleOf("GB")!!
    private val gba = CoreRegistry.consoleOf("GBA")!!

    @Test fun new_identical_replace_and_skip_from_a_folder() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/New.srm", "new")
        file(src, "Saves/GBA/Same.srm", "same"); file(sd, "Saves/GBA/Same.srm", "same", t0 - 50_000)
        file(src, "Saves/GBA/Newer.srm", "newer", t0); file(sd, "Saves/GBA/Newer.srm", "old", t0 - 60_000)
        file(src, "Saves/GBA/Older.srm", "older", t0 - 60_000); file(sd, "Saves/GBA/Older.srm", "mine", t0)
        file(src, ".userdata/shared/GBA-gpsp/New.state", "st")
        file(src, ".userdata/shared/game_time.db", "db")
        val summary = SaveImporter(sd, now = { t0 }).run(DirImportFiles(src))
        assertEquals("new", File(sd, "Saves/GBA/New.srm").readText())
        assertEquals(t0, File(sd, "Saves/GBA/New.srm").lastModified() / 1000 * 1000)
        assertEquals("newer", File(sd, "Saves/GBA/Newer.srm").readText())
        assertTrue(File(sd, "Saves/GBA").list()!!.any { SaveCopyRules.isBackupOf(it, "Newer.srm") })
        assertEquals("mine", File(sd, "Saves/GBA/Older.srm").readText())
        assertFalse(File(sd, ".userdata/shared/game_time.db").exists())
        // Copy only: every source stays.
        for (p in listOf("New.srm", "Same.srm", "Newer.srm", "Older.srm")) assertTrue(p, File(src, "Saves/GBA/$p").exists())
        assertTrue(File(src, ".userdata/shared/game_time.db").exists())
        val skipped = listOf(SkippedFile("Older.srm", SkipReason.PHONE_NEWER))
        assertEquals(ImportSummary(saves = 2, states = 1, identical = 1, replaced = 1, ignored = 1, skipped = skipped), summary)
    }

    /** `Saves/GPGX` holds several consoles' saves: each goes to its game's console by the library's ROM names, else Genesis. */
    @Test fun a_gpgx_save_goes_to_its_games_console() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GPGX/Alex Kidd.srm", "sms")
        file(src, "Saves/GPGX/Shinobi.srm", "gg")
        file(src, "Saves/GPGX/Sonic.srm", "md")
        file(src, "Saves/GPGX/Both.srm", "both")
        file(src, "Saves/GPGX/Unknown.srm", "unknown")
        val library = index(
            "SMS" to listOf("Alex Kidd.sms", "Both.sms"), "GG" to listOf("Shinobi.gg"), "MD" to listOf("Sonic.md", "Both.md"),
            "GBA" to listOf("Unknown.gba"),   // not a console the GPGX folder serves
        )
        SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals("sms", File(sd, "Saves/SMS/Alex Kidd.srm").readText())
        assertEquals("gg", File(sd, "Saves/GG/Shinobi.srm").readText())
        assertEquals("md", File(sd, "Saves/MD/Sonic.srm").readText())
        assertEquals("both", File(sd, "Saves/MD/Both.srm").readText())         // two consoles hold it: today's fallback
        assertEquals("unknown", File(sd, "Saves/MD/Unknown.srm").readText())   // held by none
    }

    @Test fun copy_never_deletes() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/New.srm", "new")
        SaveImporter(sd).run(DirImportFiles(src))
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
        val summary = SaveImporter(sd).run(ZipImportFiles { ByteArrayInputStream(bytes) })
        assertEquals("token", File(sd, ".userdata/android/ra/cache/login.json").readText())
        assertFalse(File(sd.parentFile, "escape.srm").exists())
        assertEquals("ok", File(sd, "Saves/GBA/Ok.srm").readText())
        assertEquals(3, summary.ignored)
    }

    @Test fun a_failed_entry_is_listed_and_the_rest_imports() {
        val sd = tmp.newFolder("sd")
        val bytes = zipOf(Triple("Saves/GBA/Ok.srm", "ok", t0), Triple("Saves/GBA/Bad.srm", "bad", t0))
        val importer = SaveImporter(sd, rename = { a, b -> if (b.name == "Bad.srm") false else a.renameTo(b) })
        val summary = importer.run(ZipImportFiles { ByteArrayInputStream(bytes) })
        assertEquals(listOf(SkippedFile("Bad.srm", SkipReason.UNREADABLE)), summary.skipped)
        assertEquals(1, summary.saves)
    }

    @Test fun a_zip_entry_is_streamed_not_buffered() {
        val bytes = zipOf(Triple("Saves/GBA/A.srm", "a", t0), Triple("Saves/GBA/B.srm", "b", t0))
        val entries = ZipImportFiles { ByteArrayInputStream(bytes) }.entries().iterator()
        val first = entries.next()
        assertEquals("b", entries.next().open().readBytes().decodeToString())
        assertTrue(runCatching { first.open().read() }.exceptionOrNull() is java.io.IOException)   // valid only until the next entry
    }

    @Test fun a_large_zip_entry_round_trips() {
        val sd = tmp.newFolder("sd")
        val big = ByteArray(20 * 1024 * 1024).also { java.util.Random(7).nextBytes(it) }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry("Saves/PS/Big.mcd").apply { time = t0 }); z.write(big); z.closeEntry() }
        val summary = SaveImporter(sd).run(ZipImportFiles { ByteArrayInputStream(out.toByteArray()) })
        assertEquals(1, summary.saves)
        assertTrue(big.contentEquals(File(sd, "Saves/PS/Big.mcd").readBytes()))
    }

    /** One entry that offers [size] but whose stream gives [data]. */
    private class OneEntry(val data: ByteArray, val size: Long?, val mtime: Long?) : ImportFiles {
        override fun entries() = sequenceOf(object : ImportEntry {
            override val path = "Saves/GBA/Short.srm"
            override val mtime = this@OneEntry.mtime
            override val size = this@OneEntry.size
            override fun open() = ByteArrayInputStream(data)
        })
    }

    @Test fun a_copy_shorter_than_the_source_fails_and_keeps_the_source() {
        val sd = tmp.newFolder("sd")
        val source = OneEntry("12345".toByteArray(), size = 10, mtime = t0)
        val summary = SaveImporter(sd).run(source)
        assertEquals(listOf(SkippedFile("Short.srm", SkipReason.UNREADABLE)), summary.skipped)
        assertFalse(File(sd, "Saves/GBA/Short.srm").exists())
        assertEquals(emptyList<String>(), File(sd, "Saves/GBA").list()!!.toList())   // no temp file left
    }

    @Test fun a_source_without_a_time_lands_now_and_never_replaces() {
        val sd = tmp.newFolder("sd"); val now = t0 + 86_400_000
        SaveImporter(sd, now = { now }).run(OneEntry("new".toByteArray(), size = 3, mtime = null))
        assertEquals(now / 1000, File(sd, "Saves/GBA/Short.srm").lastModified() / 1000)
        val other = OneEntry("other".toByteArray(), size = null, mtime = null)
        val summary = SaveImporter(sd, now = { now + 60_000 }).run(other)
        assertEquals(listOf(SkippedFile("Short.srm", SkipReason.PHONE_NEWER)), summary.skipped)
        assertEquals("new", File(sd, "Saves/GBA/Short.srm").readText())
    }

    @Test fun a_failed_replace_puts_the_private_save_back() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/Newer.srm", "newer", t0); file(sd, "Saves/GBA/Newer.srm", "old", t0 - 60_000)
        val summary = SaveImporter(sd, { t0 }, rename = { _, _ -> false }).run(DirImportFiles(src))
        assertEquals("old", File(sd, "Saves/GBA/Newer.srm").readText())
        assertEquals(listOf("Newer.srm"), File(sd, "Saves/GBA").list()!!.toList())
        assertTrue(File(src, "Saves/GBA/Newer.srm").exists())
        assertEquals(listOf(SkippedFile("Newer.srm", SkipReason.UNREADABLE)), summary.skipped)
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
            SaveImporter(b).run(ZipImportFiles { ByteArrayInputStream(zip) })
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
        SaveImporter(b).run(ZipImportFiles { ByteArrayInputStream(zip) })
        for (p in listOf("Saves/GBC/Pokemon - Crystal.srm", "Saves/GBC/Pokemon - Crystal.gbc.rtc", ".userdata/shared/GBC-gambatte/Pokemon - Crystal.state1", ".userdata/shared/.minui/GBC/Pokemon - Crystal.gbc.1.bmp")) {
            assertEquals(p, File(a, p).readText(), File(b, p).readText())
            assertTrue(p, kotlin.math.abs(File(a, p).lastModified() - File(b, p).lastModified()) <= 2_000)
        }
    }

    @Test fun both_emulator_folders_merge_into_the_console_folder() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/A.srm", "a"); file(src, "Saves/MGBA/B.srm", "b")
        SaveImporter(sd).run(DirImportFiles(src))
        assertEquals("a", File(sd, "Saves/GBA/A.srm").readText())
        assertEquals("b", File(sd, "Saves/GBA/B.srm").readText())
        assertFalse(File(sd, "Saves/MGBA").exists())
    }

    @Test fun a_newer_save_under_another_format_replaces_and_backs_up() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "old", t0 - 60_000)
        file(src, "Saves/MGBA/Game.gba.sav", "new", t0)
        val summary = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH)).run(DirImportFiles(src))
        assertEquals("new", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(1, File(sd, "Saves/GBA").list()!!.count { SaveCopyRules.isBackupOf(it, "Game.srm") })
        assertEquals(1, summary.replaced)
    }

    @Test fun an_older_save_under_another_format_is_skipped() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "mine", t0)
        file(src, "Saves/MGBA/Game.gba.sav", "older", t0 - 60_000)
        val summary = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH)).run(DirImportFiles(src))
        assertEquals("mine", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(listOf("Game.srm"), File(sd, "Saves/GBA").list()!!.toList())
        assertEquals(listOf(SkippedFile("Game.gba.sav", SkipReason.PHONE_NEWER)), summary.skipped)
    }

    @Test fun a_save_under_another_name_is_renamed_to_the_phone_format_and_the_old_name_backed_up() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.gba.sav", "old", t0 - 60_000)
        file(src, "Saves/GBA/Game.srm", "new", t0)
        SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH)).run(DirImportFiles(src))
        assertEquals("new", File(sd, "Saves/GBA/Game.srm").readText())
        assertFalse(File(sd, "Saves/GBA/Game.gba.sav").exists())
        assertEquals(1, File(sd, "Saves/GBA").list()!!.count { SaveCopyRules.isBackupOf(it, "Game.gba.sav") })
    }

    @Test fun a_compressed_phone_format_encodes_imported_saves() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/MGBA/Game.srm", "sram")
        SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH_COMPRESSED)).run(DirImportFiles(src))
        val bytes = File(sd, "Saves/GBA/Game.srm").readBytes()
        assertTrue(String(bytes, 0, 6) == "#RZIPv")
        assertEquals("sram", String(Rzip.decode(bytes)!!))
    }

    @Test fun a_malformed_rzip_is_listed_unreadable_and_keeps_the_phone_save() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "mine", t0 - 60_000)
        val bad = Rzip.encode(ByteArray(1000) { it.toByte() }).copyOf(30)
        File(src, "Saves/GBA").mkdirs()
        File(src, "Saves/GBA/Game.srm").apply { writeBytes(bad); setLastModified(t0) }
        val summary = SaveImporter(sd).run(DirImportFiles(src))
        assertEquals("mine", File(sd, "Saves/GBA/Game.srm").readText())
        assertEquals(listOf(SkippedFile("Game.srm", SkipReason.UNREADABLE)), summary.skipped)
    }

    /** Follow-up 6: a Generic name under the MinUI format takes the ROM name from the phone's copy of the game. */
    @Test fun a_name_without_the_rom_extension_borrows_it_from_the_phones_copy() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(sd, "Saves/GBA/Game.srm", "older", t0 - 120_000)
        file(sd, "Saves/GBA/Game.gba.sav", "old", t0 - 60_000)
        file(src, "Saves/MGBA/Game.sav", "new", t0)
        file(src, "Saves/GBA/Other.sav", "other", t0)
        SaveImporter(sd, naming = SaveNaming(SaveFormat.MINUI)).run(DirImportFiles(src))
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
        SaveImporter(sd, naming = SaveNaming(SaveFormat.MINUI)).run(ZipImportFiles { ByteArrayInputStream(bytes) })
        assertEquals("new", File(sd, "Saves/GBA/Game.gba.sav").readText())
        assertFalse(File(sd, "Saves/GBA/Game.sav").exists())
    }

    /** `Emerald.gba.sav` may be ROM `Emerald.gba.zip`'s Generic name: kept unless a phone copy tells the ROM. */
    @Test fun a_save_named_after_an_unknown_rom_file_keeps_its_name() {
        val src = tmp.newFolder("home"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/Emerald.gba.sav", "new", t0)
        SaveImporter(sd, naming = SaveNaming(SaveFormat.GENERIC)).run(DirImportFiles(src))
        assertEquals(listOf("Emerald.gba.sav"), File(sd, "Saves/GBA").list()!!.toList())

        val src2 = tmp.newFolder("home2"); val sd2 = tmp.newFolder("sd2")
        file(sd2, "Saves/GBA/Emerald.gba.zip.sav", "old", t0 - 60_000)
        file(src2, "Saves/MGBA/Emerald.gba.sav", "new", t0)
        SaveImporter(sd2, naming = SaveNaming(SaveFormat.RETROARCH)).run(DirImportFiles(src2))
        assertEquals("new", File(sd2, "Saves/GBA/Emerald.gba.srm").readText())
        assertFalse(File(sd2, "Saves/GBA/Emerald.gba.zip.sav").exists())
    }

    @Test fun retroarch_core_folder_save_lands_in_console_folder() {
        val src = tmp.newFolder("ra"); val sd = tmp.newFolder("sd")
        file(src, "saves/mGBA/Pokemon - Lazarus.srm", "sram")
        val summary = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals("sram", File(sd, "Saves/GBA/Pokemon - Lazarus.srm").readText())
        assertEquals(1, summary.saves)
        assertTrue(File(src, "saves/mGBA/Pokemon - Lazarus.srm").exists())
    }

    @Test fun flat_dump_matches_by_name() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        file(src, "Pokemon - Lazarus.srm", "sram")
        val summary = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals("sram", File(sd, "Saves/GBA/Pokemon - Lazarus.srm").readText())
        assertEquals(1, summary.saves)
    }

    @Test fun rzip_save_is_decoded() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        val raw = ByteArray(4096) { (it % 7).toByte() }
        File(src, "Pokemon - Lazarus.srm").apply { writeBytes(Rzip.encode(raw)); setLastModified(t0) }
        SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertTrue(raw.contentEquals(File(sd, "Saves/GBA/Pokemon - Lazarus.srm").readBytes()))
    }

    @Test fun minui_format_names_after_library_rom() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        file(src, "Pokemon - Lazarus.srm", "sram")
        SaveImporter(sd, naming = SaveNaming(SaveFormat.MINUI), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals(listOf("Pokemon - Lazarus.gba.sav"), File(sd, "Saves/GBA").list()!!.toList())
        assertEquals("sram", File(sd, "Saves/GBA/Pokemon - Lazarus.gba.sav").readText())
    }

    @Test fun unmatched_and_ambiguous_are_listed() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        file(src, "Zelda.srm", "z"); file(src, "Tetris.srm", "t"); file(src, "saves/Gambatte/Tetris.srm", "gb")
        val summary = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library)).run(DirImportFiles(src))
        val both = "${gb.displayName} and ${gba.displayName}"
        assertEquals("Game Boy and Game Boy Advance", both)
        assertEquals(
            setOf(SkippedFile("Zelda.srm", SkipReason.NO_GAME), SkippedFile("Tetris.srm", SkipReason.AMBIGUOUS, both)),
            summary.skipped.toSet(),
        )
        assertEquals("gb", File(sd, "Saves/GB/Tetris.srm").readText())
        assertFalse(File(sd, "Saves/GBA/Tetris.srm").exists())
        assertEquals(1, summary.saves)
    }

    @Test fun rom_and_config_files_are_only_counted() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        file(src, "Roms/GBA/Pokemon - Lazarus.gba", "rom"); file(src, "config/retroarch.cfg", "cfg")
        val summary = SaveImporter(sd, matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals(ImportSummary(ignored = 2), summary)
        assertEquals(emptyList<String>(), sd.list()!!.toList())
    }

    @Test fun reimport_is_all_already_here() {
        val src = tmp.newFolder("dump"); val sd = tmp.newFolder("sd")
        file(src, "Pokemon - Lazarus.srm", "sram")
        val importer = SaveImporter(sd, naming = SaveNaming(SaveFormat.RETROARCH), matcher = SaveMatcher(library))
        importer.run(DirImportFiles(src))
        val again = importer.run(DirImportFiles(src))
        assertEquals(1, again.identical)
        assertEquals(0, again.saves)
    }

    @Test fun nextui_layout_still_by_path() {
        val src = tmp.newFolder("card"); val sd = tmp.newFolder("sd")
        file(src, "Saves/GBA/NotInLibrary.srm", "s")
        val summary = SaveImporter(sd, matcher = SaveMatcher(library)).run(DirImportFiles(src))
        assertEquals("s", File(sd, "Saves/GBA/NotInLibrary.srm").readText())
        assertEquals(ImportSummary(saves = 1), summary)
    }

    @Test fun dir_import_walks_only_the_given_tops() {
        val src = tmp.newFolder("ra")
        file(src, "top.srm", "a"); file(src, "saves/mGBA/x.srm", "b"); file(src, "states/mGBA/x.state", "c"); file(src, "config/x.cfg", "d")
        assertEquals(setOf("top.srm", "saves/mGBA/x.srm", "states/mGBA/x.state", "config/x.cfg"), DirImportFiles(src).entries().map { it.path }.toSet())
        assertEquals(setOf("saves/mGBA/x.srm", "states/mGBA/x.state"), DirImportFiles(src, listOf("saves", "states")).entries().map { it.path }.toSet())
    }
}
