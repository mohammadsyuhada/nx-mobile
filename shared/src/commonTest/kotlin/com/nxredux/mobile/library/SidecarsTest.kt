package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.MemorySidecarStore
import com.nxredux.mobile.storage.Recents
import com.nxredux.mobile.storage.SidecarStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SidecarsTest {
    private val home = ContentRef("Home")
    private val loose = ContentRef("sd/Loose")
    private val looseB = ContentRef("sd/Loose/b")

    /** An extra "sd/Loose" (id e1) with its own systems.txt and map.txt; [withCopy] adds a home copy under Roms/.sources/e1/. */
    private fun source(readOnly: Set<String> = emptySet(), withCopy: Boolean = false) = FakeRomsSource(
        dirs = buildMap {
            put("Home", listOf("Roms"))
            put("Home/Roms", if (withCopy) listOf(".sources") else emptyList<String>())
            if (withCopy) {
                put("Home/Roms/.sources", listOf("e1"))
                put("Home/Roms/.sources/e1", listOf("systems.txt", "map.txt"))
            }
            put("sd/Loose", listOf("a.chd", "b.chd", "c.chd", "b", "systems.txt", "map.txt"))
            put("sd/Loose/b", listOf("d.chd"))
        },
        texts = mapOf(
            "sd/Loose/systems.txt" to "a.chd\tMD\nb.chd\tMD\nc.chd\tMD\n",
            "sd/Loose/map.txt" to "a.chd\tAlpha\nb.chd\tBeta\n",
            "Home/Roms/.sources/e1/systems.txt" to "c.chd\tPCE\n",
            "Home/Roms/.sources/e1/map.txt" to "b.chd\tBravo\n",
        ),
        readOnly = readOnly,
    )

    @Test fun an_extra_reads_in_extra_then_home_copy_then_store() = runTest {
        val store = MemorySidecarStore().apply { map["e1||systems.txt"] = "b.chd\tPSP\nc.chd\tPSP\n" }
        val read = Sidecars(source(withCopy = true), store) { home }.read("e1", loose, "")
        assertEquals(mapOf("a.chd" to "MD", "b.chd" to "PSP", "c.chd" to "PSP"), read)   // store last: a stale home copy never hides a newer choice
    }

    @Test fun the_home_copy_beats_the_in_extra_file_when_the_store_is_empty() = runTest {
        val read = Sidecars(source(withCopy = true), MemorySidecarStore()) { home }.read("e1", loose, "")
        assertEquals(mapOf("a.chd" to "MD", "b.chd" to "MD", "c.chd" to "PCE"), read)
    }

    @Test fun the_home_source_reads_its_folder_then_the_store() = runTest {
        val src = FakeRomsSource(
            dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to listOf("Game Boy (GB)"), "Home/Roms/Game Boy (GB)" to listOf("x.gb", "y.gb", "systems.txt")),
            texts = mapOf("Home/Roms/Game Boy (GB)/systems.txt" to "x.gb\tGBC\ny.gb\tGB\n"),
        )
        val store = MemorySidecarStore().apply { map["home|Roms/Game Boy (GB)|systems.txt"] = "y.gb\tGBC\n" }
        val read = Sidecars(src, store) { home }.read(Recents.HOME, ContentRef("Home/Roms/Game Boy (GB)"), "Roms/Game Boy (GB)")
        assertEquals(mapOf("x.gb" to "GBC", "y.gb" to "GBC"), read)
    }

    @Test fun display_names_take_the_home_copy_over_the_extra() = runTest {
        val sidecars = Sidecars(source(withCopy = true), MemorySidecarStore()) { home }
        assertEquals(mapOf("a.chd" to "Alpha", "b.chd" to "Bravo"), sidecars.displayNames("e1", loose, ""))
        assertEquals(emptyMap<String, String>(), sidecars.displayNames("e1", looseB, "b"))   // no copy folder for b: nothing, no error
    }

    @Test fun set_on_an_extra_writes_the_home_copy_and_nothing_under_the_extra() = runTest {
        val src = source()   // the extra is writable here: the app must still not write there
        val store = MemorySidecarStore()
        assertTrue(Sidecars(src, store) { home }.set("e1", looseB, "b", "d.chd", "psp"))
        assertEquals("d.chd\tPSP\n", src.textAt("Home/Roms/.sources/e1/b/systems.txt"))
        assertEquals(
            listOf("Home/Roms/.sources", "Home/Roms/.sources/e1", "Home/Roms/.sources/e1/b", "Home/Roms/.sources/e1/b/systems.txt"),
            src.written,
        )
        assertTrue(store.map.isEmpty())
    }

    @Test fun the_home_copy_carries_the_in_extra_entries_and_the_extra_is_untouched() = runTest {
        val src = source()
        assertTrue(Sidecars(src, MemorySidecarStore()) { home }.set("e1", loose, "", "a.chd", "PSP"))
        assertEquals("a.chd\tPSP\nb.chd\tMD\nc.chd\tMD\n", src.textAt("Home/Roms/.sources/e1/systems.txt"))
        assertEquals("a.chd\tMD\nb.chd\tMD\nc.chd\tMD\n", src.textAt("sd/Loose/systems.txt"))
        assertTrue(src.written.none { it.startsWith("sd/") })
    }

    @Test fun a_read_only_home_writes_the_store_and_creates_no_sources_folder() = runTest {
        val src = source(readOnly = setOf("Home"))
        val store = MemorySidecarStore()
        assertTrue(Sidecars(src, store) { home }.set("e1", looseB, "b", "d.chd", "PSP"))
        assertEquals("d.chd\tPSP\n", store.map["e1|b|systems.txt"])
        assertEquals(emptyList<String>(), src.written)
        assertNull(src.child(ContentRef("Home/Roms"), ".sources"))
    }

    @Test fun a_later_home_write_rewrites_an_existing_store_copy() = runTest {
        val src = source()
        val store = MemorySidecarStore().apply { map["e1|b|systems.txt"] = "d.chd\tMD\n" }   // left by an earlier failed home write
        val sidecars = Sidecars(src, store) { home }
        assertTrue(sidecars.set("e1", looseB, "b", "e.chd", "PSP"))
        assertEquals("d.chd\tMD\ne.chd\tPSP\n", src.textAt("Home/Roms/.sources/e1/b/systems.txt"))
        assertEquals("d.chd\tMD\ne.chd\tPSP\n", store.map["e1|b|systems.txt"])
        assertEquals(mapOf("d.chd" to "MD", "e.chd" to "PSP"), sidecars.read("e1", looseB, "b"))
    }

    @Test fun without_a_home_the_store_takes_it_and_false_when_neither_does() = runTest {
        val store = MemorySidecarStore()
        assertTrue(Sidecars(source(), store) { null }.set("e1", looseB, "b", "d.chd", "PSP"))
        assertEquals("d.chd\tPSP\n", store.map["e1|b|systems.txt"])
        val refusing = object : SidecarStore {
            override suspend fun read(sourceId: String, relativeDir: String, file: String): String? = null
            override suspend fun write(sourceId: String, relativeDir: String, file: String, text: String): Boolean = false
            override suspend fun list(sourceId: String, relativeDir: String): List<String> = emptyList()
            override suspend fun delete(sourceId: String, relativeDir: String, file: String) = false
        }
        assertFalse(Sidecars(source(readOnly = setOf("Home")), refusing) { home }.set("e1", looseB, "b", "d.chd", "PSP"))
    }

    @Test fun a_home_folder_still_writes_its_own_systems_txt() = runTest {
        val src = FakeRomsSource(dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to listOf("Game Boy Advance (GBA)"), "Home/Roms/Game Boy Advance (GBA)" to listOf("Mario.gba")))
        val folder = ContentRef("Home/Roms/Game Boy Advance (GBA)")
        assertTrue(Sidecars(src, MemorySidecarStore()) { home }.set(Recents.HOME, folder, "Roms/Game Boy Advance (GBA)", "Mario.gba", "MGBA"))
        assertEquals(listOf("Home/Roms/Game Boy Advance (GBA)/systems.txt"), src.written)
        assertEquals("Mario.gba\tMGBA\n", src.textAt("Home/Roms/Game Boy Advance (GBA)/systems.txt"))
    }

    private fun homeWithGb(texts: Map<String, String> = emptyMap(), writable: Boolean = true) = FakeRomsSource(
        dirs = mapOf(
            "Home" to listOf("Roms"),
            "Home/Roms" to listOf("Game Boy (GB)", "PlayStation (PS)"),
            "Home/Roms/Game Boy (GB)" to (listOf("Tetris.gb", "Zelda.gb") + if ("Home/Roms/Game Boy (GB)/map.txt" in texts) listOf("map.txt") else emptyList()),
            "Home/Roms/PlayStation (PS)" to listOf("FF7"),
            "Home/Roms/PlayStation (PS)/FF7" to listOf("FF7.m3u", "FF7 (Disc 1).cue"),
        ),
        texts = texts, writable = writable,
    )

    @Test fun set_name_in_a_home_folder_merges_with_the_users_aliases() = runTest {
        val src = homeWithGb(texts = mapOf("Home/Roms/Game Boy (GB)/map.txt" to "Zelda.gb\tLink's Awakening\n"))
        val sidecars = Sidecars(src, MemorySidecarStore()) { home }
        assertTrue(sidecars.setName(Recents.HOME, "Roms/Game Boy (GB)", "Tetris.gb", "Tetris DX"))
        assertEquals("Tetris.gb\tTetris DX\nZelda.gb\tLink's Awakening\n", src.textAt("Home/Roms/Game Boy (GB)/map.txt"))
        assertEquals(mapOf("Tetris.gb" to "Tetris DX", "Zelda.gb" to "Link's Awakening"), sidecars.displayNames(Recents.HOME, ContentRef("Home/Roms/Game Boy (GB)"), "Roms/Game Boy (GB)"))
    }

    @Test fun set_name_for_a_home_disc_folder_writes_the_parent_folder_with_the_folder_name_key() = runTest {
        val src = homeWithGb()
        val sidecars = Sidecars(src, MemorySidecarStore()) { home }
        assertTrue(sidecars.setName(Recents.HOME, "Roms/PlayStation (PS)", "FF7", "Final Fantasy VII"))
        assertEquals("FF7\tFinal Fantasy VII\n", src.textAt("Home/Roms/PlayStation (PS)/map.txt"))
        assertNull(src.textAt("Home/Roms/PlayStation (PS)/FF7/map.txt"))
    }

    @Test fun set_name_for_an_extra_writes_the_sources_copy_and_never_the_extra() = runTest {
        val src = source()
        val sidecars = Sidecars(src, MemorySidecarStore()) { home }
        assertTrue(sidecars.setName("e1", "", "a.chd", "Alpha Prime"))
        assertEquals("a.chd\tAlpha Prime\n", src.textAt("Home/Roms/.sources/e1/map.txt"))
        assertTrue(src.written.none { it.startsWith("sd/") })
        assertEquals(mapOf("a.chd" to "Alpha Prime", "b.chd" to "Beta"), sidecars.displayNames("e1", loose, ""))   // the copy wins over the extra's own file
    }

    @Test fun set_name_falls_back_to_the_store_when_the_home_is_read_only_and_display_names_read_it_last() = runTest {
        val src = homeWithGb(writable = false)
        val store = MemorySidecarStore()
        val sidecars = Sidecars(src, store) { home }
        assertTrue(sidecars.setName(Recents.HOME, "Roms/Game Boy (GB)", "Tetris.gb", "Tetris DX"))
        assertNull(src.textAt("Home/Roms/Game Boy (GB)/map.txt"))
        assertEquals("Tetris.gb\tTetris DX\n", store.map["home|Roms/Game Boy (GB)|map.txt"])
        assertEquals(mapOf("Tetris.gb" to "Tetris DX"), sidecars.displayNames(Recents.HOME, ContentRef("Home/Roms/Game Boy (GB)"), "Roms/Game Boy (GB)"))
        src.writable = true
        assertTrue(sidecars.setName(Recents.HOME, "Roms/Game Boy (GB)", "Zelda.gb", "LA"))
        assertEquals("Tetris.gb\tTetris DX\nZelda.gb\tLA\n", src.textAt("Home/Roms/Game Boy (GB)/map.txt"))
        assertEquals("Tetris.gb\tTetris DX\nZelda.gb\tLA\n", store.map["home|Roms/Game Boy (GB)|map.txt"])   // the store is rewritten after a later home write
    }

    @Test fun set_name_without_a_home_fails() = runTest {
        assertFalse(Sidecars(homeWithGb(), MemorySidecarStore()) { null }.setName(Recents.HOME, "Roms/Game Boy (GB)", "Tetris.gb", "X"))
    }

    // A sidecar that is there but gives no text (SAF reports a failed read as null) is not an empty one: a rewrite from it
    // would replace every other line with the one set, so the write is dropped and the caller told.
    @Test fun set_never_rewrites_a_home_systems_txt_it_could_not_read() = runTest {
        val dir = "Home/Roms/Game Boy Advance (GBA)"
        val src = FakeRomsSource(
            dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to listOf("Game Boy Advance (GBA)"), dir to listOf("Mario.gba", "Zelda.gba", "systems.txt")),
            texts = mapOf("$dir/systems.txt" to "Zelda.gba\tMGBA\n"),
        )
        src.unreadable += "$dir/systems.txt"
        val store = MemorySidecarStore()
        assertFalse(Sidecars(src, store) { home }.set(Recents.HOME, ContentRef(dir), "Roms/Game Boy Advance (GBA)", "Mario.gba", "MGBA"))
        assertEquals(emptyList(), src.written)
        assertEquals("Zelda.gba\tMGBA\n", src.textAt("$dir/systems.txt"))
        assertTrue(store.map.isEmpty())
    }

    @Test fun set_never_rewrites_an_extras_home_copy_it_could_not_read() = runTest {
        val src = source(withCopy = true).apply { unreadable += "Home/Roms/.sources/e1/systems.txt" }
        assertFalse(Sidecars(src, MemorySidecarStore()) { home }.set("e1", loose, "", "a.chd", "PCE"))
        assertEquals(emptyList(), src.written)
        assertEquals("c.chd\tPCE\n", src.textAt("Home/Roms/.sources/e1/systems.txt"))
    }

    @Test fun set_name_never_rewrites_a_map_txt_it_could_not_read() = runTest {
        val src = homeWithGb(texts = mapOf("Home/Roms/Game Boy (GB)/map.txt" to "Zelda.gb\tLink's Awakening\n")).apply { unreadable += "Home/Roms/Game Boy (GB)/map.txt" }
        val store = MemorySidecarStore()
        assertFalse(Sidecars(src, store) { home }.setName(Recents.HOME, "Roms/Game Boy (GB)", "Tetris.gb", "Tetris DX"))
        assertEquals("Zelda.gb\tLink's Awakening\n", src.textAt("Home/Roms/Game Boy (GB)/map.txt"))
        assertTrue(store.map.isEmpty())
        val extra = source(withCopy = true).apply { unreadable += "Home/Roms/.sources/e1/map.txt" }
        assertFalse(Sidecars(extra, MemorySidecarStore()) { home }.setName("e1", "", "a.chd", "Alpha Prime"))
        assertEquals("b.chd\tBravo\n", extra.textAt("Home/Roms/.sources/e1/map.txt"))
    }

    @Test fun the_store_lists_its_files_per_folder() = runTest {
        val store = MemorySidecarStore()
        store.write("home", "Collections", "RPGs.txt", "/Roms/A (A)/a.gb\n")
        store.write("home", "Collections", "Racing.txt", "")
        store.write("e1", "", "systems.txt", "a\tMD\n")
        assertEquals(listOf("RPGs.txt", "Racing.txt"), store.list("home", "Collections"))
        assertEquals(emptyList<String>(), store.list("home", "Nope"))
    }
}
