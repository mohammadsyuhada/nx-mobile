package com.nxredux.mobile.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileImportTreeTest {
    @get:Rule val tmp = TemporaryFolder()
    private val t0 = 1_700_000_000_000L

    @Test fun lists_one_level_with_sizes_and_times_and_finds_children_by_name() {
        val dir = tmp.newFolder("Bios")
        File(dir, "GBA").mkdir()
        File(dir, "a #1.bin").apply { writeText("bios"); setLastModified(t0) }
        val root = FileRefs.of(dir.absolutePath)
        val listed = FileImportTree.list(root).sortedBy { it.name }
        assertEquals(listOf("GBA", "a #1.bin"), listed.map { it.name })
        assertEquals(TreeEntry(FileRefs.child(root, "a #1.bin"), "a #1.bin", false, 4L, t0), listed[1])
        assertTrue(listed[0].isDirectory); assertNull(listed[0].size)
        assertEquals("GBA", FileImportTree.child(root, "GBA")?.name)
        assertNull(FileImportTree.child(root, "missing"))
        assertNull(FileImportTree.child(root, ".."))
        assertEquals("bios", FileImportTree.openRead(FileRefs.child(root, "a #1.bin"))!!.use { it.readBytes().decodeToString() })
    }

    @Test fun delete_removes_a_file_and_only_an_empty_folder() {
        val dir = tmp.newFolder("Saves")
        File(dir, "Full").mkdir(); File(dir, "Full/x.srm").writeText("x")
        File(dir, "Empty").mkdir()
        val root = FileRefs.of(dir.absolutePath)
        assertFalse(FileImportTree.delete(FileRefs.child(root, "Full")))
        assertTrue(File(dir, "Full/x.srm").exists())
        assertTrue(FileImportTree.delete(FileRefs.child(root, "Empty")))
        assertTrue(FileImportTree.delete(FileRefs.child(FileRefs.child(root, "Full"), "x.srm")))
    }
}
