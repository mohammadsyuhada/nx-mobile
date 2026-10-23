package com.nxredux.mobile.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

/** Scriptable platform: set the fields, then read the recorded calls. */
private class FakeUpdatePlatform(override val dev: Boolean = false) : UpdatePlatform {
    override val installedCode = 3_000L
    override val installedVersion = "0.3.0"
    /** The feed's body; [feedError] makes `fetchLatest` throw instead (offline, 403). */
    var feed: String = releaseJson("v0.4.0", digest = "sha256:ABC123")
    var feedError: Exception? = null
    var digest = "abc123"
    var busy: String? = null
    var installAllowed = true
    /** When set, `download` / `sha256` suspend on it after the first progress report. */
    var downloadGate: CompletableDeferred<Unit>? = null
    var digestGate: CompletableDeferred<Unit>? = null

    var fetches = 0
    var digests = 0
    var permissionAsks = 0
    val discarded = ArrayList<String>()
    val installs = ArrayList<String>()
    val keepAlives = ArrayList<Pair<Boolean, String?>>()
    var installResult: ((UpdateInstallResult) -> Unit)? = null

    override var lastSuccess = 0L
    override var lastFailure = 0L
    override var silenced: String? = null

    override suspend fun fetchLatest(): String { fetches++; feedError?.let { throw it }; return feed }
    override suspend fun download(release: AppRelease, onProgress: (done: Long, total: Long) -> Unit): UpdateFile {
        onProgress(500L, 1000L)
        downloadGate?.await()
        onProgress(1000L, 1000L)
        return UpdateFile("/cache/update.apk")
    }
    override suspend fun sha256(file: UpdateFile): String { digests++; digestGate?.await(); return digest }
    override fun discard(file: UpdateFile) { discarded += file.path }
    override fun busyReason(): String? = busy
    override fun canInstall(): Boolean = installAllowed
    override fun askInstallPermission() { permissionAsks++ }
    override fun install(file: UpdateFile, onResult: (UpdateInstallResult) -> Unit) { installs += file.path; installResult = onResult }
    override fun keepAlive(active: Boolean, text: String?) { keepAlives += active to text }
}

