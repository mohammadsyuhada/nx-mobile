package com.nxredux.mobile.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the updater needs from the app: the feed, the download, the install gate and the stored check times. */
interface UpdatePlatform {
    /** The dev flavor, which installs the `-dev` APK. */
    val dev: Boolean
    val installedCode: Long
    /** The installed versionName, for `Up to date (<version>)` when the feed's release is older than the install. */
    val installedVersion: String
    /** The feed's response body; throws when offline or on any HTTP status but 200. */
    suspend fun fetchLatest(): String
    suspend fun download(release: AppRelease, onProgress: (done: Long, total: Long) -> Unit): UpdateFile
    /** The file's lowercase hex SHA-256. */
    suspend fun sha256(file: UpdateFile): String
    fun discard(file: UpdateFile)
    /** Why the app can't be replaced now (a game, Device Sync, artwork, achievements, the cheat database), in the Global Constraints order; null when free. */
    fun busyReason(): String?
    /** Whether the app may install packages (`canRequestPackageInstalls()`). */
    fun canInstall(): Boolean
    /** Opens the system screen that allows installs from this app. */
    fun askInstallPermission()
    fun install(file: UpdateFile, onResult: (UpdateInstallResult) -> Unit)
    /** Keeps the process alive with a notification showing [text] while [active]. */
    fun keepAlive(active: Boolean, text: String?)
    /** Epoch millis of the last good / failed check, 0 for never; persisted. */
    var lastSuccess: Long
    var lastFailure: Long
    /** The version the user answered Not now for; persisted. */
    var silenced: String?
}

/** A downloaded APK. */
class UpdateFile(val path: String)

enum class UpdateInstallResult { STARTED, CANCELLED, FAILED }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val release: AppRelease, val done: Long, val total: Long) : UpdateState
    /** Downloaded and verified; [reason] says what the install waits for. */
    data class Waiting(val release: AppRelease, val file: UpdateFile, val reason: String) : UpdateState
    data class Installing(val release: AppRelease) : UpdateState
    /** [text]: "Couldn't reach GitHub" or "Update failed"; [release]: what Update downloads again, null after a failed check. */
    data class Failed(val text: String, val release: AppRelease?) : UpdateState
}

/**
 * The in-app updater: checks the GitHub feed (on the main menu once a day, or from About), asks about a newer release,
 * downloads and verifies it, waits until nothing would be lost by replacing the app, then hands it to the installer.
 * Runs its work in a process-level [scope] so a download outlives the screen that started it.
 */
