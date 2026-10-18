package com.nxredux.mobile.launcher

import com.nxredux.mobile.ui.setHomePast
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppIconsTest {
    /** Counts icon loads, each waiting for [gate]; no app has an icon (an ImageBitmap needs Android's Bitmap, absent from unit tests). */
    private class CountingLauncher : AppLauncher {
        val asked = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun installedApps() = emptyList<InstalledApp>()
        override fun label(id: String): String? = null
        override fun launch(id: String) = false
        override fun isHome() = false
        override fun setHome(on: Boolean) = Unit
        override suspend fun icon(id: String): ImageBitmap? { asked += id; gate?.await(); return null }
    }

    @Test fun nothingIsCachedBeforeALoad() = runTest {
        assertNull(AppIcons(CountingLauncher(), backgroundScope).cached("a/A"))
    }

    // A missing icon (the tile draws the generic app glyph) is remembered too: the platform is asked once per app.
    @Test fun eachAppIsAskedOnce() = runTest {
        val launcher = CountingLauncher()
        val icons = AppIcons(launcher, backgroundScope)
        assertNull(icons.load("a/A"))
        assertNull(icons.load("a/A"))
        assertNull(icons.load("b/B"))
        assertNull(icons.cached("a/A"))
        assertEquals(listOf("a/A", "b/B"), launcher.asked)
    }

    // Two tiles asking for the same app while its icon loads share that one load.
    @Test fun concurrentLoadsShareOne() = runTest {
        val launcher = CountingLauncher().apply { gate = CompletableDeferred() }
        val icons = AppIcons(launcher, backgroundScope)
        val first = async { icons.load("a/A") }
        val second = async { icons.load("a/A") }
        testScheduler.advanceUntilIdle()
        launcher.gate!!.complete(Unit)
        first.await(); second.await()
        assertEquals(listOf("a/A"), launcher.asked)
    }

    // Retaining drops every other app's icon: the next load asks again; a kept one is still cached.
    @Test fun retainDropsTheRest() = runTest {
        val launcher = CountingLauncher()
        val icons = AppIcons(launcher, backgroundScope)
        icons.load("a/A"); icons.load("b/B")
        icons.retain(listOf("a/A"))
        icons.load("a/A"); icons.load("b/B")
        assertEquals(listOf("a/A", "b/B", "b/B"), launcher.asked)
    }

    // An app dropped while its icon loads is not cached when the load ends.
    @Test fun aLoadDroppedMidwayIsNotKept() = runTest {
        val launcher = CountingLauncher().apply { gate = CompletableDeferred() }
        val icons = AppIcons(launcher, backgroundScope)
        val pending = async { icons.load("b/B") }
        testScheduler.advanceUntilIdle()
        icons.retain(emptyList())
        launcher.gate!!.complete(Unit)
        pending.await()
        launcher.gate = null
        icons.load("b/B")
        assertEquals(listOf("b/B", "b/B"), launcher.asked)
    }
}
