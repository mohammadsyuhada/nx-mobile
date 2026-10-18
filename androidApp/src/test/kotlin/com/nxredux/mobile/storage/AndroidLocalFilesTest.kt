package com.nxredux.mobile.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AndroidLocalFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun lists_entries_with_dot_ones_and_makes_single_segment_folders() = runBlocking {
        val root = tmp.newFolder("vol")
        File(root, "Roms").mkdir(); File(root, ".hidden").mkdir(); File(root, "a #1.zip").writeText("z")
        val volume = LocalVolume("Internal storage", root.absolutePath)
        val files = AndroidLocalFiles { listOf(volume) }
        assertEquals(listOf(volume), files.volumes())
        assertEquals(setOf(LocalEntry("Roms", true), LocalEntry(".hidden", true), LocalEntry("a #1.zip", false)), files.list(root.absolutePath).toSet())
        assertEquals(emptyList<LocalEntry>(), files.list(File(root, "missing").absolutePath))
        assertTrue(files.mkdir(root.absolutePath, "New"))
        assertTrue(File(root, "New").isDirectory)
        assertFalse(files.mkdir(root.absolutePath, "New"))
        assertFalse(files.mkdir(root.absolutePath, ".."))
        assertFalse(files.mkdir(root.absolutePath, "a/b"))
        assertTrue(files.isDirectory(root.absolutePath))
        assertFalse(files.isDirectory(File(root, "a #1.zip").absolutePath))
        assertTrue(files.canWrite(root.absolutePath))
        assertFalse(files.canWrite(File(root, "missing").absolutePath))
    }

    @Test fun a_failing_volume_query_is_no_volumes() = runBlocking {
        assertEquals(emptyList<LocalVolume>(), AndroidLocalFiles { error("no StorageManager") }.volumes())
    }
}
