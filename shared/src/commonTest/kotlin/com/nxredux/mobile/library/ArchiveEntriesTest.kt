package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveEntriesTest {
    private val gba = setOf("gba")

    @Test fun one_file_is_the_rom_and_a_folder_prefix_is_kept_on_the_entry_but_not_the_file_name() {
        assertEquals(ArchiveEntries.Pick.Single("Game.gba"), ArchiveEntries.pick(listOf("Game.gba"), gba))
        val nested = ArchiveEntries.pick(listOf("Sub/", "Sub/Game.gba"), gba) as ArchiveEntries.Pick.Single
        assertEquals("Sub/Game.gba", nested.entry)
        assertEquals("Game.gba", nested.fileName)
    }
    @Test fun junk_entries_are_not_roms() {
        for (exts in listOf(gba, emptySet(), setOf("txt"))) {
            assertEquals(ArchiveEntries.Pick.Single("x.gba"), ArchiveEntries.pick(listOf("__MACOSX/", "__MACOSX/._x.gba", "x.gba"), exts))
            assertEquals(ArchiveEntries.Pick.Single("x.gba"), ArchiveEntries.pick(listOf(".DS_Store", "x.gba"), exts))
            assertEquals(ArchiveEntries.Pick.Single("d/x.gba"), ArchiveEntries.pick(listOf("d/", "d/.hidden", "d/__MACOSX/._x.gba", "d/x.gba"), exts))
        }
    }
    @Test fun a_lone_entry_is_the_rom_whatever_its_extension() {
        assertEquals(ArchiveEntries.Pick.Single("readme.txt"), ArchiveEntries.pick(listOf("readme.txt"), gba))
    }
    @Test fun among_several_entries_the_one_with_a_core_extension_wins() {
        assertEquals(ArchiveEntries.Pick.Single("Game.gba"), ArchiveEntries.pick(listOf("Game.gba", "readme.txt"), gba))
        assertEquals(ArchiveEntries.Pick.Single("Game.gba"), ArchiveEntries.pick(listOf("Game.nfo", "Game.gba", "Game.txt"), setOf("gba", "bin")))
    }
    @Test fun none_or_several_roms_are_reported_with_the_candidate_names() {
        assertEquals(ArchiveEntries.Pick.Empty, ArchiveEntries.pick(emptyList(), gba))
        assertEquals(ArchiveEntries.Pick.Empty, ArchiveEntries.pick(listOf("only/", "__MACOSX/._a"), gba))
        assertEquals(ArchiveEntries.Pick.Many(listOf("a.gba", "b.gba")), ArchiveEntries.pick(listOf("a.gba", "b.gba"), gba))
        assertEquals(ArchiveEntries.Pick.Many(listOf("a.gba", "b.gba", "readme.txt")), ArchiveEntries.pick(listOf("a.gba", "b.gba", "readme.txt"), gba))
        assertEquals(ArchiveEntries.Pick.Many(listOf("readme.txt", "notes.nfo")), ArchiveEntries.pick(listOf("readme.txt", "notes.nfo"), gba))
        // Two ROM-extension entries: a disc sheet is still refused.
        val disc = ArchiveEntries.pick(listOf("Game.cue", "Game.bin"), setOf("cue", "bin", "gba"))
        assertEquals(ArchiveEntries.Pick.Many(listOf("Game.cue", "Game.bin")), disc)
        assertEquals(2, (disc as ArchiveEntries.Pick.Many).count)
        // Names are base names, junk is left out.
        assertEquals(ArchiveEntries.Pick.Many(listOf("a.gba", "b.gba")), ArchiveEntries.pick(listOf("d/", "d/a.gba", ".DS_Store", "d/b.gba"), gba))
    }

    @Test fun an_unpacked_rom_keeps_its_own_name_unless_the_core_names_saves_after_the_content() {
        assertEquals("disc.cdi", ArchiveEntries.unpackedName("Crazy Taxi (USA).zip", "disc.cdi", namedAfterArchive = false))
        // flycast names the VMU after the content: the unpacked file carries the archive's stem, so the VMU matches the game's saves.
        assertEquals("Crazy Taxi (USA).cdi", ArchiveEntries.unpackedName("Crazy Taxi (USA).zip", "disc.cdi", namedAfterArchive = true))
        // Only the last extension goes; the inner extension keeps its case.
        assertEquals("Game v1.0.CHD", ArchiveEntries.unpackedName("Game v1.0.7z", "GAME.CHD", namedAfterArchive = true))
        // An inner name with no extension is kept as it is.
        assertEquals("disc", ArchiveEntries.unpackedName("Game.zip", "disc", namedAfterArchive = true))
    }
}
