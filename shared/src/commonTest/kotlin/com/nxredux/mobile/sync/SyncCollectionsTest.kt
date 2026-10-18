package com.nxredux.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncCollectionsTest {
    private val fc = ConsolePair("Famicom (FC)", "Nintendo ES (FC)", "FC")
    private val gba = ConsolePair("Game Boy Advance (GBA)", "Game Boy Advance (GBA)", "GBA")
    private val mgba = "Game Boy Advance (MGBA)"

    /** A loose extra game `/Extra/<id>/<dir>/<file>`, landing as `<file>`. */
    private fun loose(extra: Int, id: String, dir: String, file: String, tag: String = "FC") =
        ExtraUnit(extra, tag, dir, listOf(file), file, ".sources/$id/$dir/$file", file, emptyList())

    /** A disc-folder extra game: folder `<dir>/<folder>` with sheet `<sheet>`, landing as `<folder>`. */
    private fun disc(extra: Int, id: String, dir: String, folder: String, sheet: String, tag: String = "PS") =
        ExtraUnit(extra, tag, dir, listOf(folder), folder, ".sources/$id/$dir/$folder/$sheet", "$folder/$sheet", emptyList())

    private val paths = CollectionPaths(pairs = listOf(fc))

    @Test fun a_paired_folder_is_renamed_both_ways() {
        assertEquals("/Roms/Nintendo ES (FC)/Mario.nes", paths.toPeer("/Roms/Famicom (FC)/Mario.nes"))
        assertEquals("/Roms/Famicom (FC)/Mario.nes", paths.fromPeer("/Roms/Nintendo ES (FC)/Mario.nes"))
        assertEquals("/Roms/Nintendo ES (FC)/Sub/Mario.nes", paths.toPeer("/Roms/Famicom (FC)/Sub/Mario.nes"))
    }

    @Test fun unpaired_folders_and_other_lines_travel_unchanged() {
        for (line in listOf("/Roms/Game Boy (GB)/Tetris.gb", "/Roms/Famicom (FC)", "/Roms/Famicom (FC)/", "/Apps/x.pak", "", "Tetris.gb",
            "/Extra/sd/Games/a.nes")) {
            assertEquals(line, paths.toPeer(line))
            assertEquals(line, paths.fromPeer(line))
        }
    }

    @Test fun no_pairing_leaves_every_line_unchanged() {
        val none = CollectionPaths()
        assertEquals("/Roms/Famicom (FC)/Mario.nes", none.toPeer("/Roms/Famicom (FC)/Mario.nes"))
        assertEquals("/Extra/sd/Games/a.nes", none.fromPeer("/Extra/sd/Games/a.nes"))
    }

    @Test fun this_phones_loose_extra_lands_in_the_peers_console_folder_and_back() {
        val p = CollectionPaths(pairs = listOf(fc), own = ExtrasManifest(units = listOf(loose(0, "sd", "Games/FC", "Zelda.nes"))))
        assertEquals("/Roms/Nintendo ES (FC)/Zelda.nes", p.toPeer("/Extra/sd/Games/FC/Zelda.nes"))
        assertEquals("/Extra/sd/Games/FC/Zelda.nes", p.fromPeer("/Roms/Nintendo ES (FC)/Zelda.nes"))
        // Another file of that extra folder is no unit: unchanged.
        assertEquals("/Extra/sd/Games/FC/Other.nes", p.toPeer("/Extra/sd/Games/FC/Other.nes"))
    }

    @Test fun a_home_copy_wins_over_an_extra_copy_on_the_receiver() {
        val p = CollectionPaths(pairs = listOf(fc), own = ExtrasManifest(units = listOf(loose(0, "sd", "Games", "Zelda.nes"))),
            homeNames = mapOf(fc.local to setOf("Zelda.nes")))
        assertEquals("/Roms/Famicom (FC)/Zelda.nes", p.fromPeer("/Roms/Nintendo ES (FC)/Zelda.nes"))
    }

    @Test fun a_disc_folder_extra_maps_its_sheet() {
        val ps = ConsolePair("PlayStation (PS)", "Sony PlayStation (PS)", "PS")
        val p = CollectionPaths(pairs = listOf(ps), own = ExtrasManifest(units = listOf(disc(1, "usb", "PS", "FF7", "FF7.m3u"))))
        assertEquals("/Roms/Sony PlayStation (PS)/FF7/FF7.m3u", p.toPeer("/Extra/usb/PS/FF7/FF7.m3u"))
        assertEquals("/Extra/usb/PS/FF7/FF7.m3u", p.fromPeer("/Roms/Sony PlayStation (PS)/FF7/FF7.m3u"))
    }

    @Test fun a_peer_phones_extra_lands_in_this_phones_console_folder_and_back() {
        val p = CollectionPaths(pairs = listOf(fc), peer = ExtrasManifest(units = listOf(loose(2, "card", "Roms", "Metroid.nes"))))
        assertEquals("/Roms/Famicom (FC)/Metroid.nes", p.fromPeer("/Extra/card/Roms/Metroid.nes"))
        assertEquals("/Extra/card/Roms/Metroid.nes", p.toPeer("/Roms/Famicom (FC)/Metroid.nes"))
    }

    @Test fun the_same_game_in_both_phones_extras_maps_extra_to_extra() {
        val p = CollectionPaths(pairs = listOf(fc), own = ExtrasManifest(units = listOf(loose(0, "sd", "A", "Zelda.nes"))),
            peer = ExtrasManifest(units = listOf(loose(1, "card", "B", "Zelda.nes"))))
        assertEquals("/Extra/card/B/Zelda.nes", p.toPeer("/Extra/sd/A/Zelda.nes"))
        assertEquals("/Extra/sd/A/Zelda.nes", p.fromPeer("/Extra/card/B/Zelda.nes"))
    }

    @Test fun an_unsafe_or_other_console_unit_is_no_match() {
        val p = CollectionPaths(pairs = listOf(fc), own = ExtrasManifest(units = listOf(loose(0, "sd", "G", "../x.nes"), loose(0, "sd", "G", "y.gb", tag = "GB"))))
        assertEquals("/Extra/sd/G/../x.nes", p.toPeer("/Extra/sd/G/../x.nes"))
        assertEquals("/Extra/sd/G/y.gb", p.toPeer("/Extra/sd/G/y.gb"))
    }

    @Test fun a_game_the_receiver_keeps_in_a_sibling_folder_maps_there() {
        // The peer keeps Emerald in its MGBA folder, not in the paired GBA one; this phone keeps Ruby in MGBA.
        val p = CollectionPaths(pairs = listOf(gba), localAll = listOf(gba.local, mgba), remoteAll = listOf(gba.remote, mgba),
            homeNames = mapOf(gba.local to setOf("Emerald.gba"), mgba to setOf("Ruby.gba")),
            remoteNames = mapOf(gba.remote to setOf("ruby.gba"), mgba to setOf("emerald.gba")))
        assertEquals("/Roms/$mgba/Emerald.gba", p.toPeer("/Roms/${gba.local}/Emerald.gba"))
        assertEquals("/Roms/${gba.local}/Emerald.gba", p.fromPeer("/Roms/$mgba/Emerald.gba"))
        assertEquals("/Roms/$mgba/Ruby.gba", p.fromPeer("/Roms/${gba.remote}/Ruby.gba"))
        assertEquals("/Roms/${gba.remote}/Ruby.gba", p.toPeer("/Roms/$mgba/Ruby.gba"))
        // Neither side has it: the paired folder, unchanged by the sibling rule.
        assertEquals("/Roms/${gba.remote}/Sapphire.gba", p.toPeer("/Roms/${gba.local}/Sapphire.gba"))
    }

    // A handheld peer's mapped pairs: one landing folder here, every emulator folder there.
    private val landing = "Game Boy Advance (GBA)"
    private val mapped = SyncConsoles.pairMapped(listOf(landing, mgba), listOf(landing, "GBA (MGBA)")).pairs

    @Test fun a_peer_line_from_a_second_emulator_folder_lands_in_the_console_folder() {
        val p = CollectionPaths(pairs = mapped, localAll = listOf(landing, mgba), remoteAll = listOf(landing, "GBA (MGBA)"), mapped = true,
            homeNames = mapOf(landing to setOf("Emerald.gba"), mgba to setOf("Ruby.gba")))
        assertEquals("/Roms/$landing/Emerald.gba", p.fromPeer("/Roms/GBA (MGBA)/Emerald.gba"))
        assertEquals("/Roms/$landing/Sapphire.gba", p.fromPeer("/Roms/GBA (MGBA)/Sapphire.gba"))
        assertEquals("/Roms/$landing/Sapphire.gba", p.fromPeer("/Roms/$landing/Sapphire.gba"))
        // A game this phone keeps in its leftover folder stays there.
        assertEquals("/Roms/$mgba/Ruby.gba", p.fromPeer("/Roms/GBA (MGBA)/Ruby.gba"))
    }

    @Test fun a_phone_line_goes_to_the_folder_holding_the_game_else_its_emulator() {
        val remoteMgba = "GBA (MGBA)"
        val p = CollectionPaths(pairs = mapped, localAll = listOf(landing, mgba), remoteAll = listOf(landing, remoteMgba), mapped = true,
            homeNames = mapOf(landing to setOf("A.gba", "B.gba", "C.gba", "D.gba"), mgba to setOf("E.gba")),
            remoteNames = mapOf(landing to setOf("a.gba"), remoteMgba to setOf("b.gba")),
            gameTags = mapOf(landing to mapOf("A.gba" to "MGBA", "B.gba" to "GBA", "C.gba" to "MGBA", "D.gba" to "GBA")))
        // Held there: the folder holding it, whatever its emulator here.
        assertEquals("/Roms/$landing/A.gba", p.toPeer("/Roms/$landing/A.gba"))
        assertEquals("/Roms/$remoteMgba/B.gba", p.toPeer("/Roms/$landing/B.gba"))
        // Held nowhere: the folder of its emulator.
        assertEquals("/Roms/$remoteMgba/C.gba", p.toPeer("/Roms/$landing/C.gba"))
        assertEquals("/Roms/$landing/D.gba", p.toPeer("/Roms/$landing/D.gba"))
        // No tag known: the console's default, whichever folder of the console holds the game (its tag names only the console).
        assertEquals("/Roms/$landing/Z.gba", p.toPeer("/Roms/$landing/Z.gba"))
        assertEquals("/Roms/$landing/E.gba", p.toPeer("/Roms/$mgba/E.gba"))
    }

    // Merge.

    @Test fun each_side_keeps_its_order_and_gets_the_others_new_lines_appended_translated() {
        val m = SyncCollections.merge(
            own = listOf("/Roms/Famicom (FC)/A.nes", "/Roms/Famicom (FC)/B.nes"),
            peer = listOf("/Roms/Nintendo ES (FC)/C.nes", "/Roms/Nintendo ES (FC)/A.nes", "/Roms/Game Boy (GB)/T.gb"),
            paths = paths,
        )
        assertEquals(listOf("/Roms/Famicom (FC)/A.nes", "/Roms/Famicom (FC)/B.nes", "/Roms/Famicom (FC)/C.nes", "/Roms/Game Boy (GB)/T.gb"), m.own)
        assertEquals(listOf("/Roms/Nintendo ES (FC)/C.nes", "/Roms/Nintendo ES (FC)/A.nes", "/Roms/Game Boy (GB)/T.gb", "/Roms/Nintendo ES (FC)/B.nes"), m.peer)
    }

    @Test fun nothing_new_on_a_side_leaves_it_unwritten() {
        val m = SyncCollections.merge(listOf("/Roms/Famicom (FC)/A.nes"), listOf("/Roms/Nintendo ES (FC)/A.nes", "/Roms/Nintendo ES (FC)/B.nes"), paths)
        assertEquals(listOf("/Roms/Famicom (FC)/A.nes", "/Roms/Famicom (FC)/B.nes"), m.own)
        assertNull(m.peer)
        val same = SyncCollections.merge(listOf("/Roms/Famicom (FC)/A.nes"), listOf("/Roms/Nintendo ES (FC)/A.nes"), paths)
        assertNull(same.own); assertNull(same.peer)
    }

    @Test fun duplicates_and_blank_lines_are_never_added() {
        val m = SyncCollections.merge(
            own = listOf("/Roms/Game Boy (GB)/A.gb"),
            peer = listOf("/Roms/Game Boy (GB)/B.gb", "", "  ", "/Roms/Game Boy (GB)/B.gb"),
            paths = paths,
        )
        assertEquals(listOf("/Roms/Game Boy (GB)/A.gb", "/Roms/Game Boy (GB)/B.gb"), m.own)
        // The peer's own blank lines stay where they are; nothing blank is appended.
        assertEquals(listOf("/Roms/Game Boy (GB)/B.gb", "", "  ", "/Roms/Game Boy (GB)/B.gb", "/Roms/Game Boy (GB)/A.gb"), m.peer)
    }

    @Test fun crlf_lines_equal_lf_lines() {
        assertEquals(listOf("/Roms/Game Boy (GB)/A.gb", "/Roms/Game Boy (GB)/B.gb"), SyncCollections.lines("/Roms/Game Boy (GB)/A.gb\r\n/Roms/Game Boy (GB)/B.gb\r\n"))
        assertEquals(listOf("a"), SyncCollections.lines("﻿a\n"))
        assertEquals(emptyList(), SyncCollections.lines(""))
        val m = SyncCollections.merge(listOf("/Roms/Game Boy (GB)/A.gb"), SyncCollections.lines("/Roms/Game Boy (GB)/A.gb\r\n"), paths)
        assertNull(m.own); assertNull(m.peer)
    }

    @Test fun a_collection_only_on_one_side_is_copied_translated() {
        val toOwn = SyncCollections.merge(own = null, peer = listOf("/Roms/Nintendo ES (FC)/A.nes", ""), paths = paths)
        assertEquals(listOf("/Roms/Famicom (FC)/A.nes"), toOwn.own); assertNull(toOwn.peer)
        val toPeer = SyncCollections.merge(own = listOf("/Roms/Famicom (FC)/A.nes"), peer = null, paths = paths)
        assertNull(toPeer.own); assertEquals(listOf("/Roms/Nintendo ES (FC)/A.nes"), toPeer.peer)
    }

    @Test fun an_empty_collection_is_copied_as_an_empty_file() {
        val m = SyncCollections.merge(own = null, peer = emptyList(), paths = paths)
        assertEquals(emptyList(), m.own); assertNull(m.peer)
        val both = SyncCollections.merge(own = emptyList(), peer = emptyList(), paths = paths)
        assertNull(both.own); assertNull(both.peer)
    }

    @Test fun merging_the_result_again_changes_nothing() {
        val p = CollectionPaths(pairs = listOf(fc, gba), localAll = listOf(fc.local, gba.local, mgba), remoteAll = listOf(fc.remote, gba.remote, mgba),
            own = ExtrasManifest(units = listOf(loose(0, "sd", "Games", "Zelda.nes"))),
            peer = ExtrasManifest(units = listOf(loose(1, "card", "R", "Metroid.nes"))),
            homeNames = mapOf(fc.local to setOf("Mario.nes"), gba.local to setOf("Emerald.gba"), mgba to setOf("Ruby.gba")),
            remoteNames = mapOf(fc.remote to setOf("mario.nes"), gba.remote to setOf("ruby.gba"), mgba to setOf("emerald.gba")))
        val own = listOf("/Extra/sd/Games/Zelda.nes", "/Roms/Famicom (FC)/Mario.nes", "/Roms/${gba.local}/Emerald.gba", "/Roms/Game Boy (GB)/T.gb")
        val peer = listOf("/Extra/card/R/Metroid.nes", "/Roms/${gba.remote}/Ruby.gba", "/Roms/Nintendo ES (FC)/Mario.nes", "/Apps/x")
        val first = SyncCollections.merge(own, peer, p)
        val own2 = first.own ?: own
        val peer2 = first.peer ?: peer
        val again = SyncCollections.merge(own2, peer2, p)
        assertNull(again.own); assertNull(again.peer)
    }

    @Test fun plan_matches_names_ignoring_case_and_renders_files() {
        val plan = SyncCollections.plan(
            own = mapOf("RPGs" to listOf("/Roms/Famicom (FC)/A.nes"), "Mine" to listOf("/Roms/Famicom (FC)/M.nes")),
            peerFiles = mapOf("rpgs.txt" to "/Roms/Nintendo ES (FC)/B.nes\r\n", "Theirs.txt" to "/Roms/Nintendo ES (FC)/T.nes\n",
                "map.txt" to "x\ty\n", ".hidden.txt" to "z\n", "Bad:Name.txt" to "q\n", "notes.md" to "n\n"),
            paths = paths,
        )
        assertEquals(mapOf("RPGs" to listOf("/Roms/Famicom (FC)/A.nes", "/Roms/Famicom (FC)/B.nes"), "Theirs" to listOf("/Roms/Famicom (FC)/T.nes")), plan.own)
        assertEquals(mapOf("rpgs.txt" to "/Roms/Nintendo ES (FC)/B.nes\n/Roms/Nintendo ES (FC)/A.nes\n", "Mine.txt" to "/Roms/Nintendo ES (FC)/M.nes\n"), plan.peer)
    }

    /** Case-insensitive volumes on both sides: `Favorites` and `favorites.txt` are one collection, each side keeping its name. */
    @Test fun names_pair_ignoring_case_each_side_keeping_its_spelling() {
        val plan = SyncCollections.plan(
            own = mapOf("Favorites" to listOf("/Roms/Game Boy (GB)/A.gb")),
            peerFiles = mapOf("favorites.txt" to "/Roms/Game Boy (GB)/B.gb\n"),
            paths = paths,
        )
        assertEquals(mapOf("Favorites" to listOf("/Roms/Game Boy (GB)/A.gb", "/Roms/Game Boy (GB)/B.gb")), plan.own)
        assertEquals(mapOf("favorites.txt" to "/Roms/Game Boy (GB)/B.gb\n/Roms/Game Boy (GB)/A.gb\n"), plan.peer)
    }

    @Test fun map_txt_in_any_case_is_no_collection() {
        val plan = SyncCollections.plan(
            own = emptyMap(),
            peerFiles = mapOf("map.txt" to "a\tb\n", "MAP.TXT" to "c\td\n", "Map.txt" to "e\tf\n"),
            paths = paths,
        )
        assertEquals(CollectionsPlan(emptyMap(), emptyMap()), plan)
    }

    /** This phone's names pass the same check as the peer's: `Map` is the alias map, `Bad:Name` would fail the push every sync. */
    @Test fun own_names_that_are_no_valid_collection_are_left_alone() {
        val plan = SyncCollections.plan(
            own = mapOf("Map" to listOf("x\ty"), "MAP" to listOf("z"), "Bad:Name" to listOf("q"), "Ok" to listOf("/Roms/Game Boy (GB)/O.gb")),
            peerFiles = emptyMap(),
            paths = paths,
        )
        assertEquals(mapOf("Ok.txt" to "/Roms/Game Boy (GB)/O.gb\n"), plan.peer)
        assertEquals(emptyMap(), plan.own)
    }

    /** A peer file that would not decode (not UTF-8) is left alone both ways, like an unreadable collection here. */
    @Test fun an_undecodable_peer_file_is_left_alone_both_ways() {
        val plan = SyncCollections.plan(
            own = mapOf("Faves" to listOf("/Roms/Game Boy (GB)/A.gb")),
            peerFiles = emptyMap(),
            paths = paths,
            peerUnreadable = setOf("faves.TXT"),
        )
        assertEquals(CollectionsPlan(emptyMap(), emptyMap()), plan)
    }

    @Test fun an_upper_case_extension_on_the_peer_keeps_its_spelling() {
        val plan = SyncCollections.plan(
            own = mapOf("faves" to listOf("/Roms/Game Boy (GB)/A.gb")),
            peerFiles = mapOf("Faves.TXT" to "/Roms/Game Boy (GB)/B.gb\n"),
            paths = paths,
        )
        assertEquals(mapOf("faves" to listOf("/Roms/Game Boy (GB)/A.gb", "/Roms/Game Boy (GB)/B.gb")), plan.own)
        assertEquals(mapOf("Faves.TXT" to "/Roms/Game Boy (GB)/B.gb\n/Roms/Game Boy (GB)/A.gb\n"), plan.peer)
    }

    /** A collection of this phone that would not read is neither written here nor pushed (merging from nothing would lose lines). */
    @Test fun an_unreadable_own_collection_is_left_alone_both_ways() {
        val plan = SyncCollections.plan(
            own = mapOf("Favorites" to null, "Mine" to listOf("/Roms/Game Boy (GB)/M.gb")),
            peerFiles = mapOf("favorites.txt" to "/Roms/Game Boy (GB)/B.gb\n"),
            paths = paths,
        )
        assertEquals(emptyMap(), plan.own)
        assertEquals(mapOf("Mine.txt" to "/Roms/Game Boy (GB)/M.gb\n"), plan.peer)
    }
}
