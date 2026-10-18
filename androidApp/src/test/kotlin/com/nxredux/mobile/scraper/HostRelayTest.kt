package com.nxredux.mobile.scraper

import com.nxredux.mobile.library.ArtRefs
import com.nxredux.mobile.storage.PrivateFiles
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostRelayTest {
    private class Host(val name: String) : ArtworkHost {
        val applied = ArrayList<String>()
        override suspend fun libraryGames(): List<LibraryGameRef> = emptyList()
        override suspend fun allLibraryGames(): List<LibraryGameRef> = emptyList()
        override suspend fun applyArt(gamePath: String, art: ArtRefs) { applied += gamePath }
        override suspend fun readPrivate(name: String): String? = error("the relay reads its own files")
        override suspend fun writePrivate(name: String, text: String) = error("the relay writes its own files")
    }

    private class Files : PrivateFiles {
        val map = HashMap<String, String>()
        override suspend fun read(name: String): String? = map[name]
        override suspend fun write(name: String, text: String) { map[name] = text }
    }

    @Test fun detachingAnOlderHostKeepsTheNewerOne() = runBlocking {
        val old = Host("old"); val new = Host("new")
        val relay = HostRelay(old, Files())
        relay.attach(new)
        relay.detach(old)   // the old activity's onDestroy after the new one attached
        relay.applyArt("a", ArtRefs())
        assertEquals(listOf("a"), new.applied)
        assertTrue(old.applied.isEmpty())
    }

    @Test fun aDetachedRelayHoldsCallsUntilAHostAttaches() = runBlocking {
        val first = Host("first"); val next = Host("next")
        val relay = HostRelay(first, Files())
        relay.detach(first)
        val call = async(start = CoroutineStart.UNDISPATCHED) { relay.applyArt("b", ArtRefs()) }
        repeat(3) { yield() }
        assertFalse(call.isCompleted)
        relay.attach(next)
        withTimeout(1_000) { call.await() }
        assertEquals(listOf("b"), next.applied)
        assertTrue(first.applied.isEmpty())
    }

    @Test fun privateFilesNeedNoHost() = runBlocking {
        val h = Host("h"); val files = Files()
        val relay = HostRelay(h, files)
        relay.detach(h)
        withTimeout(1_000) { relay.writePrivate("job", "x") }
        assertEquals("x", withTimeout(1_000) { relay.readPrivate("job") })
    }
}