private fun releaseJson(tag: String, digest: String? = null): String {
    val digestField = digest?.let { ""","digest":"$it"""" } ?: ""
    return """{"tag_name":"$tag","body":"Faster menus.","assets":[{"name":"NXReduxMobile-$tag.apk","size":1000$digestField,"browser_download_url":"https://example.com/$tag.apk"}]}"""
}

private val RELEASE = AppRelease("0.4.0", 4_000L, "Faster menus.", "https://example.com/v0.4.0.apk", 1000L, "abc123")

private const val NOW = 100_000_000_000L

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateControllerTest {
    private fun TestScope.controller(fake: FakeUpdatePlatform, host: () -> String? = { null }): UpdateController {
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        return UpdateController(fake, scope, now = { NOW }).apply { hostBusy = host }
    }

    /** A controller whose download finished and is waiting on, or past, the install gate. */
    private fun TestScope.downloaded(fake: FakeUpdatePlatform, host: () -> String? = { null }): UpdateController =
        controller(fake, host).apply { onMainMenu(); update() }

    @Test fun a_due_check_with_a_newer_release_asks_about_it() = runTest {
        val fake = FakeUpdatePlatform()
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(RELEASE, c.popup.value)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertEquals(NOW, fake.lastSuccess)
    }

    @Test fun a_check_that_is_not_due_does_not_fetch() = runTest {
        val fake = FakeUpdatePlatform().apply { lastSuccess = NOW - 1_000L }
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(0, fake.fetches)
        assertEquals(UpdateState.Idle, c.state.value)
    }

    @Test fun a_silenced_version_is_available_without_a_popup() = runTest {
        val fake = FakeUpdatePlatform().apply { silenced = "0.4.0" }
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertNull(c.popup.value)
    }

    @Test fun an_unreachable_feed_fails_quietly() = runTest {
        val fake = FakeUpdatePlatform().apply { feedError = RuntimeException("HTTP 403") }
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(UpdateState.Failed("Couldn't reach GitHub", null), c.state.value)
        assertNull(c.popup.value)
        assertEquals(NOW, fake.lastFailure)
        assertEquals(0L, fake.lastSuccess)
    }

    @Test fun an_html_body_counts_as_unreachable() = runTest {
        val fake = FakeUpdatePlatform().apply { feed = "<html>" }
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(UpdateState.Failed("Couldn't reach GitHub", null), c.state.value)
        assertNull(c.popup.value)
        assertEquals(NOW, fake.lastFailure)
    }

    @Test fun a_release_without_this_builds_apk_is_up_to_date() = runTest {
        // The dev build against the real feed: only the release APK is attached.
        val fake = FakeUpdatePlatform(dev = true).apply { feed = """{"tag_name":"v0.4.0","assets":[{"name":"NXReduxMobile-v0.4.0.apk","size":1000,"browser_download_url":"https://example.com/v0.4.0.apk"}]}"""; lastFailure = 7L }
        val c = controller(fake)
        c.onMainMenu()
        assertEquals(UpdateState.UpToDate("0.3.0"), c.state.value)
        assertNull(c.popup.value)
        assertEquals(NOW, fake.lastSuccess)
        assertEquals(7L, fake.lastFailure)
    }

    @Test fun check_now_ignores_the_interval_and_silence_but_never_pops_up() = runTest {
        val fake = FakeUpdatePlatform().apply { silenced = "0.4.0"; lastSuccess = NOW }
        val c = controller(fake)
        c.checkNow()
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertNull(c.popup.value)
    }

    @Test fun check_now_on_the_installed_release_is_up_to_date() = runTest {
        val fake = FakeUpdatePlatform().apply { feed = releaseJson("v0.3.0") }
        val c = controller(fake)
        c.checkNow()
        assertEquals(UpdateState.UpToDate("0.3.0"), c.state.value)
        assertEquals(NOW, fake.lastSuccess)
    }

    @Test fun an_older_release_shows_the_installed_version() = runTest {
        val fake = FakeUpdatePlatform().apply { feed = releaseJson("v0.2.9") }
        val c = controller(fake)
        c.checkNow()
        assertEquals(UpdateState.UpToDate("0.3.0"), c.state.value)
    }

    @Test fun not_now_silences_the_popup_version() = runTest {
        val fake = FakeUpdatePlatform()
        val c = controller(fake)
        c.onMainMenu()
        c.notNow()
        assertEquals("0.4.0", fake.silenced)
        assertNull(c.popup.value)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
    }

    @Test fun update_reports_progress_then_a_digest_mismatch_fails() = runTest {
        val fake = FakeUpdatePlatform().apply { downloadGate = CompletableDeferred(); digest = "bad" }
        val c = controller(fake)
        c.onMainMenu()
        c.update()
        assertNull(c.popup.value)
        assertEquals(UpdateState.Downloading(RELEASE, 500L, 1000L), c.state.value)
        assertEquals(true to "Downloading 50%", fake.keepAlives.last())
        fake.downloadGate!!.complete(Unit)
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
        assertEquals(UpdateState.Failed("Update failed", RELEASE), c.state.value)
        assertEquals(false, fake.keepAlives.last().first)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun a_busy_platform_waits_then_installs_from_the_main_menu() = runTest {
        val fake = FakeUpdatePlatform().apply { busy = "Close your game to install" }
        val c = downloaded(fake)
        val waiting = assertIs<UpdateState.Waiting>(c.state.value)
        assertEquals(RELEASE, waiting.release)
        assertEquals("/cache/update.apk", waiting.file.path)
        assertEquals("Close your game to install", waiting.reason)
        assertEquals(true to "Close your game to install", fake.keepAlives.last())
        assertTrue(fake.installs.isEmpty())
        fake.busy = null
        c.onMainMenu()
        assertEquals(listOf("/cache/update.apk"), fake.installs)
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
        assertEquals(false, fake.keepAlives.last().first)
    }

    @Test fun the_poll_installs_once_the_platform_is_free() = runTest {
        val fake = FakeUpdatePlatform().apply { busy = "Waiting for Device Sync to finish" }
        val c = downloaded(fake)
        advanceTimeBy(2_001L)
        assertTrue(fake.installs.isEmpty())
        fake.busy = null
        advanceTimeBy(2_001L)
        assertEquals(listOf("/cache/update.apk"), fake.installs)
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
    }

    @Test fun the_host_reason_is_shown_while_waiting() = runTest {
        val fake = FakeUpdatePlatform()
        var host: String? = "Close Netplay to install"
        val c = downloaded(fake) { host }
        assertEquals("Close Netplay to install", assertIs<UpdateState.Waiting>(c.state.value).reason)
        host = null
        advanceTimeBy(2_001L)
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
    }

    @Test fun off_the_main_menu_the_platform_reason_shows_or_ready_to_install() = runTest {
        val fake = FakeUpdatePlatform().apply { busy = "Waiting for artwork to finish" }
        val c = downloaded(fake) { "" }
        assertEquals("Waiting for artwork to finish", assertIs<UpdateState.Waiting>(c.state.value).reason)
        fake.busy = null
        advanceTimeBy(2_001L)
        assertEquals("Ready to install", assertIs<UpdateState.Waiting>(c.state.value).reason)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun no_install_permission_asks_once_then_installs_when_granted() = runTest {
        val fake = FakeUpdatePlatform().apply { installAllowed = false }
        val c = downloaded(fake)
        assertEquals(1, fake.permissionAsks)
        assertEquals("Allow installs from NX Redux to update", assertIs<UpdateState.Waiting>(c.state.value).reason)
        advanceTimeBy(10_000L)
        assertEquals(1, fake.permissionAsks)   // the poll never asks again
        fake.installAllowed = true
        c.onMainMenu()
        assertEquals(listOf("/cache/update.apk"), fake.installs)
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
    }

    @Test fun permission_still_refused_goes_back_to_available() = runTest {
        val fake = FakeUpdatePlatform().apply { installAllowed = false }
        val c = downloaded(fake)
        c.onMainMenu()
        assertEquals(1, fake.permissionAsks)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
        assertTrue(fake.installs.isEmpty())
        assertEquals(false, fake.keepAlives.last().first)
    }

    @Test fun a_wait_for_a_game_still_asks_for_the_install_permission() = runTest {
        val fake = FakeUpdatePlatform().apply { busy = "Close your game to install"; installAllowed = false }
        val c = downloaded(fake)
        assertEquals(0, fake.permissionAsks)
        fake.busy = null
        c.onMainMenu()
        assertEquals(1, fake.permissionAsks)
        assertEquals("Allow installs from NX Redux to update", assertIs<UpdateState.Waiting>(c.state.value).reason)
        c.onMainMenu()
        assertEquals(1, fake.permissionAsks)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun each_download_asks_for_the_permission_again() = runTest {
        val fake = FakeUpdatePlatform().apply { installAllowed = false }
        val c = downloaded(fake)
        c.onMainMenu()   // still refused: Available
        c.update()
        assertEquals(2, fake.permissionAsks)
    }

    @Test fun a_cancelled_install_is_available_again() = runTest {
        val fake = FakeUpdatePlatform()
        val c = downloaded(fake)
        fake.installResult!!(UpdateInstallResult.CANCELLED)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertNull(c.popup.value)
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
    }

    @Test fun a_failed_install_fails() = runTest {
        val fake = FakeUpdatePlatform()
        val c = downloaded(fake)
        fake.installResult!!(UpdateInstallResult.FAILED)
        assertEquals(UpdateState.Failed("Update failed", RELEASE), c.state.value)
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
    }

    @Test fun a_started_install_stays_installing() = runTest {
        val fake = FakeUpdatePlatform()
        val c = downloaded(fake)
        fake.installResult!!(UpdateInstallResult.STARTED)
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
        assertTrue(fake.discarded.isEmpty())
    }

    @Test fun a_release_without_a_digest_skips_the_check() = runTest {
        val fake = FakeUpdatePlatform().apply { feed = releaseJson("v0.4.0") }
        val c = downloaded(fake)
        assertEquals(0, fake.digests)
        assertEquals(UpdateState.Installing(RELEASE.copy(sha256 = null)), c.state.value)
    }

    @Test fun cancel_during_download_is_available_again() = runTest {
        val fake = FakeUpdatePlatform().apply { downloadGate = CompletableDeferred() }
        val c = downloaded(fake)
        c.cancel()
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertEquals(false, fake.keepAlives.last().first)
        fake.downloadGate!!.complete(Unit)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun cancel_after_the_download_discards_the_file() = runTest {
        val fake = FakeUpdatePlatform().apply { digestGate = CompletableDeferred() }
        val c = downloaded(fake)
        c.cancel()
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun cancel_while_waiting_discards_and_stops_the_poll() = runTest {
        val fake = FakeUpdatePlatform().apply { busy = "Close your game to install" }
        val c = downloaded(fake)
        c.cancel()
        assertEquals(listOf("/cache/update.apk"), fake.discarded)
        assertEquals(UpdateState.Available(RELEASE), c.state.value)
        fake.busy = null
        advanceTimeBy(10_000L)
        assertTrue(fake.installs.isEmpty())
    }

    @Test fun update_after_a_failure_downloads_again() = runTest {
        val fake = FakeUpdatePlatform().apply { digest = "bad" }
        val c = downloaded(fake)
        fake.digest = "abc123"
        c.update()
        assertEquals(UpdateState.Installing(RELEASE), c.state.value)
    }
}
