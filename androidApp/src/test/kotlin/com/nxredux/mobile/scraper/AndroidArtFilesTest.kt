package com.nxredux.mobile.scraper

import coil3.filePath
import coil3.toUri
import com.nxredux.mobile.runner.ContentRef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AndroidArtFilesTest {
    private val dir = File(Files.createTempDirectory("art").toFile(), AndroidArtFiles.DIR)
    private val store = AndroidArtFiles(dir)

    @Test fun storesByPackageAndReturnsAVersionedFileRef() = runBlocking {
        assertNull(store.screenshot("com.example.game/.Main"))
        val ref = store.writeScreenshot("com.example.game/.Main", byteArrayOf(1, 2))
        val file = File(dir, "com.example.game.png")
        assertEquals(ContentRef("file://${file.absolutePath}?v=${file.lastModified()}"), ref)
        assertEquals(ref, store.screenshot("com.example.game/com.example.game.Other"))   // the package names the file
        store.writeScreenshot("com.example.game/.Main", byteArrayOf(3))                   // a replace
        assertArrayEquals(byteArrayOf(3), file.readBytes())
        assertEquals(listOf("com.example.game.png"), dir.list()!!.toList())                 // no temp file left
    }

    @Test fun aReplacedScreenshotGetsANewRef() = runBlocking {
        val first = store.writeScreenshot("com.a/.Main", byteArrayOf(1))
        val file = File(dir, "com.a.png")
        assertTrue(file.setLastModified(1_000_000L))
        assertEquals(ContentRef("file://${file.absolutePath}?v=1000000"), store.screenshot("com.a/.Main"))
        assertNotEquals(first, store.screenshot("com.a/.Main"))   // a new memory-cache key for Coil
    }

    @Test fun coilReadsTheFileThroughTheVersion() = runBlocking {
        val ref = store.writeScreenshot("com.a/.Main", byteArrayOf(1))!!
        val uri = ref.value.toUri()
        assertEquals("file", uri.scheme)
        assertEquals(File(dir, "com.a.png").absolutePath, uri.filePath)          // what FileUriFetcher opens: the query is not part of it
        assertEquals(ref.value, uri.toString())                                   // the memory cache key keeps it
    }

    @Test fun anIdThatIsNotAPackageNamesNoFile() = runBlocking {
        assertNull(store.writeScreenshot("../escape/.Main", byteArrayOf(1)))
        assertNull(store.writeScreenshot("/.Main", byteArrayOf(1)))
        assertNull(store.screenshot("../escape/.Main"))
        assertTrue(!dir.exists() || dir.list()!!.isEmpty())
    }

    @Test fun deleteAllRemovesEveryScreenshotAndCountsThem() = runBlocking {
        assertEquals(0, store.deleteAll())   // no folder yet
        store.writeScreenshot("com.a/.Main", byteArrayOf(1))
        store.writeScreenshot("com.b/.Main", byteArrayOf(2))
        File(dir, "com.c.png.1f.tmp").writeBytes(byteArrayOf(3))   // left by an interrupted write: removed, not counted
        assertEquals(2, store.deleteAll())
        assertNull(store.screenshot("com.a/.Main"))
        assertTrue(dir.list()!!.isEmpty())
    }
}
