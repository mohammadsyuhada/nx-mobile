package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.settings.ExtraFolder
import com.nxredux.mobile.storage.Recents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LibraryIndexTest {
    private val gbaDir = "Roms/Game Boy Advance (GBA)"

    private fun home(dir: String, file: String, name: String = Names.stripExtension(file), box2d: String? = null, lookupDir: String = dir) =
        IndexedGame(Recents.HOME, dir, file, "Home/$dir/$file", "Home/$dir", name, box2d = box2d, lookupDir = lookupDir)

    private fun loose(dir: String, file: String): IndexedGame {
        val folder = if (dir.isEmpty()) "sd/X" else "sd/X/$dir"
        return IndexedGame("x", dir, file, "$folder/$file", folder, Names.stripExtension(file))
    }

    private val index = LibraryIndex(
        version = LibraryIndex.VERSION, home = "Home", extras = listOf(IndexedExtra("x", "sd/X")), defaultTags = emptyMap(),
        systems = listOf(
            IndexedSystem("C64", "Commodore 64", "Commodore 64 (C64)", games = listOf(home("Roms/Commodore 64 (C64)", "Boulder Dash.chd"))),
            IndexedSystem(
                "GBA", "Game Boy Advance", "Game Boy Advance (GBA)", art = "Home/Roms/.media/Game Boy Advance (GBA).png",
                games = listOf(home(gbaDir, "Mario.gba", box2d = "Home/$gbaDir/.media/box2d/Mario.png"), home(gbaDir, "Zelda.gba")),
            ),
            IndexedSystem("MGBA", "Game Boy Advance", "Game Boy Advance (MGBA)", games = emptyList()),
            IndexedSystem("NDS", "Nintendo DS", "Nintendo DS (NDS)", games = listOf(home("Roms/Nintendo DS (NDS)", "Contra 4.nds", name = "Contra IV"))),
            IndexedSystem("", LibraryScanner.UNASSIGNED, unassigned = true, games = listOf(loose("a/b", "deep.chd"), loose("", "thing.chd"))),
        ),
    )

    @Test fun to_systems_derives_cores_and_refs_with_no_source() {
        val systems = index.toSystems()
        assertEquals(listOf("C64", "GBA", "MGBA", "NDS", ""), systems.map { it.tag })
        assertNull(systems[0].core)                                         // Commodore 64 has no core
        val gba = systems[1]
        assertEquals(CoreRegistry.forTag("GBA"), gba.core)
        assertEquals(ContentRef("Home/Roms/.media/Game Boy Advance (GBA).png"), gba.artRef)
        assertEquals("Game Boy Advance (GBA)", gba.folderName)
        assertEquals(
            GameEntry(
                ref = ContentRef("Home/$gbaDir/Mario.gba"), fileName = "Mario.gba", shownName = "Mario",
                box2dRef = ContentRef("Home/$gbaDir/.media/box2d/Mario.png"), folder = ContentRef("Home/$gbaDir"),
                sourceId = Recents.HOME, relativeDir = gbaDir, subtitle = null,
            ),
            gba.games[0],
        )
        assertEquals("Contra IV", systems[3].games.single().shownName)
        assertNull(systems[4].core)
    }

    @Test fun the_subtitle_is_the_folder_in_unassigned_and_the_stored_one_elsewhere() {
        val systems = index.toSystems()
        assertEquals(listOf("a/b", "/"), systems.last().games.map { it.subtitle })
        assertTrue(systems.dropLast(1).flatMap { it.games }.all { it.subtitle == null })
        val copy = home("Roms/Nintendo DS (NDS)", "Contra 4.nds").copy(subtitle = "My NDS")
        val withCopy = index.copy(systems = index.systems.map { if (it.tag == "NDS") it.copy(games = listOf(copy)) else it })
        assertEquals("My NDS", withCopy.toSystems().first { it.tag == "NDS" }.games.single().subtitle)
    }

    @Test fun playable_hides_empty_and_coreless_systems_but_keeps_unassigned() {
        assertEquals(listOf("GBA", "NDS", ""), index.playableSystems().map { it.tag })
    }

    @Test fun the_fixture_is_in_library_order() {
        assertEquals(index.systems, index.systems.reversed().inLibraryOrder())
    }

    private fun gameIn(tag: String, file: String): Pair<SystemFolder, GameEntry> {
        val s = index.toSystems().first { it.tag == tag }
        return s to s.games.first { it.fileName == file }
    }

    @Test fun move_to_an_existing_home_system_keeps_art_and_order() {
        val (gba, mario) = gameIn("GBA", "Mario.gba")
        val moved = index.move(mario, gba, "MGBA")
        assertEquals(listOf("Zelda.gba"), moved.systems.first { it.tag == "GBA" }.games.map { it.file })
        assertEquals(listOf(home(gbaDir, "Mario.gba", box2d = "Home/$gbaDir/.media/box2d/Mario.png")), moved.systems.first { it.tag == "MGBA" }.games)
        assertEquals(index.systems.map { it.tag }, moved.systems.map { it.tag })
        assertEquals(index.copy(systems = emptyList()), moved.copy(systems = emptyList()))   // home, extras and default tags untouched
    }

    @Test fun move_to_a_missing_tag_creates_the_system_from_the_console_and_keeps_unassigned_last() {
        val (unassigned, thing) = gameIn("", "thing.chd")
        val moved = index.move(thing, unassigned, "psp")
        assertEquals(IndexedSystem("PSP", "Sony Playstation Portable", games = listOf(loose("", "thing.chd"))), moved.systems.first { it.tag == "PSP" })
        assertEquals(listOf("C64", "GBA", "MGBA", "NDS", "PSP", ""), moved.systems.map { it.tag })
        assertEquals(listOf("deep.chd"), moved.systems.last().games.map { it.file })
    }

    @Test fun moving_the_last_unassigned_game_removes_unassigned() {
        val (u1, thing) = gameIn("", "thing.chd")
        val once = index.move(thing, u1, "PSP")
        val u2 = once.toSystems().last()
        val twice = once.move(u2.games.single(), u2, "PSP")
        assertTrue(twice.systems.none { it.unassigned })
        assertEquals(listOf("deep.chd", "thing.chd"), twice.systems.first { it.tag == "PSP" }.games.map { it.file })
    }

    @Test fun an_emptied_home_system_is_kept_at_zero_and_the_target_re_sorts() {
        val (nds, contra) = gameIn("NDS", "Contra 4.nds")
        val moved = index.move(contra, nds, "GBA")
        assertEquals(IndexedSystem("NDS", "Nintendo DS", "Nintendo DS (NDS)", games = emptyList()), moved.systems.first { it.tag == "NDS" })
        assertEquals(listOf("Contra IV", "Mario", "Zelda"), moved.systems.first { it.tag == "GBA" }.games.map { it.name })
        assertTrue(moved.playableSystems().none { it.tag == "NDS" })
    }

    @Test fun an_unknown_tag_is_named_after_itself() {
        val (gba, zelda) = gameIn("GBA", "Zelda.gba")
        val moved = index.move(zelda, gba, "XYZ")
        assertEquals("XYZ", moved.systems.first { it.tag == "XYZ" }.displayName)
        assertEquals(listOf("C64", "GBA", "MGBA", "NDS", "XYZ", ""), moved.systems.map { it.tag })
    }

    @Test fun a_game_not_in_the_from_system_changes_nothing() {
        val (gba, mario) = gameIn("GBA", "Mario.gba")
        assertSame(index, index.move(mario.copy(fileName = "Gone.gba"), gba, "MGBA"))   // the same instance: AppState rescans on it
        val nds = index.toSystems().first { it.tag == "NDS" }
        assertSame(index, index.move(mario, nds, "MGBA"))
    }

    @Test fun fits_compares_home_extras_as_a_set_and_default_tags() {
        val extras = listOf(ExtraFolder("a", ContentRef("sd/A")), ExtraFolder("b", ContentRef("sd/B")))
        val ix = index.copy(extras = extras.map { IndexedExtra(it.id, it.ref.value) }, defaultTags = mapOf("gba" to "MGBA"))
        val home = ContentRef("Home")
        assertTrue(ix.fits(home, extras, mapOf("gba" to "MGBA")))
        assertTrue(ix.fits(home, extras.reversed(), mapOf("gba" to "MGBA")))
        assertFalse(ix.fits(ContentRef("Home2"), extras, mapOf("gba" to "MGBA")))
        assertFalse(ix.fits(home, extras.take(1), mapOf("gba" to "MGBA")))
        assertFalse(ix.fits(home, listOf(ExtraFolder("a", ContentRef("sd/A2")), extras[1]), mapOf("gba" to "MGBA")))   // same id, other folder
        assertFalse(ix.fits(home, extras, emptyMap()))
    }

    @Test fun rename_changes_the_shown_name_and_resorts_the_console() {
        val zelda = index.toSystems()[1].games.first { it.fileName == "Zelda.gba" }
        val renamed = index.rename(zelda, "A Link to the Past")
        assertEquals(listOf("A Link to the Past", "Mario"), renamed.systems[1].games.map { it.name })
        assertEquals("Zelda.gba", renamed.systems[1].games[0].file)
        assertSame(index, index.rename(zelda.copy(fileName = "nope.gba"), "X"))   // unknown game: unchanged instance
    }

    @Test fun hidden_paths_drop_games_and_an_all_hidden_console_leaves_the_playable_list() {
        val hidden = setOf("/Roms/Nintendo DS (NDS)/Contra 4.nds", "/Roms/Game Boy Advance (GBA)/Zelda.gba")
        val systems = index.playableSystems(hidden)
        assertEquals(listOf("GBA", ""), systems.map { it.tag })                    // NDS hidden away; C64 coreless; MGBA empty
        assertEquals(listOf("Mario"), systems[0].games.map { it.shownName })
        assertEquals(listOf("C64", "GBA", "MGBA", "NDS", ""), index.toSystems(hidden).map { it.tag })   // toSystems keeps the consoles
        assertEquals(emptyList<String>(), index.toSystems(hidden)[3].games.map { it.shownName })
    }

    @Test fun unassigned_with_every_game_hidden_leaves_the_playable_list() {
        val loose = index.toSystems().single { it.unassigned }.games.map { it.path }.toSet()
        assertTrue(index.playableSystems(loose).none { it.unassigned })
        assertTrue(index.toSystems(loose).single { it.unassigned }.games.isEmpty())   // still in the index: unhide restores it
        assertTrue(index.playableSystems(loose - loose.first()).any { it.unassigned })
    }

    @Test fun lookup_dir_and_path_reach_the_game_entry() {
        val ff7 = home("Roms/PlayStation (PS)/FF7", "FF7.m3u", lookupDir = "Roms/PlayStation (PS)")
        val ix = index.copy(systems = index.systems + IndexedSystem("PS", "PlayStation", "PlayStation (PS)", games = listOf(ff7)))
        val entry = ix.toSystems().first { it.tag == "PS" }.games.single()
        assertEquals("Roms/PlayStation (PS)", entry.lookupDir)
        assertEquals("Roms/PlayStation (PS)/FF7", entry.relativeDir)
        assertEquals("/Roms/PlayStation (PS)/FF7/FF7.m3u", entry.path)
        assertEquals(gbaDir, index.toSystems()[1].games[0].lookupDir)              // default: the game's own dir
    }
}
