package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LibraryIndexStoreTest {
    @Test fun memory_store_reads_what_was_written_and_forgets_on_delete() = runTest {
        val store = MemoryLibraryIndexStore()
        assertNull(store.read())
        assertTrue(store.write("{\"version\":1}"))
        assertEquals("{\"version\":1}", store.read())
        store.delete()
        assertNull(store.read())
        assertEquals(1, store.writes)
        assertEquals(1, store.deletes)
    }

    @Test fun a_failing_write_keeps_the_old_text_and_reports_false() = runTest {
        val store = MemoryLibraryIndexStore("old")
        store.failWrites = true
        assertFalse(store.write("new"))
        assertEquals("old", store.read())
        assertEquals(1, store.writes)
    }
}
