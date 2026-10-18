package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FakeRomsSourceRangeTest {
    @Test fun range_reads_move_and_copy() = runTest {
        val src = FakeRomsSource(
            mapOf("a" to listOf("x.bin"), "b" to listOf("y.bin")),
            bytes = mapOf("a/x.bin" to ByteArray(100) { it.toByte() }, "b/y.bin" to ByteArray(3)),
        )
        assertContentEquals(byteArrayOf(10, 11, 12), src.readRange(ContentRef("a/x.bin"), 10, 3))
        assertContentEquals(byteArrayOf(98, 99), src.readRange(ContentRef("a/x.bin"), 98, 10))
        assertNull(src.readRange(ContentRef("a/none.bin"), 0, 4))
        assertTrue(src.moveFile(ContentRef("a"), "x.bin", ContentRef("b")))
        assertEquals(listOf("y.bin", "x.bin"), src.listChildren(ContentRef("b")).map { it.name })
        assertTrue(src.listChildren(ContentRef("a")).isEmpty())
        assertFalse(src.moveFile(ContentRef("b"), "y.bin", ContentRef("b")))           // the name is taken
        assertTrue(src.copyFile(ContentRef("b"), "y.bin", ContentRef("a")))
        assertEquals(3, src.readRange(ContentRef("a/y.bin"), 0, 10)!!.size)
    }
}
