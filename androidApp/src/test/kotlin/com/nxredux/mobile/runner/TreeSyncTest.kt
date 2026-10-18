package com.nxredux.mobile.runner

import com.nxredux.mobile.storage.FileImportTree
import com.nxredux.mobile.storage.FileRefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TreeSyncTest {
    @get:Rule val tmp = TemporaryFolder()
    private val t0 = 1_700_000_000_000L

    @Test fun a_file_tree_bios_folder_imports_when_it_differs_keeps_its_time_and_skips_parts() {
        val bios = tmp.newFolder("Bios", "GBA")
        File(bios, "gba_bios.bin").apply { writeText("dump"); setLastModified(t0) }
        File(bios, "next.bin.part").writeText("cut")
        File(bios, "sub").mkdir(); File(bios, "sub/x.bin").apply { writeText("x"); setLastModified(t0) }
        val system = tmp.newFolder("system")
        File(system, "gba_bios.bin").writeText("bundled")
        TreeSync(FileImportTree).importDir(FileRefs.of(bios.absolutePath), system, recursive = true, whenDiffers = true)
        assertEquals("dump", File(system, "gba_bios.bin").readText())
        assertEquals(t0, File(system, "gba_bios.bin").lastModified())
        assertEquals("x", File(system, "sub/x.bin").readText())
        assertFalse(File(system, "next.bin.part").exists())
    }
}
