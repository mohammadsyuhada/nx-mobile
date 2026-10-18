package com.nxredux.mobile.settings

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.FileRefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourcesTest {
    private val home = ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux")
    @Test fun a_tree_overlaps_itself_its_parent_and_its_children() {
        assertTrue(Sources.overlaps(home, home))
        assertTrue(Sources.overlaps(home, ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux%2FRoms")))
        assertTrue(Sources.overlaps(ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux%2FRoms"), home))
        assertFalse(Sources.overlaps(home, ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux2")))
        assertFalse(Sources.overlaps(home, ContentRef("content://com.android.externalstorage.documents/tree/1234-5678%3AROMs")))
        assertTrue(Sources.overlaps(ContentRef("sd/A"), ContentRef("sd/A/B")))
        // A volume root ("primary:") contains every tree on that volume; its children have no '/' after the colon.
        val root = ContentRef("content://com.android.externalstorage.documents/tree/primary%3A")
        assertTrue(Sources.overlaps(root, home))
        assertTrue(Sources.overlaps(home, root))
        assertFalse(Sources.overlaps(root, ContentRef("content://com.android.externalstorage.documents/tree/1234-5678%3AROMs")))
    }
    @Test fun fnv1a_matches_the_published_vectors() {
        assertEquals("811c9dc5", Sources.fnv1a32(""))
        assertEquals("e40c292c", Sources.fnv1a32("a"))
        assertEquals("bf9cf968", Sources.fnv1a32("foobar"))
    }

    @Test fun ids_are_the_sanitised_name_and_the_hash_of_the_ref() {
        val roms = ContentRef("content://com.android.externalstorage.documents/tree/primary%3AROMs")
        assertEquals("ROMs_Backup-116ad198", Sources.idFor("ROMs Backup", roms))
        assertEquals("x-116ad198", Sources.idFor("../x", roms))
        assertEquals("folder-116ad198", Sources.idFor("日本", roms))
        assertEquals("a_b-116ad198", Sources.idFor("-_a  b_-", roms))
        assertEquals("A".repeat(32) + "-116ad198", Sources.idFor("A".repeat(40), roms))
        assertEquals("Loose-d116b9e3", Sources.idFor("Loose", ContentRef("sd/Loose")))
    }

    @Test fun the_same_ref_gives_the_same_id_and_same_named_folders_differ() {
        assertEquals(Sources.idFor("ROMs", ContentRef("sd/ROMs")), Sources.idFor("ROMs", ContentRef("sd/ROMs")))
        val a = Sources.idFor("ROMs", ContentRef("sd/ROMs"))
        val b = Sources.idFor("ROMs", ContentRef("usb/ROMs"))
        assertNotEquals(a, b)
        assertTrue(a.startsWith("ROMs-") && b.startsWith("ROMs-"))
    }

    @Test fun a_taken_id_gets_the_next_free_suffix() {
        assertEquals("A-1", Sources.uniqueId("A-1", emptyList()))
        assertEquals("A-1-2", Sources.uniqueId("A-1", listOf("A-1")))
        assertEquals("A-1-3", Sources.uniqueId("A-1", listOf("A-1", "A-1-2")))
    }

    @Test fun a_tree_ref_reads_as_the_path_a_file_manager_shows() {
        val t = "content://com.android.externalstorage.documents/tree/"
        assertEquals("/sdcard/NXRedux", Sources.displayPath(home))
        assertEquals("/storage/1234-ABCD/Games/NXRedux", Sources.displayPath(ContentRef("${t}1234-ABCD%3AGames%2FNXRedux")))
        assertEquals("/sdcard", Sources.displayPath(ContentRef("${t}primary%3A")))
        assertEquals("/storage/1234-ABCD", Sources.displayPath(ContentRef("${t}1234-ABCD%3A")))
        assertEquals("/sdcard/Jeux vidéo", Sources.displayPath(ContentRef("${t}primary%3AJeux%20vid%C3%A9o")))
        assertEquals("/sdcard/NXRedux", Sources.displayPath(ContentRef("${t}primary%3ANXRedux/document/primary%3ANXRedux%2FRoms")))
        assertNull(Sources.displayPath(ContentRef("content://com.android.providers.downloads.documents/tree/raw%3A%2Fx")))
        assertNull(Sources.displayPath(ContentRef("Home")))
    }

    private val tree = "content://com.android.externalstorage.documents/tree/"

    @Test fun mapped_path_of_external_storage_trees() {
        assertEquals("/storage/emulated/0/NXRedux", Sources.mappedPath(ContentRef("${tree}primary%3ANXRedux")))
        assertEquals("/storage/emulated/0", Sources.mappedPath(ContentRef("${tree}primary%3A")))
        assertEquals("/storage/1234-ABCD/Games/NXRedux", Sources.mappedPath(ContentRef("${tree}1234-ABCD%3AGames%2FNXRedux")))
        assertEquals("/storage/emulated/0/Jeux vidéo", Sources.mappedPath(ContentRef("${tree}primary%3AJeux%20vid%C3%A9o")))
        // A document under a tree maps by its tree (home and extras are trees).
        assertEquals("/storage/emulated/0/NXRedux", Sources.mappedPath(ContentRef("${tree}primary%3ANXRedux/document/primary%3ANXRedux%2FRoms")))
        assertNull(Sources.mappedPath(ContentRef("content://com.android.providers.downloads.documents/tree/raw%3A%2Fstorage%2Femulated%2F0%2FDownload")))
        assertNull(Sources.mappedPath(ContentRef("${tree}home%3ARoms")))   // the Documents root: no volume
        assertNull(Sources.mappedPath(FileRefs.of("/storage/emulated/0/NXRedux")))
        assertNull(Sources.mappedPath(ContentRef("sd/Loose")))
    }

    @Test fun overlaps_compares_file_refs_and_mapped_trees_by_path() {
        val fileHome = FileRefs.of("/storage/emulated/0/NXRedux")
        assertTrue(Sources.overlaps(fileHome, ContentRef("${tree}primary%3ANXRedux%2FRoms%2FGB")))      // a SAF extra inside a file home
        assertTrue(Sources.overlaps(ContentRef("${tree}primary%3ANXRedux"), FileRefs.of("/storage/emulated/0/NXRedux/Loose")))
        assertTrue(Sources.overlaps(fileHome, ContentRef("${tree}primary%3ANXRedux")))                 // the same folder both ways
        assertTrue(Sources.overlaps(FileRefs.of("/storage/emulated/0"), fileHome))                     // a volume root holds everything on it
        assertTrue(Sources.overlaps(ContentRef("${tree}primary%3A"), fileHome))
        assertFalse(Sources.overlaps(fileHome, FileRefs.of("/storage/emulated/0/NXRedux2")))           // a sibling sharing a prefix
        assertFalse(Sources.overlaps(fileHome, ContentRef("${tree}1234-ABCD%3ANXRedux")))
        assertTrue(Sources.overlaps(FileRefs.of("/storage/1234-ABCD/Games"), ContentRef("${tree}1234-ABCD%3AGames%2FGB")))
        // An unmapped provider is compared as itself, never by a guessed path.
        assertFalse(Sources.overlaps(fileHome, ContentRef("content://com.android.providers.downloads.documents/tree/raw%3A%2Fstorage%2Femulated%2F0%2FNXRedux")))
    }

    @Test fun display_path_of_a_file_ref_is_its_path() {
        assertEquals("/storage/emulated/0/NXRedux", Sources.displayPath(FileRefs.of("/storage/emulated/0/NXRedux")))
    }

}
