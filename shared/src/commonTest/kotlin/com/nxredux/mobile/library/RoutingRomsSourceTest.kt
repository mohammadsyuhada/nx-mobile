package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RoutingRomsSourceTest {
    private val saf = FakeRomsSource(dirs = mapOf("Home" to listOf("a.gb")))
    private val files = FakeRomsSource(dirs = mapOf("file:///s/Roms" to listOf("b #1.gb")))
    private val offlineFiles = object : RomsSource by files {
        override suspend fun isOffline(tree: ContentRef) = true
        override fun displayName(tree: ContentRef) = "from files"
    }

    @Test fun a_file_ref_goes_to_the_file_source_and_any_other_ref_to_saf() = runTest {
        val routed = RoutingRomsSource(saf, files)
        assertEquals(listOf("a.gb"), routed.listChildren(ContentRef("Home")).map { it.name })
        assertEquals(listOf("b #1.gb"), routed.listChildren(ContentRef("file:///s/Roms")).map { it.name })
        assertEquals("b #1.gb", routed.child(ContentRef("file:///s/Roms"), "b #1.gb")?.name)
        assertTrue(routed.writeText(ContentRef("file:///s/Roms"), "map.txt", "x"))
        assertTrue(routed.writeBytes(ContentRef("Home"), "y.png", byteArrayOf(1)))
        assertEquals(listOf("file:///s/Roms/map.txt"), files.written)
        assertEquals(listOf("Home/y.png"), saf.written)
    }

    @Test fun offline_and_names_follow_the_ref() = runTest {
        val routed = RoutingRomsSource(saf, offlineFiles)
        assertTrue(routed.isOffline(ContentRef("file:///s/Roms")))
        assertFalse(routed.isOffline(ContentRef("Home")))
        assertEquals("from files", routed.displayName(ContentRef("file:///s/Roms")))
        assertEquals("Home", routed.displayName(ContentRef("Home")))
    }

    /** A source answering with [tag]-specific non-default values, to tell which delegate a call reached. */
    private fun tagged(tag: String) = object : RomsSource {
        override suspend fun listChildren(dir: ContentRef): List<Entry> = emptyList()
        override suspend fun readText(file: ContentRef): String? = "text-$tag"
        override suspend fun readHead(file: ContentRef, max: Int): ByteArray? = tag.encodeToByteArray()
        override suspend fun canWrite(tree: ContentRef) = tag == "file"
        override suspend fun createDirectory(parent: ContentRef, name: String): Entry? =
            Entry(ContentRef("$tag/$name"), name, true, null)
        override suspend fun delete(dir: ContentRef, name: String) = tag == "file"
        override suspend fun deleteEmptyDirectory(parent: ContentRef, name: String) = tag == "file"
        override suspend fun rename(dir: ContentRef, from: String, to: String) = tag == "file"
    }

    @Test fun every_call_routes_on_its_own_ref() = runTest {
        val routed = RoutingRomsSource(tagged("saf"), tagged("file"))
        val f = ContentRef("file:///s/Roms")
        val s = ContentRef("Home")
        assertEquals("text-file", routed.readText(f))
        assertEquals("text-saf", routed.readText(s))
        assertEquals("file", routed.readHead(f, 4)?.decodeToString())
        assertEquals("saf", routed.readHead(s, 4)?.decodeToString())
        assertTrue(routed.canWrite(f))
        assertFalse(routed.canWrite(s))
        assertEquals("file/d", routed.createDirectory(f, "d")?.ref?.value)
        assertEquals("saf/d", routed.createDirectory(s, "d")?.ref?.value)
        assertTrue(routed.delete(f, "x"))
        assertFalse(routed.delete(s, "x"))
        assertTrue(routed.deleteEmptyDirectory(f, "x"))
        assertFalse(routed.deleteEmptyDirectory(s, "x"))
        assertTrue(routed.rename(f, "a", "b"))
        assertFalse(routed.rename(s, "a", "b"))
    }
}
