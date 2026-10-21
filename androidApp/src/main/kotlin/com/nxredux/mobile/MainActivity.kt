package com.nxredux.mobile

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.WindowMetricsCalculator
import com.nxredux.mobile.runner.Posture
import com.nxredux.mobile.storage.LibchdrSectors
import com.nxredux.mobile.storage.NotificationAccess
import com.nxredux.mobile.storage.AndroidLocalFiles
import com.nxredux.mobile.storage.FileRefs
import com.nxredux.mobile.ui.FilePickMode
import com.nxredux.mobile.ui.HOME_NOT_WRITABLE
import com.nxredux.mobile.runner.PostureTracker
import com.nxredux.mobile.ui.nx.LocalPosture
import com.nxredux.mobile.achievements.AchievementsService
import com.nxredux.mobile.gametime.GameTimeService
import com.nxredux.mobile.gametime.LazyGameTimeData
import com.nxredux.mobile.launcher.AndroidAppLauncher
import com.nxredux.mobile.netplay.AndroidNetplay
import com.nxredux.mobile.netplay.NetplaySessionDir
import com.nxredux.mobile.netplay.usb.UsbCables
import com.nxredux.mobile.padlayout.PadLayoutActivity
import com.nxredux.mobile.runner.AndroidEmulatorConfigStore
import com.nxredux.mobile.runner.AndroidUserDataStore
import com.nxredux.mobile.runner.ArcadeSetsAsset
import com.nxredux.mobile.runner.Archives
import com.nxredux.mobile.runner.AssetCoreOptionSource
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.LibretroRunner
import com.nxredux.mobile.runner.NaomiSetsAsset
import com.nxredux.mobile.runner.NdsRenderer
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.Overlays
import com.nxredux.mobile.runner.PadKeymap
import com.nxredux.mobile.runner.playRumblePreview
import com.nxredux.mobile.runner.RomCache
import com.nxredux.mobile.runner.Shaders
import com.nxredux.mobile.runner.SystemFiles
import com.nxredux.mobile.scraper.AndroidArtFiles
import com.nxredux.mobile.scraper.ArtworkHost
import com.nxredux.mobile.scraper.ArtworkService
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.settings.AndroidDeviceInfo
import com.nxredux.mobile.settings.FolderPicker
import com.nxredux.mobile.settings.PadScreen
import com.nxredux.mobile.storage.AndroidAllFilesAccess
import com.nxredux.mobile.storage.AndroidLibraryIndexStore
import com.nxredux.mobile.storage.AndroidPrivateFiles
import com.nxredux.mobile.storage.AndroidSaveTransfer
import com.nxredux.mobile.storage.AndroidSidecarStore
import com.nxredux.mobile.storage.RecentsRecorder
import com.nxredux.mobile.storage.AndroidRomsSources
import com.nxredux.mobile.storage.SafTree
import com.nxredux.mobile.sync.DeviceSyncs
import com.nxredux.mobile.tools.CheatDbService
import com.nxredux.mobile.ui.App
import com.nxredux.mobile.ui.AppDeps
import com.nxredux.mobile.ui.AppState
import com.nxredux.mobile.ui.OnboardingStep
import com.nxredux.mobile.ui.Screen
import com.nxredux.mobile.ui.nx.BottomScreenButton
import com.nxredux.mobile.ui.nx.NxTheme
import com.nxredux.mobile.ui.nx.nxColorsFor
import com.nxredux.mobile.runner.SecondScreenPresentation
import com.nxredux.mobile.runner.SecondScreens
import com.nxredux.mobile.ui.nx.AxisDirections
import com.nxredux.mobile.ui.nx.InputRouter
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.PadEvent
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity(), FolderPicker {

    private lateinit var settings: AndroidAppSettings
    private lateinit var state: AppState
    private lateinit var router: InputRouter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // The hardware hat and left stick as held directions, with matching Down/Up pairs for the router.
    private val axes = AxisDirections { pad(it) }

    // The top refresh rate while buttons (or, on Android 11-14, a finger) drive the menu; FrameRateBoost has the why.
    private val frameRateBoost by lazy { FrameRateBoost(this) }

    /** Every pad event (keys, the hat and stick, the on-screen pad) on its way to the router. */
    private fun pad(event: PadEvent) {
        frameRateBoost.onPad(event)
        router.onEvent(event)
    }

    private val inputManager: InputManager? by lazy { getSystemService(InputManager::class.java) }
    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {}
        override fun onInputDeviceChanged(deviceId: Int) {}
        // A controller unplugged mid-hold can never send its own Up, so release the hat.
        override fun onInputDeviceRemoved(deviceId: Int) { releaseHat() }
    }

    // Emit Ups for whatever the hat or stick currently holds and forget its direction.
    private fun releaseHat() = axes.release()

    private val pickHomeTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            takeHomeGrant(uri)?.let { problem -> state.showError(problem); return@registerForActivityResult }
            state.setHome(ContentRef(uri.toString()))   // AppState checks overlap, saves, releases the old grant and rescans the library
        }
    }

    // Settings → Emulators → Saves → Export / Import ([AndroidSaveTransfer]).
    private var zipCreated: ((Uri?) -> Unit)? = null
    private val createZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        zipCreated?.invoke(uri); zipCreated = null
    }
    private var zipOpened: ((Uri?) -> Unit)? = null
    // Any file type, opening in the home folder: Samsung's picker listed nothing (search too) under a zip MIME filter;
    // AndroidSaveTransfer checks the picked file is a zip.
    private val openZip = registerForActivityResult(object : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent = super.createIntent(context, input).apply {
            homeDocument()?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
    }) { uri -> zipOpened?.invoke(uri); zipOpened = null }

    /** The home folder's own document, for a picker's starting place; null without a home (or for a file home). */
    private fun homeDocument(): Uri? = settings.treeRef?.takeIf { !FileRefs.isFile(it) }?.let { ref ->
        runCatching { Uri.parse(ref.value).let { DocumentsContract.buildDocumentUriUsingTree(it, DocumentsContract.getTreeDocumentId(it)) } }.getOrNull()
    }
    private var folderOpened: ((Uri?) -> Unit)? = null
    private val openFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> folderOpened?.invoke(uri); folderOpened = null }

    /** With All files access the app's list-style picker answers (a file ref); else Android's (a content ref). */
    private val savePickers = object : AndroidSaveTransfer.Pickers {
        private fun asRef(onRef: (ContentRef?) -> Unit): (Uri?) -> Unit = { uri -> onRef(uri?.let { ContentRef(it.toString()) }) }
        override fun createZip(suggestedName: String, onRef: (ContentRef?) -> Unit) {
            if (allFiles.granted()) return state.pickFile(FilePickMode.Export(suggestedName), onRef)
            zipCreated = asRef(onRef); createZip.launch(suggestedName)
        }
        override fun openZip(onRef: (ContentRef?) -> Unit) {
            if (allFiles.granted()) return state.pickFile(FilePickMode.ImportZip, onRef)
            zipOpened = asRef(onRef); openZip.launch(arrayOf("*/*"))
        }
        override fun openFolder(onRef: (ContentRef?) -> Unit) {
            if (allFiles.granted()) return state.pickFile(FilePickMode.ImportFolder, onRef)
            folderOpened = asRef(onRef); openFolder.launch(initialUri)
        }
    }

    private val pickExtraTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            takeGrant(uri)?.let { e -> state.showError("Could not keep access to that folder: ${e.message ?: e::class.simpleName}"); return@registerForActivityResult }
            state.addExtra(ContentRef(uri.toString()))   // AppState checks overlap, assigns the id, saves and rescans the library
        }
    }

    /**
     * The home must take writes (saves, badges, fingerprints and the handheld's own files live there): a folder that
     * says it takes no new files is refused before any grant is kept, and only a read+write grant will do. The message
     * to show, or null on success.
     */
    private fun takeHomeGrant(uri: Uri): String? {
        if (SafTree(contentResolver).takesNewFiles(uri) == false) return HOME_NOT_WRITABLE
        return runCatching { contentResolver.takePersistableUriPermission(uri, SafTree.RW) }.exceptionOrNull()?.let { HOME_NOT_WRITABLE }
    }

    /**
     * An extra folder only lends ROMs: read+write when the provider offers it (external storage does); read-only
     * otherwise. Returns the failure when neither grant could be taken (the caller then saves nothing), null on success.
     */
    private fun takeGrant(uri: Uri): Throwable? =
        runCatching { contentResolver.takePersistableUriPermission(uri, SafTree.RW) }
            .recoverCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            .exceptionOrNull()

    private val allFiles by lazy { AndroidAllFilesAccess(this) }   // the setup step, the Settings row and the pickers ask it
    private val secondScreens by lazy { SecondScreens(this) }   // onboarding's Bottom screen step asks it
    /** The Bottom screen step's button on the built-in second screen ([syncBottomScreenPrompt]); null while not shown. */
    private var bottomScreenPrompt: SecondScreenPresentation? = null
    private var inFront = false
    private lateinit var launcher: AndroidAppLauncher
    /**
     * The ROLE_HOME dialog: whatever the answer, the Launcher page re-reads the state. Without the role (declined, or
     * "Don't ask again" answering at once with no dialog shown) a notice says where to set the home app instead.
     */
    private val requestHomeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isFinishing) return@registerForActivityResult   // already handed over to the home instance, which reads the state itself
        if (launcher.isHome()) { if (!handOverToHome()) state.onLauncherChanged(); return@registerForActivityResult }
        launcher.setHome(false)   // the HOME alias goes off again (not home, so no settings open)
        state.onHomeRoleRefused()
    }

    /** This instance was started or re-delivered as the home screen (a HOME intent); a drawer launch can make a second instance that is not. */
    private var asHome = false

    /**
     * While this app is the home app only the home task's instance lives: Android keeps home and standard tasks apart, so
     * singleTask cannot merge a drawer (or notification) launch into it, and two AppStates would compete for the
     * process's services. A non-home instance starts the home one and finishes. True when it did; never as home.
     * The role can outlive the HOME alias (launcher mode just turned off, no other home picked yet): then the intent
     * resolves to nothing, and this instance carries on as a normal one.
     */
    private fun handOverToHome(): Boolean {
        if (asHome || isFinishing || !launcher.isHome()) return false
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(home) }.isFailure) return false
        finish()
        return true
    }

    /**
     * The other half of [handOverToHome], for a drawer instance that never comes back to run it: when the role dialog
     * is accepted, Android starts the home instance itself and the drawer one only stops, its result held until a start
     * that never comes. A home instance closes every non-home one.
     */
    private fun closeOtherInstances() {
        live.mapNotNull { it.get() }.filter { it !== this && !it.asHome && !it.isFinishing }.forEach { it.finish() }
    }

    /** USB Cable netplay: Android's one-time VPN consent (the cable's IP runs over a VPN that owns 10.99.0.0/30 only). */
    private val vpnConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        UsbCables.get(this).onConsent(r.resultCode == RESULT_OK)
    }

    /** Controls → Pad layout's editor; the menu re-reads the layouts when it returns, saved or not. */
    private val padLayoutEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { state.onPadLayoutEdited() }

    /** Artwork fetch and achievement download progress are notifications; the permission is asked when the first starts (the job runs either way). */
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (::state.isInitialized) state.onAllFilesAccessChanged()   // setup's Permissions page shows the answer
    }
    private val needNotificationPermission: () -> Unit = {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) runCatching { askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }

    /** Setup's Permissions page: the notification permission on Android 13+, asked through [askNotifications]. */
    private val notificationAccess: NotificationAccess? = if (android.os.Build.VERSION.SDK_INT < 33) null else object : NotificationAccess {
        override fun granted() = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        override fun request() { runCatching { askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) } }
    }

    /** This instance's VPN consent launcher on the cable, let go of in [onDestroy] while still the cable's. */
    private var usbConsent: ((Intent) -> Unit)? = null

    /** This instance's AppState as the artwork host, let go of in [onDestroy] (the service outlives this activity). */
    private var artworkHost: ArtworkHost? = null

    /**
     * This instance's Device Sync permission hook, let go of in [onDestroy] while still [DeviceSyncs]' (the controller
     * outlives this activity). The platform may call it from an IO thread, so it hops to the UI thread. (Screen-on is
     * the companion's [syncScreenOn], for every live instance.)
     */
    private val syncNotifications: () -> Unit = { runOnUiThread { needNotificationPermission() } }

    /** Start the picker at the primary volume so the user lands somewhere pickable. */
    private val initialUri: Uri = DocumentsContract.buildDocumentUri(SafTree.EXTERNAL_STORAGE, "primary:")

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // before super.onCreate: swaps Theme.NxMobile.Starting for Theme.NxMobile
        super.onCreate(savedInstanceState)
        live += WeakReference(this)
        asHome = intent.hasCategory(Intent.CATEGORY_HOME)
        launcher = AndroidAppLauncher(this) { requestHomeRole.launch(it) }
        if (handOverToHome()) return   // nothing built: onStart/onStop never run for an activity finished in onCreate
        if (asHome) closeOtherInstances()
        thread(name = "nx-temp-sweep") {
            NxStorage.sweepTemps(this)
            NetplaySessionDir.sweepOnce(filesDir)   // a netplay session copy left by a killed game (a process's first start only)
        }
        if (savedInstanceState == null) {   // once per launch, not per rotation; archive extractions and real-path copies
            val app = applicationContext
            thread(name = "nx-rom-cache-sweep") { RomCache.sweep(app) }
        }
        thread(name = "nx-overlays") { Overlays.extractBundled(this) }
        thread(name = "nx-shaders") { Shaders.extractBundled(this) }
        // The bundled system files (BIOS, PPSSPP's flash0 ...) after an update, here rather than inside the first game's
        // start; a game started meanwhile waits on SystemFiles' lock for this copy to finish.
        applicationContext.let { app -> thread(name = "nx-system-files") { SystemFiles.ensure(app) } }
        settings = AndroidAppSettings(this)
        // Launcher mode (this instance is the home task's): games get their own task; otherwise they share this one.
        val runner = LibretroRunner(this) { asHome }
        val indexStore = AndroidLibraryIndexStore(this)
        val glesVersion = getSystemService(ActivityManager::class.java).deviceConfigurationInfo.reqGlEsVersion
        // An import refuses while a sync runs; a sync refuses while an import runs (both write saves).
        val saveTransfer = AndroidSaveTransfer(this, savePickers, { runner.gameScreenAlive }, syncing = { DeviceSyncs.current()?.running == true })
        DeviceSyncs.screenOn = syncScreenOn
        DeviceSyncs.onNeedNotificationPermission = syncNotifications
        state = AppState(
            AppDeps(
                settings,
                AndroidRomsSources.of(this),
                runner,
                this,
                AndroidUserDataStore(this),
                cheatDb = CheatDbService.controller(this),
                achievements = AchievementsService.controller(this, settings, indexStore),
                sidecarStore = AndroidSidecarStore(this),
                indexStore = indexStore,
                appInfo = AndroidDeviceInfo.appInfo(this),
                deviceGpu = AndroidDeviceInfo::gpu,
                copyText = { AndroidDeviceInfo.copyText(applicationContext, it) },
                recents = processRecents(),
                arcadeSets = { ArcadeSetsAsset.get(applicationContext) },
                naomiSets = { NaomiSetsAsset.get(applicationContext) },
                coreOptions = AssetCoreOptionSource(assets),
                emulatorConfigs = AndroidEmulatorConfigStore(NxStorage.root(this)),
                coreDefaults = { id -> if (id == NdsRenderer.CORE_ID_MELONDSDS) NdsRenderer.defaults(glesVersion) else emptyMap() },
                gameTime = LazyGameTimeData { withContext(Dispatchers.IO) { GameTimeService.data(this@MainActivity) } },
                now = System::currentTimeMillis,
                // The process's one controller; each AppState becomes its host (index writes stay in AppState).
                artwork = { host -> artworkHost = host; ArtworkService.controller(this, host) },
                privateFiles = AndroidPrivateFiles(this),
                launcher = launcher,
                androidArtStore = AndroidArtFiles(this),
                rumblePreview = { playRumblePreview(applicationContext, it) },
                netplay = AndroidNetplay(this, NxStorage.root(this)).also { np ->
                    np.screenOn = { on -> runOnUiThread { keepScreenOn(on) } }
                },
                padEditor = { request -> padLayoutEditor.launch(PadLayoutActivity.intent(this, request)) },
                saveTransfer = saveTransfer,
                deviceSync = DeviceSyncs.get(this),
                chdSectors = LibchdrSectors(contentResolver),
                allFilesAccess = allFiles,
                notificationAccess = notificationAccess,
                localFiles = AndroidLocalFiles(AndroidLocalFiles.deviceVolumes(applicationContext)),
                archiveHead = { ref, name, exts, max -> withContext(Dispatchers.IO) { Archives.head(applicationContext, ref, name, exts, max) } },
                builtInSecondScreen = { secondScreens.builtInNow() != null },
            ),
            scope,
        )
        // Settings shows the dual-screen rows only with a second screen. Only while started: stop() clears `current`, which is not a removal.
        scope.launch { secondScreens.current.collect { if (inFront) state.onSecondScreenChanged(it != null) } }
        usbConsent = { consent: Intent ->
            runOnUiThread { runCatching { vpnConsent.launch(consent) }.onFailure { UsbCables.get(this).onConsent(false) } }
        }.also { UsbCables.get(this).askConsent = it }
        ArtworkService.onNeedNotificationPermission = needNotificationPermission
        AchievementsService.onNeedNotificationPermission = needNotificationPermission
        // A held direction's repeats are flagged: LEFT / RIGHT switch main menu tabs on a fresh press only. Text prompts
        // (context menu, RetroAchievements) take their buttons in AppState too: A confirms the draft, B cancels.
        router = InputRouter(scope, onPress = { state.onButton(it) }, onRepeat = { state.onButton(it, repeat = true) })
        // Same inset behaviour on every API level; App pads for the bars, only the modal pads for the IME.
        // Dark bar style: the app is always dark, so the bar icons stay light even when the system is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            // As the home screen, Back on the main menu stays: there is nothing under a launcher. Another instance (the
            // drawer's, in its own task) still closes.
            BackHandler(enabled = true) { if (!state.back() && !(asHome && launcher.isHome())) finish() }
            // The pad layouts' screen from the same measure the game and the editor use (the whole display, bars
            // included), so the Pad layout page edits the layouts a game will draw.
            val padScreen = PadScreen.of(LocalConfiguration.current.smallestScreenWidthDp.toFloat())
            LaunchedEffect(padScreen) { state.onPadScreen(padScreen) }
            CompositionLocalProvider(LocalPosture provides posture.value) { App(state, ::pad) }
        }
        collectPosture()
        scope.launch { state.screen.collect { syncBottomScreenPrompt() } }
    }

    /**
     * Onboarding's Bottom screen step: its "This is the bottom screen" button on the built-in second screen too, while the
     * step shows and the app is in front; a tap there picks that screen. Gone with the step, or when the app stops.
     */
    private fun syncBottomScreenPrompt() {
        val onStep = ::state.isInitialized && (state.screen.value as? Screen.Onboarding)?.step == OnboardingStep.BOTTOM_SCREEN
        val display = if (onStep && inFront) secondScreens.builtInNow() else null
        if (bottomScreenPrompt?.display?.displayId == display?.displayId) return
        bottomScreenPrompt?.dismiss()
        bottomScreenPrompt = null
        if (display == null) return
        val prompt = SecondScreenPresentation(this, display)
        val colors = nxColorsFor(settings.accent, settings.secondaryAccent)
        prompt.theme = { content -> NxTheme(colors) { content() } }
        prompt.showPanel { BottomScreenButton { state.onboardingBottomScreenOnSecond() } }
        // The display may have gone meanwhile: the step still works from the main screen.
        runCatching { prompt.show() }.onSuccess { bottomScreenPrompt = prompt }
            .onFailure { NxLog.w("NXAPP", "bottom screen prompt: ${it.message}") }
    }

    /** Flex mode from launch: the folding feature (and the hinge angle where it hides half-open), in window px. */
    private val posture = mutableStateOf<Posture>(Posture.Flat)

    private fun collectPosture() {
        val tracker = PostureTracker(this, area = {
            WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds.height().takeIf { it > 0 }?.let { 0 to it }
        }) { next, angle ->
            NxLog.i("NXAPP", "main posture: $next angle=$angle")
            posture.value = next
        }
        tracker.start()
    }

    /** singleTask: Home (the HOME alias) while running brings the menu back to its first tab. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        asHome = true
        closeOtherInstances()
        if (::state.isInitialized) state.goHome()
    }

    override fun onStart() {
        super.onStart()
        if (handOverToHome()) return   // made home (role dialog, Android's settings) while this drawer instance ran
        inputManager?.registerInputDeviceListener(inputDeviceListener, null)
        // API 26-28: back from Android's home settings without picking this app, the HOME alias goes off again.
        if (::launcher.isInitialized && launcher.dropAliasIfNotHome() && ::state.isInitialized) state.onLauncherChanged()
        if (::state.isInitialized) {
            state.refreshResume()
            state.onForeground()   // a card inserted while in the background: its games come back
        }
        inFront = true
        syncBottomScreenPrompt()
        secondScreens.start()
        if (::state.isInitialized) state.onSecondScreenChanged(secondScreens.current.value != null)   // what is there now, once, after the pause
    }

    /**
     * The menu stays awake for pad browsing, but not as the home screen: there it would keep the phone from ever
     * sleeping. Read on every resume, after the role dialog or Android's home settings may have changed it.
     */
    override fun onResume() {
        super.onResume()
        if (handOverToHome()) return
        applyKeepScreenOn()
        // Back from Android's All files access page (or a revoke meanwhile): setup moves on, the row and the refs follow.
        if (::state.isInitialized) state.onAllFilesAccessChanged()
    }

    /**
     * Netplay's host/join pages ([netplayAwake], set through [AndroidNetplay.screenOn]) and a running Device Sync
     * ([syncAwake], set for every live instance through [DeviceSyncs.screenOn]) hold the screen on even as the home
     * screen. [syncAwake] null: no report since this instance was created (the server role reports keepAlive once, maybe
     * before), so the controller's `running` decides. A report wins over `running`, which still reads true while the
     * sync's final keepAlive(false) runs.
     */
    private var netplayAwake = false
    private var syncAwake: Boolean? = null

    private fun keepScreenOn(on: Boolean) {
        netplayAwake = on
        applyKeepScreenOn()
    }

    private fun applyKeepScreenOn() {
        val syncing = syncAwake ?: (DeviceSyncs.current()?.running == true)
        if (!netplayAwake && !syncing && ::launcher.isInitialized && launcher.isHome()) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        inFront = false
        syncBottomScreenPrompt()   // takes the second screen's button away
        secondScreens.stop()
        inputManager?.unregisterInputDeviceListener(inputDeviceListener)   // a no-op when onStart handed over before registering
        if (::router.isInitialized) { releaseHat(); router.releaseAll() }
        frameRateBoost.reset()
        super.onStop()
    }

    override fun onDestroy() {
        live.removeAll { it.get().let { a -> a == null || a === this } }
        if (ArtworkService.onNeedNotificationPermission === needNotificationPermission) ArtworkService.onNeedNotificationPermission = null
        if (AchievementsService.onNeedNotificationPermission === needNotificationPermission) AchievementsService.onNeedNotificationPermission = null
        artworkHost?.let { ArtworkService.detach(it) }; artworkHost = null
        if (DeviceSyncs.onNeedNotificationPermission === syncNotifications) DeviceSyncs.onNeedNotificationPermission = null
        UsbCables.get(this).let { c -> if (c.askConsent === usbConsent) c.askConsent = {} }
        scope.cancel(); super.onDestroy()
    }

    /** The process's one recorder (created on the main thread, the only thread that asks). */
    private fun processRecents(): RecentsRecorder =
        recents ?: RecentsRecorder(AndroidUserDataStore(applicationContext), processScope).also { recents = it }

    companion object {
        // Process-lifetime, unlike [scope]: a game reports its start from GameActivity, maybe after this activity was
        // destroyed ("Don't keep activities", OEM reaping during a long unpack), and its recent must still be written.
        private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var recents: RecentsRecorder? = null
        /** Every created instance, for [closeOtherInstances] and Device Sync's screen-on; main thread only. */
        private val live = mutableListOf<WeakReference<MainActivity>>()
        /**
         * [DeviceSyncs.screenOn]: a sync's keep-screen-on reaches every live instance (an older one resumed mid-sync must
         * hear the end too). Any thread; always posted (never run inline, even on the main thread), so reports apply in
         * the order they were made: a step's last keepAlive(true) from IO can't land after the end's false. Captures
         * no activity, so it is installed once and never let go of.
         */
        private val syncScreenOn: (Boolean) -> Unit = { on ->
            Handler(Looper.getMainLooper()).post {
                live.mapNotNull { it.get() }.forEach { a -> a.syncAwake = on; a.applyKeepScreenOn() }
            }
        }
    }

    override fun pickHome() = pickHomeTree.launch(initialUri)
    override fun pickExtra() = pickExtraTree.launch(initialUri)
    override fun forget(ref: ContentRef) {
        if (FileRefs.isFile(ref)) return   // a path holds no grant to release
        val uri = Uri.parse(ref.value)
        runCatching { contentResolver.releasePersistableUriPermission(uri, SafTree.RW) }
        runCatching { contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun keyToButton(keyCode: Int): NxButton? = PadKeymap.buttonFor(keyCode)

    /**
     * Mapped pad keys reach the router before the view hierarchy: Compose's focus navigation would otherwise take a
     * keyboard-source DPAD (a BT keyboard, a pad in keyboard mode) and move its own focus instead of the NX selection.
     * While a text field is up the hierarchy sees them first, so LEFT/RIGHT move its cursor.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::state.isInitialized && !state.textEntryOpen && keyToButton(event.keyCode) != null) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> { onUserInteraction(); return onKeyDown(event.keyCode, event) }
                KeyEvent.ACTION_UP -> { onUserInteraction(); return onKeyUp(event.keyCode, event) }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val button = keyToButton(keyCode) ?: return super.onKeyDown(keyCode, event)
        // Ignore auto-repeats: the InputRouter runs its own repeat rules for held directions.
        if ((event?.repeatCount ?: 0) == 0) pad(PadEvent.Down(button))
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val button = keyToButton(keyCode) ?: return super.onKeyUp(keyCode, event)
        pad(PadEvent.Up(button))
        return true
    }

    /** A finger on the menu boosts its refresh rate too on Android 11-14, where not every device does it by itself. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        frameRateBoost.onTouch(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null &&
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            // The hat, else the left stick (as the in-game menu reads them): held directions repeat like the d-pad's.
            // Behind a text box (sign-in, Rename, New collection) the stick is ignored; the hat still reads.
            val stick = !(::state.isInitialized && state.textEntryOpen)
            axes.update(
                event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                if (stick) event.getAxisValue(MotionEvent.AXIS_X) else 0f, if (stick) event.getAxisValue(MotionEvent.AXIS_Y) else 0f,
            )
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
