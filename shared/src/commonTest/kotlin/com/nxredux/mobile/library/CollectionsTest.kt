package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.MemorySidecarStore
import com.nxredux.mobile.storage.SidecarStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CollectionsTest {
    private val home = ContentRef("Home")
    private val a = "/Roms/Game Boy (GB)/a.gb"
    private val b = "/Roms/Game Boy (GB)/b.gb"

    private fun source(withFolder: Boolean = true, writable: Boolean = true) = FakeRomsSource(
        dirs = buildMap {
            put("Home", if (withFolder) listOf("Roms", "Collections") else listOf("Roms"))
            put("Home/Roms", emptyList())
            if (withFolder) put("Home/Collections", listOf("RPGs.txt", "racing.TXT", "map.txt", ".hidden.txt", "Old.txt.disabled", "notes.md", "Sub"))
            if (withFolder) put("Home/Collections/Sub", emptyList())
        },
        texts = mapOf("Home/Collections/RPGs.txt" to "$a\n$b\n", "Home/Collections/racing.TXT" to "$a\n"),
        writable = writable,
    )

    @Test fun names_are_the_txt_files_minus_hidden_ones_plus_the_store_sorted_ignoring_case() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|Zeta.txt"] = "" }
        assertEquals(listOf("racing", "RPGs", "Zeta"), Collections(source(), store) { home }.names())
        assertEquals(emptyList<String>(), Collections(source(withFolder = false), MemorySidecarStore()) { home }.names())
        assertEquals(emptyList<String>(), Collections(source(), MemorySidecarStore()) { null }.names())
    }

    @Test fun a_hidden_collections_listing_is_reported_not_read_as_no_collections() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|Zeta.txt"] = "" }
        val hidden = Collections(source().apply { listingFault = ListingFault.UNAVAILABLE }, store) { home }
        assertEquals(CollectionNames(listOf("Zeta"), listed = false), hidden.namesChecked())
        assertEquals(listOf("Zeta"), hidden.names())                                  // display callers get what is known
        assertEquals(CollectionNames(listOf("racing", "RPGs"), listed = true), Collections(source(), MemorySidecarStore()) { home }.namesChecked())
        assertEquals(CollectionNames(emptyList(), listed = true), Collections(source(withFolder = false), MemorySidecarStore()) { home }.namesChecked())
    }

    @Test fun a_failed_home_read_refuses_add_and_remove_without_writing() = runTest {
        val src = source()
        val failing = object : RomsSource by src {
            override suspend fun readText(file: ContentRef): String? =
                if (file.value == "Home/Collections/RPGs.txt") throw IllegalStateException("boom") else src.readText(file)
        }
        val store = MemorySidecarStore()
        val c = Collections(failing, store) { home }
        assertEquals(emptyList<String>(), c.read("RPGs"))                 // lenient for display
        assertEquals(CollectionWrite.FAILED, c.add("RPGs", "/Roms/Game Boy (GB)/c.gb"))
        assertFalse(c.remove("RPGs", a))
        assertEquals(emptyList<String>(), src.written)                    // the intact home file is not overwritten
        assertTrue(store.map.isEmpty())                                   // and no one-line store copy shadows it
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))
        val unreadable = source()                                         // SAF reports a failed read as null, not a throw
        val quiet = object : RomsSource by unreadable {
            override suspend fun readText(file: ContentRef): String? = if (file.value == "Home/Collections/RPGs.txt") null else unreadable.readText(file)
        }
        val c2 = Collections(quiet, MemorySidecarStore()) { home }
        assertEquals(CollectionWrite.FAILED, c2.add("RPGs", "/Roms/Game Boy (GB)/c.gb"))
        assertFalse(c2.remove("RPGs", a))
        assertEquals(emptyList<String>(), unreadable.written)
    }

    // Device Sync's writes.

    private val c = "/Roms/Game Boy (GB)/c.gb"

    @Test fun a_sync_write_on_the_base_it_read_writes_the_lines() = runTest {
        val src = source()
        assertTrue(Collections(src, MemorySidecarStore()) { home }.writeMerged("RPGs", listOf(a, b, c), base = listOf(a, b)))
        assertEquals("$a\n$b\n$c\n", src.textAt("Home/Collections/RPGs.txt"))
        assertTrue(Collections(src, MemorySidecarStore()) { home }.writeMerged("New", listOf(c), base = null))
        assertEquals("$c\n", src.textAt("Home/Collections/New.txt"))
    }

    /** "New" for the merge, but `rpgs.txt` (any case) is there: merged into, never replaced. */
    @Test fun a_sync_write_never_replaces_a_collection_that_reads_otherwise() = runTest {
        val src = source()
        assertTrue(Collections(src, MemorySidecarStore()) { home }.writeMerged("rpgs", listOf(c, a), base = null))
        assertEquals("$a\n$b\n$c\n", src.textAt("Home/Collections/RPGs.txt"))
        assertNull(src.textAt("Home/Collections/rpgs.txt"))
    }

    @Test fun a_sync_write_refuses_an_unreadable_collection() = runTest {
        val src = source()
        val failing = object : RomsSource by src {
            override suspend fun readText(file: ContentRef): String? = if (file.value == "Home/Collections/RPGs.txt") null else src.readText(file)
        }
        assertFalse(Collections(failing, MemorySidecarStore()) { home }.writeMerged("RPGs", listOf(c), base = null))
        assertEquals(emptyList<String>(), src.written)
    }

    /** The daemon replaced a home file: a store copy (read first) gets the home lines it lacks; none of its own is lost. */
    @Test fun a_store_copy_takes_in_a_home_file_written_from_outside() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|RPGs.txt"] = "$c\n$a\n" }
        val col = Collections(source(), store) { home }
        col.storeFollowsHome("RPGs")
        assertEquals("$c\n$a\n$b\n", store.map["home|Collections|RPGs.txt"])
        col.storeFollowsHome("racing")   // no store copy: none is made
        assertNull(store.map["home|Collections|racing.txt"])
    }

    /** Before a server phone serves `Collections/`: each store copy that differs is folded into its home file (union). */
    @Test fun store_copies_fold_into_home_before_serving() = runTest {
        val src = source()
        val store = MemorySidecarStore().apply {
            map["home|Collections|RPGs.txt"] = "$c\n$a\n"     // differs: c is only here
            map["home|Collections|racing.txt"] = "$a\n"      // the same as home: left alone
            map["home|Collections|Zeta.txt"] = "$b\n"        // only in the store
        }
        Collections(src, store) { home }.foldStoreIntoHome()
        assertEquals("$a\n$b\n$c\n", src.textAt("Home/Collections/RPGs.txt"))
        assertEquals("$a\n$b\n$c\n", store.map["home|Collections|RPGs.txt"])
        assertEquals("$b\n", src.textAt("Home/Collections/Zeta.txt"))
        assertFalse(src.written.any { it.contains("racing") })
    }

    @Test fun read_takes_the_store_when_present_else_the_home_file() = runTest {
        val store = MemorySidecarStore()
        val c = Collections(source(), store) { home }
        assertEquals(listOf(a, b), c.read("RPGs"))
        store.map["home|Collections|RPGs.txt"] = "$b\n"
        assertEquals(listOf(b), c.read("RPGs"))
        assertEquals(emptyList<String>(), c.read("Nope"))
    }

    @Test fun a_name_listed_from_an_upper_case_extension_reads_and_writes_that_same_file() = runTest {
        val src = source()
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(listOf(a), c.read("racing"))
        assertEquals(CollectionWrite.ADDED, c.add("racing", b))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/racing.TXT"))
        assertNull(src.textAt("Home/Collections/racing.txt"))
        assertTrue(c.remove("racing", a))
        assertEquals("$b\n", src.textAt("Home/Collections/racing.TXT"))
        assertNull(src.textAt("Home/Collections/racing.txt"))
    }

    @Test fun an_unavailable_listing_still_writes_the_existing_home_file_by_name() = runTest {
        val src = FakeRomsSource(
            dirs = mapOf("Home" to listOf("Roms", "Collections"), "Home/Roms" to emptyList(), "Home/Collections" to listOf("Favs.txt")),
            texts = mapOf("Home/Collections/Favs.txt" to "$a\n"),
        )
        src.listingFault = ListingFault.UNAVAILABLE
        val store = MemorySidecarStore()
        val c = Collections(src, store) { home }
        assertEquals(CollectionWrite.ADDED, c.add("Favs", b))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/Favs.txt"))
        assertEquals(listOf("Home/Collections/Favs.txt"), src.written)
        assertTrue(store.map.isEmpty())
    }

    @Test fun add_appends_once_creating_the_folder_and_file_on_first_use() = runTest {
        val src = source(withFolder = false)
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionWrite.ADDED, c.add("Favourites", a))
        assertEquals("$a\n", src.textAt("Home/Collections/Favourites.txt"))
        assertEquals(CollectionWrite.ALREADY, c.add("Favourites", a))
        assertEquals(CollectionWrite.ADDED, c.add("Favourites", b))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/Favourites.txt"))
        assertEquals(listOf("Favourites"), c.names())
    }

    @Test fun remove_rewrites_the_file_and_keeps_an_empty_one() = runTest {
        val src = source()
        val c = Collections(src, MemorySidecarStore()) { home }
        assertTrue(c.remove("RPGs", a))
        assertEquals("$b\n", src.textAt("Home/Collections/RPGs.txt"))
        assertTrue(c.remove("RPGs", b))
        assertEquals("", src.textAt("Home/Collections/RPGs.txt"))
        assertTrue(c.names().contains("RPGs"))
    }

    @Test fun a_read_only_home_falls_back_to_the_store_and_a_later_home_write_refreshes_it() = runTest {
        val src = source(writable = false)
        val store = MemorySidecarStore()
        val c = Collections(src, store) { home }
        assertEquals(CollectionWrite.ADDED, c.add("RPGs", "/Roms/Game Boy (GB)/c.gb"))
        assertEquals("$a\n$b\n/Roms/Game Boy (GB)/c.gb\n", store.map["home|Collections|RPGs.txt"])
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))                 // untouched
        src.writable = true
        assertTrue(c.remove("RPGs", a))
        assertEquals("$b\n/Roms/Game Boy (GB)/c.gb\n", src.textAt("Home/Collections/RPGs.txt"))
        assertEquals("$b\n/Roms/Game Boy (GB)/c.gb\n", store.map["home|Collections|RPGs.txt"])
    }

    @Test fun no_home_means_failed_writes_only_when_the_store_refuses_too() = runTest {
        val refusing = object : com.nxredux.mobile.storage.SidecarStore {
            override suspend fun read(sourceId: String, relativeDir: String, file: String): String? = null
            override suspend fun write(sourceId: String, relativeDir: String, file: String, text: String) = false
            override suspend fun list(sourceId: String, relativeDir: String): List<String> = emptyList()
            override suspend fun delete(sourceId: String, relativeDir: String, file: String) = false
        }
        assertEquals(CollectionWrite.FAILED, Collections(source(), refusing) { null }.add("X", a))
        assertFalse(Collections(source(), refusing) { null }.remove("X", a))
        assertEquals(CollectionWrite.ADDED, Collections(source(), MemorySidecarStore()) { null }.add("X", a))   // the store took it
    }

    // --- Rename and delete ---

    @Test fun rename_moves_the_home_file_in_one_step_keeping_its_lines_and_order() = runTest {
        val src = CountingRomsSource(source())
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("RPGs", "Role Playing"))
        assertEquals(listOf("rename:Home/Collections/RPGs.txt>Role Playing.txt"), src.calls.filter { it.startsWith("rename:") || it.startsWith("writeText:") || it.startsWith("delete:") })
        assertEquals(listOf("racing", "Role Playing"), c.names())
        assertEquals(listOf(a, b), c.read("Role Playing"))
    }

    @Test fun rename_takes_an_upper_case_extension_file_and_names_the_new_one_txt() = runTest {
        val src = source()
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("racing", "Racers"))
        assertEquals("$a\n", src.textAt("Home/Collections/Racers.txt"))
        assertNull(src.textAt("Home/Collections/racing.TXT"))
    }

    @Test fun rename_refuses_a_name_another_collection_has_ignoring_case() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|Zeta.txt"] = "$a\n" }
        val src = source()
        val c = Collections(src, store) { home }
        assertEquals(CollectionRename.EXISTS, c.rename("RPGs", "RACING"))
        assertEquals(CollectionRename.EXISTS, c.rename("RPGs", "zeta"))          // a store-only collection counts too
        assertEquals(listOf("racing", "RPGs", "Zeta"), c.names())
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))
    }

    @Test fun a_case_only_rename_goes_through_a_temporary_name() = runTest {
        val src = CountingRomsSource(source())
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("RPGs", "rpgs"))
        assertEquals(
            listOf("rename:Home/Collections/RPGs.txt>rpgs.txt.part", "rename:Home/Collections/rpgs.txt.part>rpgs.txt"),
            src.calls.filter { it.startsWith("rename:") },
        )
        assertEquals(listOf("racing", "rpgs"), c.names())
        assertEquals(listOf(a, b), c.read("rpgs"))
    }

    @Test fun rename_moves_the_store_copy_too_and_a_store_only_collection() = runTest {
        val store = MemorySidecarStore().apply {
            map["home|Collections|RPGs.txt"] = "$b\n"
            map["home|Collections|Zeta.txt"] = "$a\n"
        }
        val src = source()
        val c = Collections(src, store) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("RPGs", "Quests"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/Quests.txt"))
        assertEquals("$b\n", store.map["home|Collections|Quests.txt"])
        assertNull(store.map["home|Collections|RPGs.txt"])
        assertEquals(CollectionRename.RENAMED, c.rename("Zeta", "Omega"))
        assertEquals(mapOf("home|Collections|Quests.txt" to "$b\n", "home|Collections|Omega.txt" to "$a\n"), store.map)
        assertEquals(listOf("Omega", "Quests", "racing"), c.names())
    }

    @Test fun without_a_one_step_rename_the_file_is_copied_then_the_old_one_deleted() = runTest {
        val src = source().apply { renameUnsupported = true }
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("RPGs", "Quests"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/Quests.txt"))
        assertNull(src.textAt("Home/Collections/RPGs.txt"))
        assertEquals(CollectionRename.FAILED, c.rename("Quests", "quests"))   // a case-only rename needs the real one
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/Quests.txt"))
    }

    @Test fun a_read_only_home_fails_the_rename_and_changes_nothing() = runTest {
        val src = source(writable = false)
        val store = MemorySidecarStore()
        val c = Collections(src, store) { home }
        assertEquals(CollectionRename.FAILED, c.rename("RPGs", "Quests"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))
        assertTrue(store.map.isEmpty())
        assertEquals(CollectionRename.FAILED, c.rename("Nope", "Other"))      // nothing to rename
    }

    @Test fun delete_removes_only_the_collection_file_and_its_store_copy() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|RPGs.txt"] = "$b\n"; map["home|Collections|Zeta.txt"] = "" }
        val src = source()
        val c = Collections(src, store) { home }
        assertTrue(c.delete("RPGs"))
        assertNull(src.textAt("Home/Collections/RPGs.txt"))
        assertNull(store.map["home|Collections|RPGs.txt"])
        assertEquals("$a\n", src.textAt("Home/Collections/racing.TXT"))
        assertEquals(listOf("racing", "Zeta"), c.names())
        assertTrue(c.delete("Zeta"))                                      // a store-only collection
        assertTrue(c.delete("racing"))                                    // an upper-case extension
        assertEquals(emptyList<String>(), c.names())
        assertFalse(c.delete("Nope"))
    }

    @Test fun a_read_only_home_refuses_the_delete_and_keeps_the_store_copy() = runTest {
        val store = MemorySidecarStore().apply { map["home|Collections|RPGs.txt"] = "$b\n" }
        val src = source(writable = false)
        val c = Collections(src, store) { home }
        assertFalse(c.delete("RPGs"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))
        assertEquals("$b\n", store.map["home|Collections|RPGs.txt"])
    }

    // --- Failed renames leave nothing behind ---

    @Test fun a_case_only_rename_whose_last_steps_fail_is_recovered_on_the_next_listing() = runTest {
        val src = source()
        val stuck = object : RomsSource by src {   // every rename out of a .part fails: the last step and the way back
            override suspend fun rename(dir: ContentRef, from: String, to: String) = if (from.endsWith(".part")) false else src.rename(dir, from, to)
        }
        assertEquals(CollectionRename.FAILED, Collections(stuck, MemorySidecarStore()) { home }.rename("RPGs", "rpgs"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/rpgs.txt.part"))   // the leftover the recovery is for
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(listOf("racing", "rpgs"), c.names())                  // listed again: put back as a collection
        assertNull(src.textAt("Home/Collections/rpgs.txt.part"))
        assertEquals(listOf(a, b), c.read("rpgs"))
    }

    @Test fun a_leftover_part_beside_a_file_of_its_name_is_left_alone() = runTest {
        val src = source()
        src.writeText(ContentRef("Home/Collections"), "Racing.txt.part", "$b\n")   // racing.TXT is there
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(listOf("racing", "RPGs"), c.names())
        assertEquals("$b\n", src.textAt("Home/Collections/Racing.txt.part"))
        assertEquals("$a\n", src.textAt("Home/Collections/racing.TXT"))
    }

    @Test fun a_rename_first_recovers_a_leftover_part() = runTest {
        val src = source()
        src.writeText(ContentRef("Home/Collections"), "Zeta.txt.part", "$a\n")
        val c = Collections(src, MemorySidecarStore()) { home }
        assertEquals(CollectionRename.RENAMED, c.rename("Zeta", "Omega"))   // found once the part was put back
        assertEquals("$a\n", src.textAt("Home/Collections/Omega.txt"))
        assertNull(src.textAt("Home/Collections/Zeta.txt.part"))
    }

    @Test fun a_failed_copy_write_deletes_the_empty_file_it_created() = runTest {
        val src = source().apply { renameUnsupported = true }
        val failing = object : RomsSource by src {   // the provider creates the file, then the write fails
            override suspend fun writeText(dir: ContentRef, name: String, text: String): Boolean { src.writeText(dir, name, ""); return false }
        }
        assertEquals(CollectionRename.FAILED, Collections(failing, MemorySidecarStore()) { home }.rename("RPGs", "Quests"))
        assertNull(src.textAt("Home/Collections/Quests.txt"))
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))
        assertEquals(listOf("racing", "RPGs"), Collections(src, MemorySidecarStore()) { home }.names())
    }

    @Test fun a_store_copy_that_will_not_delete_is_retried_then_the_rename_is_undone() = runTest {
        val mem = MemorySidecarStore().apply { map["home|Collections|RPGs.txt"] = "$b\n"; map["home|Collections|Zeta.txt"] = "$a\n" }
        val tries = ArrayList<String>()
        val store = object : SidecarStore by mem {   // the old copies refuse to go; the new ones delete
            override suspend fun delete(sourceId: String, relativeDir: String, file: String): Boolean {
                tries += file
                return if (file == "RPGs.txt" || file == "Zeta.txt") false else mem.delete(sourceId, relativeDir, file)
            }
        }
        val src = source()
        val c = Collections(src, store) { home }
        assertEquals(CollectionRename.FAILED, c.rename("Zeta", "Omega"))   // store only
        assertEquals(listOf("Zeta.txt", "Zeta.txt", "Omega.txt"), tries)  // one retry, then the new copy goes again
        assertEquals(CollectionRename.FAILED, c.rename("RPGs", "Quests"))   // home and store
        assertEquals("$a\n$b\n", src.textAt("Home/Collections/RPGs.txt"))  // the home file is put back
        assertNull(src.textAt("Home/Collections/Quests.txt"))
        assertEquals(mapOf("home|Collections|RPGs.txt" to "$b\n", "home|Collections|Zeta.txt" to "$a\n"), mem.map)
        assertEquals(listOf("racing", "RPGs", "Zeta"), c.names())         // no duplicate, nothing lost
    }
}