class UpdateController(
    private val platform: UpdatePlatform,
    private val scope: CoroutineScope,
    private val now: () -> Long,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private val _popup = MutableStateFlow<AppRelease?>(null)
    /** The release the main menu should ask about, or null. */
    val popup: StateFlow<AppRelease?> = _popup.asStateFlow()

    /** The host's reason to hold the install ("Close Netplay to install"); "" when off the main menu (wait, showing the platform's reason); null when free. */
    var hostBusy: () -> String? = { null }

    private var job: Job? = null
    /** The Waiting poll; its own field because it starts inside the download [job], before that launch returns. */
    private var poll: Job? = null
    /** The downloaded APK until the installer has it, so a cancel can delete it. */
    private var file: UpdateFile? = null
    /** The Waiting state is for the install permission, which only the next [onMainMenu] re-checks. */
    private var awaitingPermission = false
    /** This download already opened the install-permission screen; the user is asked once per download. */
    private var asked = false

    private val busy: Boolean
        get() = _state.value.let { it is UpdateState.Checking || it is UpdateState.Downloading || it is UpdateState.Waiting || it is UpdateState.Installing }

    /** The main menu showed: checks the feed when due, or retries a waiting install. */
    fun onMainMenu() {
        val s = _state.value
        if (s is UpdateState.Waiting) {
            if (!awaitingPermission) { poll?.cancel(); gate(s.release, s.file); return }
            if (platform.canInstall()) { awaitingPermission = false; gate(s.release, s.file); return }
            // Refused again: the user decides when to try, from About's Update to row.
            awaitingPermission = false
            dropFile()
            finish(UpdateState.Available(s.release))
            return
        }
        if (busy || !UpdatePolicy.checkDue(now(), platform.lastSuccess, platform.lastFailure)) return
        check(automatic = true)
    }

    /** About's Check for updates: ignores the interval and the silenced version. */
    fun checkNow() {
        if (busy) return
        check(automatic = false)
    }

    /** The popup's Update or About's Update to: downloads the release. */
    fun update() {
        val release = when (val s = _state.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return
        _popup.value = null
        asked = false
        _state.value = UpdateState.Downloading(release, 0L, release.apkSize)
        platform.keepAlive(true, downloadingText(0L, release.apkSize))
        job = scope.launch {
            try {
                var shown = 0
                val downloaded = platform.download(release) { done, total ->
                    _state.value = UpdateState.Downloading(release, done, total)
                    val percent = percent(done, total)
                    if (percent != shown) { shown = percent; platform.keepAlive(true, downloadingText(done, total)) }
                }
                file = downloaded
                if (release.sha256 != null && platform.sha256(downloaded) != release.sha256) {
                    dropFile()
                    finish(UpdateState.Failed("Update failed", release))
                    return@launch
                }
                gate(release, downloaded)
            } catch (e: CancellationException) {
                throw e   // cancel() already restored the state
            } catch (e: Exception) {
                dropFile()
                finish(UpdateState.Failed("Update failed", release))
            }
        }
    }

    /** The popup's Not now: silences this version until a newer one. */
    fun notNow() {
        val release = _popup.value ?: return
        platform.silenced = release.version
        _popup.value = null
    }

    /** The notification's Cancel: stops a download or a waiting install and offers the release again. */
    fun cancel() {
        val release = when (val s = _state.value) {
            is UpdateState.Downloading -> s.release
            is UpdateState.Waiting -> s.release
            else -> return
        }
        job?.cancel()
        poll?.cancel()
        job = null
        poll = null
        awaitingPermission = false
        dropFile()
        finish(UpdateState.Available(release))
    }

    private fun check(automatic: Boolean) {
        _state.value = UpdateState.Checking
        job = scope.launch {
            val release = try {
                parseLatestRelease(platform.fetchLatest(), platform.dev)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null   // offline, rate-limited or a portal page: all the same to the user
            }
            if (release == null) {
                platform.lastFailure = now()
                _state.value = UpdateState.Failed("Couldn't reach GitHub", null)
                return@launch
            }
            platform.lastSuccess = now()
            if (release.versionCode > platform.installedCode) {
                _state.value = UpdateState.Available(release)
                if (automatic && UpdatePolicy.shouldPopup(release, platform.installedCode, platform.silenced)) _popup.value = release
            } else {
                val version = if (release.versionCode == platform.installedCode) release.version else platform.installedVersion
                _state.value = UpdateState.UpToDate(version)
            }
        }
    }

    /**
     * Installs [downloaded] when the host and the platform are free and installs are allowed; otherwise waits, polling the
     * busy reasons every 2 s so a quit game or a finished sync starts the install without another menu event.
     */
    private fun gate(release: AppRelease, downloaded: UpdateFile) {
        val reason = waitReason()
        if (reason != null) {
            waitFor(release, downloaded, reason)
            poll = scope.launch {
                while (true) {
                    delay(POLL_MS)
                    val next = waitReason() ?: break
                    if (next != (_state.value as? UpdateState.Waiting)?.reason) waitFor(release, downloaded, next)
                }
                gate(release, downloaded)
            }
            return
        }
        if (!platform.canInstall()) {
            if (!asked) { asked = true; platform.askInstallPermission() }
            awaitingPermission = true
            waitFor(release, downloaded, PERMISSION_REASON)
            return
        }
        file = null
        finish(UpdateState.Installing(release))
        platform.install(downloaded) { result ->
            when (result) {
                UpdateInstallResult.STARTED -> Unit
                UpdateInstallResult.CANCELLED -> { platform.discard(downloaded); _state.value = UpdateState.Available(release) }
                UpdateInstallResult.FAILED -> { platform.discard(downloaded); _state.value = UpdateState.Failed("Update failed", release) }
            }
        }
    }

    /** The host's reason first, then the platform's; off the main menu with nothing else running, "Ready to install". */
    private fun waitReason(): String? {
        val host = hostBusy()
        if (!host.isNullOrEmpty()) return host
        return platform.busyReason() ?: if (host == "") READY_REASON else null
    }

    private fun waitFor(release: AppRelease, downloaded: UpdateFile, reason: String) {
        _state.value = UpdateState.Waiting(release, downloaded, reason)
        platform.keepAlive(true, reason)
    }

    /** Leaves the foreground: the state no longer needs the process kept alive. */
    private fun finish(state: UpdateState) {
        _state.value = state
        platform.keepAlive(false, null)
    }

    private fun dropFile() {
        file?.let(platform::discard)
        file = null
    }

    private fun percent(done: Long, total: Long): Int = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0

    private fun downloadingText(done: Long, total: Long) = "Downloading ${percent(done, total)}%"

    companion object {
        const val POLL_MS = 2_000L
        const val PERMISSION_REASON = "Allow installs from NX Redux to update"
        const val READY_REASON = "Ready to install"
    }
}
