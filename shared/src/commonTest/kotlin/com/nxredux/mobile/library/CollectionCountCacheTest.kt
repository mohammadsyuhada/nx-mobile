package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.GameRef
import com.nxredux.mobile.storage.MemoryPrivateFiles
import com.nxredux.mobile.storage.PrivateFiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionCountCacheTest {
    private val rpgs = CachedCount("RPGs", "h:100:12", "3:ab:cd:ef", 2)
    private val racing = CachedCount("Racing", CountStamps.NONE, "3:ab:cd:ef", 0)

    @Test fun entries_survive_an_encode_and_decode() {
        val text = CollectionCountCodec.encode(listOf(rpgs, racing))
        assertEquals(mapOf("RPGs" to rpgs, "Racing" to racing), CollectionCountCodec.decode(text))
    }

    @Test fun a_missing_corrupt_or_old_format_file_is_an_empty_cache() {
        assertEquals(emptyMap(), CollectionCountCodec.decode(null))
        assertEquals(emptyMap(), CollectionCountCodec.decode("{not json"))
        assertEquals(emptyMap(), CollectionCountCodec.decode("[]"))
        val old = CollectionCountCodec.encode(listOf(rpgs)).replace("\"version\":1", "\"version\":0")
        assertEquals(emptyMap(), CollectionCountCodec.decode(old))
        assertEquals(emptyMap(), CollectionCountCodec.decode("""{"version":1,"entries":[{"name":"X","file":"h:1:1","library":"l","count":-1}]}"""))
    }

    @Test fun a_count_is_valid_only_while_both_stamps_match() = runTest {
        val files = MemoryPrivateFiles().apply { files[CollectionCountCache.FILE] = CollectionCountCodec.encode(listOf(rpgs)) }
        val cache = CollectionCountCache(files, CoroutineScope(StandardTestDispatcher(testScheduler)), StandardTestDispatcher(testScheduler))
        cache.load()
        assertEquals(2, cache.valid("RPGs", "h:100:12", "3:ab:cd:ef"))   // both unchanged
        assertNull(cache.valid("RPGs", "h:200:12", "3:ab:cd:ef"))        // the file changed
        assertNull(cache.valid("RPGs", "h:100:12", "4:00:00:ef"))        // the library changed
        assertNull(cache.valid("RPGs", null, "3:ab:cd:ef"))              // no stamp to trust
        assertNull(cache.valid("Racing", CountStamps.NONE, "3:ab:cd:ef"))   // no entry
    }

    @Test fun file_stamps_need_both_time_and_size_and_a_stored_copy_hashes_its_text() {
        val ref = ContentRef("x")
        assertEquals("h:5:7", CountStamps.file(Entry(ref, "A.txt", false, 7L, 5L)))
        assertNull(CountStamps.file(Entry(ref, "A.txt", false, null, 5L)))
        assertNull(CountStamps.file(Entry(ref, "A.txt", false, 7L, null)))
        assertEquals(CountStamps.stored("a\nb\n"), CountStamps.stored("a\nb\n"))
        assertNotEquals(CountStamps.stored("a\nb\n"), CountStamps.stored("a\nc\n"))
    }

    @Test fun the_library_stamp_follows_the_game_set_and_home_not_its_order() {
        val a = GameRef("home", "Roms/GB", "Tetris.gb")
        val b = GameRef("home", "Roms/GBA", "Mario.gba")
        assertEquals(CountStamps.library("Home", listOf(a, b)), CountStamps.library("Home", listOf(b, a)))
        assertNotEquals(CountStamps.library("Home", listOf(a, b)), CountStamps.library("Home", listOf(a)))
        assertNotEquals(CountStamps.library("Home", listOf(a)), CountStamps.library("Home", listOf(GameRef("home", "Roms/GB", "Tetris2.gb"))))
        assertNotEquals(CountStamps.library("Home", listOf(a)), CountStamps.library("Other", listOf(a)))
    }

    @Test fun a_burst_of_saves_is_one_write_and_an_unchanged_cache_is_not_rewritten() = runTest {
        val files = CountingFiles()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cache = CollectionCountCache(files, CoroutineScope(dispatcher), dispatcher)
        cache.load()
        cache.put("RPGs", "h:100:12", "lib", 2); cache.save()
        cache.put("Racing", CountStamps.NONE, "lib", 0); cache.save()
        cache.remove("Racing"); cache.save()
        advanceUntilIdle()
        assertEquals(1, files.writes)
        assertEquals(mapOf("RPGs" to CachedCount("RPGs", "h:100:12", "lib", 2)), CollectionCountCodec.decode(files.text))
        cache.save(); advanceUntilIdle()
        assertEquals(1, files.writes)
    }

    @Test fun a_corrupt_file_is_rewritten_and_a_failing_write_does_not_throw() = runTest {
        val files = CountingFiles("garbage")
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cache = CollectionCountCache(files, CoroutineScope(dispatcher), dispatcher)
        cache.load()
        assertTrue(!cache.hasAny(listOf("RPGs")))
        cache.save(); advanceUntilIdle()
        assertEquals(emptyMap(), CollectionCountCodec.decode(files.text))
        assertEquals(CollectionCountCodec.encode(emptyList()), files.text)
        files.fail = true
        cache.put("RPGs", "h:1:1", "lib", 1); cache.save(); advanceUntilIdle()
        assertEquals(CollectionCountCodec.encode(emptyList()), files.text)
    }

    private class CountingFiles(var text: String? = null) : PrivateFiles {
        var writes = 0
        var fail = false
        override suspend fun read(name: String): String? = text
        override suspend fun write(name: String, text: String) {
            if (fail) throw IllegalStateException("disk full")
            writes++
            this.text = text
        }
    }
}
