package com.nxredux.mobile.scraper

import com.nxredux.mobile.library.Entry
import com.nxredux.mobile.library.RomsSource
import com.nxredux.mobile.runner.ContentRef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class HomeRomsSourceTest {
    private class Recording : RomsSource {
        val seen = ArrayList<String>()
        override suspend fun listChildren(dir: ContentRef): List<Entry> { seen += dir.value; return emptyList() }
        override suspend fun readText(file: ContentRef): String? { seen += file.value; return null }
        override suspend fun canWrite(tree: ContentRef): Boolean { seen += tree.value; return true }
        override suspend fun writeBytes(dir: ContentRef, name: String, bytes: ByteArray): Boolean { seen += dir.value; return true }
    }

    @Test fun homePlaceholderFollowsTheCurrentHome() = runBlocking {
        val rec = Recording(); var home: ContentRef? = ContentRef("tree:a")
        val src = HomeRomsSource(rec) { home }
        src.canWrite(HomeRomsSource.HOME)
        home = ContentRef("tree:b")
        src.writeBytes(HomeRomsSource.HOME, "x.png", byteArrayOf(1))
        src.listChildren(ContentRef("tree:b/doc"))   // any other ref passes through
        assertEquals(listOf("tree:a", "tree:b", "tree:b/doc"), rec.seen)
        home = null
        try { src.canWrite(HomeRomsSource.HOME); fail("no home must fail") } catch (_: IllegalStateException) {}
    }
}
