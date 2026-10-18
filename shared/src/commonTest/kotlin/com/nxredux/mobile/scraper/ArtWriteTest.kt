package com.nxredux.mobile.scraper

import com.nxredux.mobile.library.FakeRomsSource
import com.nxredux.mobile.library.ensurePath
import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ArtWriteTest {
    private val home = ContentRef("Home")
    private fun emptyHome(writable: Boolean = true) = FakeRomsSource(dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to listOf("GB"), "Home/Roms/GB" to listOf("Tetris.gb")), writable = writable)

    @Test fun mirrorDirsAreCreated() = runTest {
        val fs = emptyHome()
        val dir = fs.ensurePath(home, listOf("Roms", ".sources", "x1", "My Sega Genesis", ".media", "boxart"))
        assertNotNull(dir)
        assertEquals("Home/Roms/.sources/x1/My Sega Genesis/.media/boxart", dir.value)
        assertTrue(fs.writeBytes(dir, "Sonic.png", byteArrayOf(1, 2)))
        assertContentEquals(byteArrayOf(1, 2), fs.bytesAt("Home/Roms/.sources/x1/My Sega Genesis/.media/boxart/Sonic.png"))
        assertNull(fs.bytesAt("Home/Roms/.sources/x1/My Sega Genesis/.media/boxart/Sonic.png.part"))
    }

    @Test fun ensurePathReusesExistingDirs() = runTest {
        val fs = emptyHome()
        assertEquals("Home/Roms/GB", fs.ensurePath(home, listOf("Roms", "GB"))?.value)
        assertEquals(listOf("Tetris.gb"), fs.listChildren(ContentRef("Home/Roms/GB")).map { it.name })   // nothing recreated
    }

    @Test fun ensurePathStopsAtAFileInTheWay() = runTest {
        assertNull(emptyHome().ensurePath(home, listOf("Roms", "GB", "Tetris.gb", ".media")))
    }

    @Test fun ensurePathNeedsAWriteGrant() = runTest {
        assertNull(emptyHome(writable = false).ensurePath(home, listOf("Roms", "GB", ".media")))
    }

    @Test fun interruptedWriteLeavesNoFile() = runTest {
        val fs = emptyHome().apply { failRename = true }
        val dir = fs.ensurePath(home, listOf("Roms", "GB", ".media"))!!
        assertFalse(fs.writeBytes(dir, "Tetris.png", byteArrayOf(1)))
        assertNull(fs.bytesAt("Home/Roms/GB/.media/Tetris.png"))
        assertNull(fs.bytesAt("Home/Roms/GB/.media/Tetris.png.part"))
        assertEquals(emptyList(), fs.listChildren(dir).map { it.name })
    }

    @Test fun failedReplaceLeavesNeitherFileNorPart() = runTest {
        // As on SAF, the old file is deleted before the rename: a failed replace does not keep it.
        val fs = emptyHome()
        val dir = fs.ensurePath(home, listOf("Roms", "GB", ".media"))!!
        assertTrue(fs.writeBytes(dir, "Tetris.png", byteArrayOf(1)))
        fs.failRename = true
        assertFalse(fs.writeBytes(dir, "Tetris.png", byteArrayOf(2)))
        assertNull(fs.bytesAt("Home/Roms/GB/.media/Tetris.png"))
        assertNull(fs.bytesAt("Home/Roms/GB/.media/Tetris.png.part"))
        assertEquals(emptyList(), fs.listChildren(dir).map { it.name })
    }

    @Test fun replacesExistingFile() = runTest {
        val fs = emptyHome()
        val dir = fs.ensurePath(home, listOf("Roms", "GB", ".media"))!!
        assertTrue(fs.writeBytes(dir, "Tetris.png", byteArrayOf(1)))
        assertTrue(fs.writeBytes(dir, "Tetris.png", byteArrayOf(2, 3)))
        assertContentEquals(byteArrayOf(2, 3), fs.bytesAt("Home/Roms/GB/.media/Tetris.png"))
        assertEquals(listOf("Tetris.png"), fs.listChildren(dir).map { it.name })
    }

    @Test fun deleteRemovesOneFile() = runTest {
        val fs = emptyHome()
        val dir = fs.ensurePath(home, listOf("Roms", "GB", ".media"))!!
        fs.writeBytes(dir, "Tetris.png", byteArrayOf(1))
        assertTrue(fs.delete(dir, "Tetris.png"))
        assertNull(fs.bytesAt("Home/Roms/GB/.media/Tetris.png"))
        assertFalse(fs.delete(dir, "Tetris.png"))
    }
}
