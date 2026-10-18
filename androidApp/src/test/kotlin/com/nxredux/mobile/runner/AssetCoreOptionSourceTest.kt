package com.nxredux.mobile.runner

import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetCoreOptionSourceTest {
    private val assets = File("src/main/assets")

    private fun source(opens: MutableList<String> = mutableListOf()) = AssetCoreOptionSource { path ->
        opens += path
        val f = File(assets, path)
        if (!f.exists()) throw FileNotFoundException(path)
        f.inputStream()
    }

    @Test fun everyBundledTableParses() {
        val files = File(assets, "core-options").listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("no bundled tables found", files.isNotEmpty())
        val src = source()
        for (f in files) {
            val table = src.table(f.nameWithoutExtension)
            assertNotNull("unparseable: ${f.name}", table)
            assertTrue("no options: ${f.name}", table!!.options.isNotEmpty())
        }
    }

    @Test fun missingTableIsNullAndCached() {
        val opens = mutableListOf<String>()
        val src = source(opens)
        assertNull(src.table("no_such_core"))
        assertNull(src.table("no_such_core"))
        assertEquals(listOf("core-options/no_such_core.json"), opens)
    }

    @Test fun malformedTableIsNullAndCached() {
        var count = 0
        val src = AssetCoreOptionSource { count++; "{ not json".byteInputStream() }
        assertNull(src.table("x"))
        assertNull(src.table("x"))
        assertEquals(1, count)
    }

    @Test fun tableIsCached() {
        val src = source()
        val a = src.table("gambatte")
        assertNotNull(a)
        assertSame(a, src.table("gambatte"))
    }
}
