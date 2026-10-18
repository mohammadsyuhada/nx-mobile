package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class AndroidRefsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val refs = AndroidRefs(null)   // file refs only: no ContentResolver on the JVM

    @Test fun a_file_ref_opens_its_file_with_uri_characters_in_the_name() {
        val f = File(tmp.newFolder("GB"), "Pokémon #1 (100%)?.gb").apply { writeBytes(byteArrayOf(1, 2, 3)); setLastModified(1_700_000_000_000) }
        val ref = FileRefs.of(f.absolutePath)
        assertArrayEquals(byteArrayOf(1, 2, 3), refs.openInput(ref)!!.use { it.readBytes() })
        assertEquals(3L, refs.size(ref))
        assertEquals(1_700_000_000_000, refs.lastModified(ref))
        assertEquals("Pokémon #1 (100%)?.gb", refs.displayName(ref))
    }

    @Test fun a_missing_file_has_no_size_or_time_and_opening_it_throws() {
        val ref = FileRefs.of(File(tmp.root, "gone.gb").absolutePath)
        assertNull(refs.size(ref))
        assertNull(refs.lastModified(ref))
        assertThrows(FileNotFoundException::class.java) { refs.openInput(ref) }
    }

    @Test fun a_folder_has_no_size() {
        assertNull(refs.size(FileRefs.of(tmp.newFolder("Roms").absolutePath)))
    }

    @Test fun without_a_resolver_a_content_ref_reads_as_unknown() {
        val doc = ContentRef("content://com.android.externalstorage.documents/tree/primary%3AX/document/primary%3AX%2Fa.gb")
        assertNull(refs.size(doc)); assertNull(refs.lastModified(doc)); assertNull(refs.displayName(doc)); assertNull(refs.openInput(doc))
    }
}
