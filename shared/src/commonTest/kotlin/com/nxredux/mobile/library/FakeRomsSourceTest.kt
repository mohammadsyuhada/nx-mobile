package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FakeRomsSourceTest {
    @Test fun writes_create_entries_with_the_clock_as_last_modified() = runTest {
        val src = FakeRomsSource(dirs = mapOf("Home" to emptyList()))
        src.now = 1000L
        val roms = src.createDirectory(ContentRef("Home"), "Roms")
        assertEquals(Entry(ContentRef("Home/Roms"), "Roms", true, null, 1000L), roms)
        assertTrue(src.writeText(ContentRef("Home/Roms"), "systems.txt", "a.gb\tGB\n"))
        assertEquals("a.gb\tGB\n", src.readText(ContentRef("Home/Roms/systems.txt")))
        assertEquals(listOf("systems.txt"), src.listChildren(ContentRef("Home/Roms")).map { it.name })
        assertEquals(1000L, src.child(ContentRef("Home/Roms"), "systems.txt")!!.lastModified)
        assertEquals(emptyList<String>(), src.written.filter { it != "Home/Roms" && it != "Home/Roms/systems.txt" })
    }

    @Test fun creating_an_existing_directory_returns_it_unchanged() = runTest {
        val src = FakeRomsSource(dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to listOf("x.gb")))
        assertEquals("Roms", src.createDirectory(ContentRef("Home"), "Roms")!!.name)
        assertEquals(listOf("x.gb"), src.listChildren(ContentRef("Home/Roms")).map { it.name })
    }

    @Test fun a_read_only_source_refuses_writes() = runTest {
        val src = FakeRomsSource(dirs = mapOf("Home" to emptyList()), writable = false)
        assertFalse(src.canWrite(ContentRef("Home")))
        assertNull(src.createDirectory(ContentRef("Home"), "Roms"))
        assertFalse(src.writeText(ContentRef("Home"), "a.txt", "x"))
    }

    @Test fun read_head_returns_the_leading_bytes() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H" to listOf("a.zip")), bytes = mapOf("H/a.zip" to byteArrayOf(1, 2, 3, 4)))
        assertEquals(listOf<Byte>(1, 2), src.readHead(ContentRef("H/a.zip"), 2)!!.toList())
        assertNull(src.readHead(ContentRef("H/missing"), 2))
    }

    @Test fun display_name_is_the_last_segment() {
        val src = FakeRomsSource(dirs = emptyMap())
        assertEquals("NXRedux", src.displayName(ContentRef("sd/NXRedux")))
        assertEquals("Roms", src.displayName(ContentRef("Roms")))
    }

    @Test fun read_only_prefixes_refuse_writes_under_them_only() = runTest {
        val src = FakeRomsSource(dirs = mapOf("Home" to listOf("Roms"), "Home/Roms" to emptyList(), "sd/X" to emptyList()), readOnly = setOf("Home"))
        assertFalse(src.canWrite(ContentRef("Home")))
        assertNull(src.createDirectory(ContentRef("Home/Roms"), ".sources"))
        assertFalse(src.writeText(ContentRef("Home/Roms"), "systems.txt", "x"))
        assertTrue(src.canWrite(ContentRef("sd/X")))
        assertTrue(src.writeText(ContentRef("sd/X"), "a.txt", "x"))
        assertEquals(listOf("sd/X/a.txt"), src.written)
    }

    @Test fun a_listing_fault_empties_or_fails_listings_but_not_child_lookups() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H" to listOf("a.gb")))
        src.listingFault = ListingFault.EMPTY
        assertEquals(emptyList(), src.listChildren(ContentRef("H")))
        src.listingFault = ListingFault.UNAVAILABLE
        assertFailsWith<ListingUnavailable> { src.listChildren(ContentRef("H")) }
        assertEquals("a.gb", src.child(ContentRef("H"), "a.gb")?.name)
    }
}
