package com.nxredux.mobile.runner

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.DpSize
import com.nxredux.mobile.ui.nx.LocalWindowSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import com.nxredux.mobile.ui.nx.symmetricCutoutInset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nxredux.mobile.BuildConfig
import com.nxredux.mobile.R
import com.nxredux.mobile.achievements.AchievementsService
import com.nxredux.mobile.achievements.AndroidRaHasher
import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.achievements.RaBadgeFolders
import com.nxredux.mobile.achievements.RaConsoles
import com.nxredux.mobile.achievements.RaFingerprintKeys
import com.nxredux.mobile.achievements.RaGamePointer
import com.nxredux.mobile.achievements.RaLaunchFingerprint
import com.nxredux.mobile.achievements.RaLaunchHash
import com.nxredux.mobile.achievements.RaLaunchTarget
import com.nxredux.mobile.cheats.CheatSet
import com.nxredux.mobile.cheats.DolphinCheats
import com.nxredux.mobile.gametime.GameTimeRecorder
import com.nxredux.mobile.gametime.GameTimeService
import com.nxredux.mobile.library.ArcadeCompanions
import com.nxredux.mobile.library.NaomiSets
import com.nxredux.mobile.library.Names
import com.nxredux.mobile.netplay.NetplayCores
import com.nxredux.mobile.netplay.NetplayLinkKind
import com.nxredux.mobile.netplay.NetplayLinkOptions
import com.nxredux.mobile.netplay.NetplayPads
import com.nxredux.mobile.netplay.NetplayRole
import com.nxredux.mobile.netplay.NetplaySaves
import com.nxredux.mobile.netplay.NetplaySession
import com.nxredux.mobile.netplay.NetplaySessionDir
import com.nxredux.mobile.netplay.NetplayText
import com.nxredux.mobile.netplay.usb.UsbCables
import com.nxredux.mobile.options.ConfigSource
import com.nxredux.mobile.options.CoreConfig
import com.nxredux.mobile.options.CoreOption
import com.nxredux.mobile.options.CoreOptionFilter
import com.nxredux.mobile.options.GraphicsApiOptions
import com.nxredux.mobile.options.FrontendOptions
import com.nxredux.mobile.options.FrontendSettings
import com.nxredux.mobile.options.N3dsOptions
import com.nxredux.mobile.options.WiiOptions
import com.nxredux.mobile.options.WiiScreen
import com.nxredux.mobile.options.NdsOptions
import com.nxredux.mobile.options.NdsScreen
import com.nxredux.mobile.options.NdsSettings
import com.nxredux.mobile.options.OverlayFile
import com.nxredux.mobile.options.OverlayResolver
import com.nxredux.mobile.options.OverlaySets
import com.nxredux.mobile.options.PadOptions
import com.nxredux.mobile.options.ScreenScaling
import com.nxredux.mobile.options.ShaderOptions
import com.nxredux.mobile.options.VulkanFallback
import com.nxredux.mobile.options.frontendLayout
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.ui.nx.LocalNxOptionText
import com.nxredux.mobile.settings.GameRumble
import com.nxredux.mobile.settings.PadLayout
import com.nxredux.mobile.settings.PadLayouts
import com.nxredux.mobile.settings.PadScreen
import com.nxredux.mobile.settings.PadShape
import com.nxredux.mobile.settings.PadSlot
import com.nxredux.mobile.shaders.GlslPreset
import com.nxredux.mobile.shaders.ScaleType
import com.nxredux.mobile.shaders.ShaderLut
import com.nxredux.mobile.shaders.ShaderParam
import com.nxredux.mobile.shaders.WrapMode
import com.nxredux.mobile.storage.GamePath
import com.nxredux.mobile.storage.NdsResumeCleanup
import com.nxredux.mobile.storage.NdsDsvSave
import com.nxredux.mobile.storage.NxPaths
import com.nxredux.mobile.storage.AndroidLibraryIndexStore
import com.nxredux.mobile.storage.AndroidRefs
import com.nxredux.mobile.storage.AndroidRomsSources
import com.nxredux.mobile.test.NxTestResults
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.NxLog
import com.swordfish.libretrodroid.ScaleMode
import com.swordfish.libretrodroid.ScreenRect
import com.swordfish.libretrodroid.ScreenRegion
import com.swordfish.libretrodroid.ShaderChainData
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.ShaderLutData
import com.swordfish.libretrodroid.ShaderPassData
import com.swordfish.libretrodroid.ShaderScaleType
import com.swordfish.libretrodroid.ShaderWrap
import com.swordfish.libretrodroid.Variable
import com.swordfish.libretrodroid.VirtualFile
import com.nxredux.mobile.ui.nx.AxisDirections
import com.nxredux.mobile.ui.nx.DpadHat
import com.nxredux.mobile.ui.nx.LocalNxColors
import com.nxredux.mobile.ui.nx.NxDefaults
import com.nxredux.mobile.ui.nx.PadProfile
import com.nxredux.mobile.ui.nx.InGameMenu
import com.nxredux.mobile.ui.nx.InGameMenuModel
import com.nxredux.mobile.ui.nx.InputRouter
import com.nxredux.mobile.ui.nx.MenuEffect
import com.nxredux.mobile.ui.nx.MenuState
import com.nxredux.mobile.ui.nx.PlayerPadRow
import com.nxredux.mobile.ui.nx.PlayerSlotRow
import com.nxredux.mobile.ui.nx.NotificationQueue
import com.nxredux.mobile.ui.nx.NxButton
import com.nxredux.mobile.ui.nx.LocalPadBelow
import com.nxredux.mobile.ui.nx.LocalPortrait
import com.nxredux.mobile.ui.nx.NxNotifications
import com.nxredux.mobile.ui.nx.NxProgressPill
import com.nxredux.mobile.ui.nx.NxToast
import com.nxredux.mobile.ui.nx.NxTheme
import com.nxredux.mobile.ui.nx.nxColorsFor
import com.nxredux.mobile.ui.nx.nxNotificationPlacement
import com.nxredux.mobile.ui.nx.nxTopCentreMaxWidth
import com.nxredux.mobile.ui.nx.HintGlyphs
import com.nxredux.mobile.ui.nx.LocalHintGlyphs
import com.nxredux.mobile.ui.nx.PadThemeProvider
import com.nxredux.mobile.ui.nx.PadVibrationProvider
import com.nxredux.mobile.ui.nx.PAD_PORTRAIT_CM
import com.nxredux.mobile.ui.nx.PadEvent
import com.nxredux.mobile.ui.nx.map
import com.nxredux.mobile.ui.nx.SlotUi
import com.nxredux.mobile.ui.nx.PadStickModes
import com.nxredux.mobile.ui.nx.VirtualPad
import com.nxredux.mobile.ui.nx.PadMotion
import com.nxredux.mobile.ui.nx.CLUSTER_MAX_WIDTH_SHARE
import com.nxredux.mobile.ui.nx.VirtualPadLeft
import com.nxredux.mobile.ui.nx.VirtualPadRight
import com.nxredux.mobile.ui.nx.VirtualStickRouter
import com.nxredux.mobile.ui.nx.padBandPx
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class GameActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CORE_FILE = "core_file"
        const val EXTRA_CORE_ID = "core_id"
        const val EXTRA_TAG = "tag"
        const val EXTRA_TITLE = "title"
        const val EXTRA_FILE_NAME = "file_name"      // ROM file name; keys save data
        const val EXTRA_CONTENT_URI = "content_uri"
        const val EXTRA_FOLDER_URI = "folder_uri"    // the folder holding the content (RetroAchievements hashes a sheet's files); "" = unknown
        const val EXTRA_LOAD_MODE = "load_mode"      // "FILE_DESCRIPTOR" | "REAL_PATH"
        const val EXTRA_OPTIONS = "options"          // array of "key=value"
        const val EXTRA_RESUME_SLOT = "resume_slot"  // Int save slot to load after start (0..7, 9 = auto); absent = fresh
        const val EXTRA_COMPANION_NAMES = "companion_names"  // String[]: files a disc sheet loads, as the sheet spells them, or an arcade set's parent/BIOS sets beside it (<set>.zip|7z, lowercase)
        const val EXTRA_COMPANION_URIS = "companion_uris"    // String[]: their content URIs, same order
        const val EXTRA_SYSTEM_SETS = "system_sets"          // String[]: arcade BIOS sets not beside the game, looked for in the system dir
        const val EXTRA_LAUNCH_ID = "launch_id"      // Long: the launcher's PendingLaunches entry, told once the game runs
        const val EXTRA_GAME_PATH = "game_path"      // the library path (GamePath): game time's identity; absent = not recorded
        const val EXTRA_FORCE_GLES = "force_gles"    // Boolean: the relaunch after Vulkan could not start (VulkanFallback)
        private const val SIMULATE_VULKAN_FAIL = "simulate_vulkan_fail"   // debug builds only: see startEmulator
        // netplay: a Wi-Fi netplay launch (GameSession.netplay); all absent for every other launch. The run core is EXTRA_CORE_ID.
        const val EXTRA_NETPLAY_ROLE = "netplay_role"            // NetplayRole name: "HOST" | "CLIENT"
        const val EXTRA_NETPLAY_PEER = "netplay_peer"            // the other device's IPv4 address
        const val EXTRA_NETPLAY_FOLDER_CORE = "netplay_folder_core"  // the game folder's own core id (saves belong to it)
        const val EXTRA_NETPLAY_EMPTY_SAVES = "netplay_empty_saves"   // Boolean: no save copy happened although one was due
        const val EXTRA_NETPLAY_USB = "netplay_usb"              // Boolean: the session runs over the USB cable
        private const val TAG_LOG = "NXAPP"
        /** The Dolphin core's descriptor id (Wii and GameCube tags). */
        private const val DOLPHIN_ID = "dolphin"
        // Instance-state key: the slot to resume from when the system recreates this activity after a process kill.
        private const val KEY_RESUME_SLOT = "nx_resume_slot"
        private const val NDS_INSET_MARGIN_DP = 8f   // the PiP inset's margin inside the big screen
        private const val NDS_CHIP_MARGIN_DP = 12f   // the touch-mode "Controls" chip's margin (ControlsChip's 12.dp)
        private const val NDS_CHIP_EST_W_DP = 96f    // the chip's size until it is first measured
        private const val NDS_CHIP_EST_H_DP = 40f
        private const val NDS_LABEL_MS = 1500L   // a DS hotkey's label ("Bottom screen", "Stylus on")
        private const val PLAYER_NOTICE_MS = 2500L   // "Player 2: <pad>" when a pad takes a player slot
        private const val NETPLAY_NOTICE_MS = 5000L   // netplay: "Netplay ended ...", "Using <core> for this match ..."
        private const val NETPLAY_PILL_MS = 1000L     // netplay: a countdown pill outlives one 250 ms status tick
        private const val PEN_DP = 14f           // the stylus cursor's size
        private const val PEN_TICK_MS = 16L
        /**
         * Cores that read the left stick as analog (N64, PSP, PS1 DualShock, Dreamcast, and Dolphin's GameCube stick and
         * Wii Nunchuk and Classic sticks): a gamepad's stick is analog only there. melonDS DS has its own routing (the NDS
         * branch of onGenericMotionEvent). Every other core, prboom included, gets the stick folded into the d-pad.
         */
        private val STICK_CORES = setOf("mupen64plus_next", "ppsspp", "pcsx_rearmed", "swanstation", "flycast", "dolphin")
        /** Beside a real-path cache copy: what it was copied from ([realPathCopyStamp]). */
        private const val REAL_PATH_STAMP = ".nxstamp"
        // Process-wide, single thread: the Home auto state and its thumbnail are written off the main thread
        // in submission order, and outlive the activity (a stopped one may be destroyed before they finish).
        private val autoWriter = Executors.newSingleThreadExecutor { Thread(it, "nx-auto-state") }
        /** How long the Wii's HOME holds the core's Home (R3): a few frames, as a tap. */
        private const val WII_HOME_PRESS_MS = 150L

        /** Blocks until every queued Home write has finished. Call off the main thread before touching slot 9. */
        private fun awaitAutoWrites() { runCatching { autoWriter.submit {}.get() } }

        /** Debug builds only: the latest game screen, for the adb GAME_STOP hook ([debugQuit]). Main thread. */
        private var debugGame: java.lang.ref.WeakReference<GameActivity>? = null

        /**
         * Debug builds only (the adb GAME_STOP hook, NxTestReceiver): the live game quits as the menu's Quit does, so
         * its battery save and auto state are written. False when there is none (or in release). Main thread.
         */
        internal fun debugQuit(): Boolean {
            if (!BuildConfig.DEBUG) return false
            val game = debugGame?.get()?.takeIf { !it.isFinishing } ?: return false
            game.quitWithAutoState(chosen = true)
            return true
        }

        // The runner's native session is process-wide: one game screen owns it at a time (main thread only). A screen
        // created while another is alive (a second launcher stacked over a stopped game) makes that one quit and waits for
        // its teardown, native destroy included, before it starts its own emulator.
        private val sessions = SessionHandoff()

        /**
         * Some game screen holds (or waits for) the session, from its claim in onCreate to the end of its teardown: in
         * launcher mode a suspended game counts, its battery save still in memory. Main thread only, as [sessions].
         */
        internal val gameScreenAlive: Boolean get() = sessions.liveCount > 0
    }

    private var sessionClaim: SessionHandoff.Claim? = null

    private var retroView: GLRetroView? = null
    private val dpadHat = DpadHat()
    private val padViews = mutableListOf<androidx.compose.ui.platform.ComposeView>()
    private var inputManager: InputManager? = null
    // Re-lay-out when a controller is plugged/unplugged so the on-screen pad (and a DS portrait band) hides/shows live.
    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = onInputDevicesChanged()
        override fun onInputDeviceRemoved(deviceId: Int) {
            padLayouts.remove(deviceId)
            rumbles.forEach { it.deviceRemoved(deviceId) }
            val key = keysById.remove(deviceId)
            val port = key?.let { k -> slots?.disconnected(k) }
            // A pad unplugged mid-press never sends its key-up or its axes back to rest. One player's pad lets go of
            // its own port only; a one-player core lets go of everything. The open menu released it all already.
            if (!menuOpen) { if (slots == null) releaseAllGameInput() else port?.let(::releasePort) }
            onInputDevicesChanged()
        }
        override fun onInputDeviceChanged(deviceId: Int) { padLayouts.remove(deviceId); onInputDevicesChanged() }
    }

    /** Each pad's detected face layout by device id ([PadFaceLayout.detect]); null for an id with no device. */
    private val padLayouts = HashMap<Int, PadFaceLayout?>()

    /** Each held on-screen button's sent code, so its release repeats its press's. */
    private val padSent = SentKeys<NxButton>()
    /** The pad table for this game (Sega pad type resolved at launch and on a Controller Type change; [PadMap.IDENTITY] for DC). */
    private var padMap: PadMap = PadMap.IDENTITY
    /** The resolved Sega pad (Sega pad tags only; null = unknown or another console). */
    private var segaPad: SegaPad? = null
    /** The cart header's pad type read at launch (null = none read), kept so Auto can be resolved again mid-game. */
    private var segaHeader: SegaPad? = null
    /** The Controller Type the core was launched with (Sega pad tags only), to catch up a change made during load. */
    private var launchedSegaSetting: String? = null
    /** The 3DS game's title read from its file at launch (null = not read: compressed, homebrew, unreadable). */
    private var n3dsTitle: N3dsTitle? = null
    /** The 3DS pad draws the New 3DS controls (ZL, ZR, the C-stick): New 3DS Controls resolved ([N3dsNewControls]). */
    private var n3dsNew = false
    /** The Wii game's controls from GameTDB, looked up by the disc ID read at launch (null = unread or not listed). */
    private var wiiControls: WiiControls? = null
    /** The resolved Wii controller ([WiiController.resolve]); null outside Wii, and until the launch resolves it. */
    private var wiiPad: WiiPad? = null
    /** Motion Controls resolved ([WiiMotion.resolve]): the phone's sensors drive port 0's Wiimote. */
    private var wiiMotionOn = false
    /** The phone's accelerometer and gyroscope, created for a Wii game only. */
    private var motionSensors: MotionSensors? = null
    /** The display rotation the motion level was last captured in: a change mid-game recentres ([onConfigurationChanged]). */
    private var lastRotation = -1
    /**
     * The disc read under the WII tag is a GameCube disc ([WiiDisc.gameCube]): it keeps the plain pad and the descriptor's
     * ports, with no Wiimote, pointer, motion or Wii menu rows.
     */
    private var wiiGameCube = false
    private val isWii: Boolean get() = WiiOptions.appliesTo(gameTag) && !wiiGameCube
    private val isDolphin: Boolean get() = gameCoreId == DOLPHIN_ID
    /** This launch asked the core for Vulkan (Console Settings → Graphics API): a failure to start it relaunches on GLES. */
    @Volatile private var launchedOnVulkan = false
    /**
     * This screen is being recreated to start the game again on OpenGL ES ([relaunchOnGles]): it writes no auto state
     * and shows no failure dialog.
     */
    private var relaunching = false
    @Volatile private var launchEnv: Map<String, String?> = emptyMap()   // Graphics API's core environment for this launch
    /**
     * The core options this launch's Graphics API forced (SwanStation's renderer on Vulkan): locked for the session, so
     * hidden from Core Options, never applied by a restore, and saved with their stored value ([CoreConfig.valuesToSave]).
     */
    @Volatile private var graphicsForcedOptions: Set<String> = emptySet()

    /**
     * The keycode a hardware key is sent to the runner as, null to send nothing. Face keys work as printed (Console
     * Settings → Controller Layout names the pad's letters, [PhysicalKeys]): N64 by its printed letter's name
     * ([N64Input]); every other tag by the console's [com.nxredux.mobile.runner.FaceMode], then its [PadMap] picks what
     * that button sends. A key-up repeats its key-down's.
     */
    private fun runnerKey(event: KeyEvent, keyCode: Int, port: Int): Int? {
        if (isN64) {
            // By name on every pad, as on the handheld: the printed letter picks the N64 input; R2 and SELECT send nothing.
            val printed = positionOf(event, keyCode)
            val button = PadKeymap.buttonFor(printed) ?: return printed
            return N64Input.toRunner(button, printed)
        }
        val held = event.deviceId to keyCode
        val sent = inputs[port].sent
        if (event.action == KeyEvent.ACTION_UP) return sent.up(held, send(event, keyCode))
        // Only the first down records; a key-repeat sends what it recorded.
        if (event.repeatCount > 0) return sent.held(held) ?: send(event, keyCode)
        return sent.down(held, send(event, keyCode))
    }

    private fun send(event: KeyEvent, keyCode: Int): Int = padMap.sendKey(positionOf(event, keyCode))

    /**
     * The console button [keyCode] is for this game, as its keycode ([PhysicalKeys.toRunner]): the printed letter
     * (Console Settings → Controller Layout names the pad's letters; Auto = the detected layout), through the core's
     * letter swap on DC/NGP, or the key's position on the positional consoles (PlayStation, Sega, arcade...).
     */
    private fun positionOf(event: KeyEvent, keyCode: Int): Int {
        val detected = detectedLayout(event)
        return PhysicalKeys.toRunner(gameTag, keyCode, detected, PadOptions.printedLayout(frontendValues, detected))
    }

    /** The letter printed on [keyCode] ([PhysicalKeys.printed]): what the in-game menus act on, so A confirms where A is printed. */
    private fun printedKey(event: KeyEvent, keyCode: Int): Int {
        val detected = detectedLayout(event)
        return PhysicalKeys.printed(keyCode, detected, PadOptions.printedLayout(frontendValues, detected))
    }

    /** Where [event]'s device has its face keycodes ([PadFaceLayout.detectDevice]), cached per device id. */
    private fun detectedLayout(event: KeyEvent): PadFaceLayout? =
        if (padLayouts.containsKey(event.deviceId)) padLayouts[event.deviceId] else
            event.device?.let { d -> PadFaceLayout.detectDevice(event.deviceId, d.isVirtual, d.vendorId, d.name.orEmpty()) }
                .also { padLayouts[event.deviceId] = it }

    /** A controller connect or disconnect (the controller list changed since the last layout) also ends DS touch mode. */
    private fun onInputDevicesChanged() {
        if (controllerNames() != loggedControllers) ndsTouchMode = false
        applyLayout(resources.configuration.orientation)
        if (menuOpen) menuModel?.let(::publishPlayers)
    }
    /**
     * The controller names of the last [applyLayout] (logged once per change, to confirm hardware names); a device event
     * that changes them ends DS touch mode ([onInputDevicesChanged]).
     */
    private var loggedControllers: List<String>? = null

    /** A physical controller is attached: the in-game hints show the standard A/B art, else the on-screen pad's labels. */
    private val controllerConnected = mutableStateOf(false)

    // ---- in-game pause menu ----
    private var menuOpen = false
    private var menuView: ComposeView? = null
    private var menuModel: InGameMenuModel? = null
    private val menuState = mutableStateOf<MenuState?>(null)
    private val slotsUi = mutableStateOf<List<SlotUi>>(emptyList())
    // openMenu reads no files on the main thread. The slot strip ([slotsUi]) and the last-used
    // slot are read on IO at start and after every write of them, so an open shows them at once; an open re-reads the
    // strip, the overlay sets and the presets on IO and publishes them when they land (the cached ones show meanwhile).
    @Volatile private var lastSlotCache: Int? = null   // SaveStates.lastSlot(); null until the first read
    private var slotsJob: Job? = null   // the newest strip refresh (main thread only); a newer one cancels it
    private var menuListingJob: Job? = null   // openMenu's overlay / preset re-list (main thread only)
    private val menuAxes = AxisDirections { menuRouter.onEvent(it) }   // the hat / left stick as held menu directions
    private lateinit var menuRouter: InputRouter
    /** The on-screen stick: the game's left analog while playing, held menu directions while the menu is open. */
    private val stickRouter = VirtualStickRouter(
        onAnalog = { x, y ->
            if (isN3ds && n3dsHotkeys.onStick(x, y)) syncN3dsCursor()
            else if (!(isNds && ndsHotkeys.onStick(x, y))) retroView?.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y, 0)
        },
        onMenu = { menuRouter.onEvent(it) },
    )
    private var saveStates: SaveStates? = null
    private lateinit var sdRoot: File
    private lateinit var paths: NxPaths
    private var batterySaves: BatterySaves? = null
    private var clockSave: ClockSave? = null
    private var configStore: CoreConfigStore? = null
    private var configSource = ConfigSource.NONE
    private var lockedOptions: Set<String> = emptySet()
    private var restartOnlyOptions: Set<String> = emptySet()   // hidden from Core Options; applied only at game start
    private var forcedOptions: Set<String> = emptySet()   // the descriptor's lockedOptions: defaults a .cfg cannot change
    private var hiddenValues: Map<String, Set<String>> = emptyMap()   // the descriptor's values never offered nor read from a .cfg
    private var descriptorDefaults: Map<String, String> = emptyMap()
    private var storedValues: Map<String, String> = emptyMap()   // the config's resolved values (launch, or the last restore)
    private var cheatsSupported = true   // false for a core that ignores cheats (the menu hides its Cheats row); set in startEmulator
    private var optionsGeneration = 0   // bumped per live option change; drops stale re-reads
    // NdsRenderer.APPLY_ON_RESTART values set in the menu: kept from the running core, shown and saved (next launch).
    private val pendingRestartOptions = mutableMapOf<String, String>()
    @Volatile private var gameReady = false   // set on the first rendered frame; SRAM is only valid after load
    // This launch's PendingLaunches entry: run on the first frame (the game is recent), dropped on a refusal or finish.
    private val launchId by lazy { intent.getLongExtra(EXTRA_LAUNCH_ID, PendingLaunches.NONE) }
    private var failShown = false   // the could-not-start dialog is up (it only leaves by finishing)
    private var resumeSlot: Int? = null   // disk slot to load once the game runs (EXTRA_RESUME_SLOT); null = fresh
    private var quitting = false   // set by quitWithAutoState: quit is single-shot, the menu freezes, onStop skips its auto state
    // The quit's off-main native calls run under [coreLock] and only while ![coreGone]. An observer
    // added after the view's (so ON_DESTROY reaches it first) sets it under the lock before LibretroDroid.destroy(): a
    // quit cut short by a destroy never touches a destroyed core, nor the next game's once it loads.
    private val coreLock = Any()
    @Volatile private var coreGone = false
    // True from start until the requested resume load has finished (or failed): until then the running state
    // is the fresh boot, so it must not overwrite the resume point.
    @Volatile private var resumePending = false
    private var autoWrittenOnStop = false   // onStop serialized an auto state (its write is queued) since the last onStart
    // Home screenshot for the switcher (main thread only): onPause starts a PixelCopy; its bitmap is written as
    // the slot-9 thumb once onStop has queued the auto state. [homeThumbGen] drops callbacks from an older pause.
    private var homeThumb: Bitmap? = null
    private var homeThumbGen = 0
    private var autoWriteOk: AtomicBoolean? = null   // this stop's queued auto-state write succeeded (set on autoWriter)
    private var autoWriteFailed: AtomicBoolean? = null   // this stop's queued auto-state write threw (set on autoWriter)
    // The newest resume point known to be on disk, for the instance state when this stop's own
    // auto write has already failed: the slot this screen started from (onCreate), then slot 9 once a stop's write lands.
    @Volatile private var lastResumePoint: Int? = null
    // API 26–27 only: onSaveInstanceState runs before onStop there, so its bundle is kept to be updated
    // after the onStop write (the framework parcels it only after onStop returns).
    private var pendingOutState: Bundle? = null
    private val slotJobs = mutableListOf<Job>()   // manual Save/Load in flight (main thread only); Quit joins them
    private var wiiHomeSaving = false   // [pressWiiHome]'s resume point being saved (main thread only)
    @Volatile private var wiiHomeSaved = false   // [pressWiiHome] saved this session's resume point
    private var cutoutTop = 0   // current top display-cutout inset, px (0 in landscape: the cutout is on a side)
    // RomCache entries this game uses (its extraction, or its real-path copies): held so no sweep deletes them; released in onDestroy.
    private val cacheHolds = java.util.concurrent.ConcurrentLinkedQueue<String>()
    @Volatile private var cacheHoldsDrained = false   // set in onDestroy; a hold kept later (an unpack/copy that outlived us) is released at once
    /** Game time for this activity's game (null until the first frame, or when the launch had no library path). */
    private var gameTime: GameTimeRecorder? = null
    private var gameTimeTicker: Job? = null

    // ---- frontend (display) settings ----
    private var gameTag = "UNKNOWN"
    private var gameCoreId = ""
    /** Debug builds: when this screen was created; game.json's autoState names only a slot 9 written since. */
    private var debugStartedAt = 0L
    /** Players this game's core gives ([CoreRegistry.playersFor]); 1 until launch. */
    private var playerCount = 1
    private var frontendValues: Map<String, String> = FrontendOptions.defaults()   // every frontend key, as in effect now
    // The landscape pad clusters' alpha (Pad Opacity (landscape)): read inside their composition, so a menu change recomposes them.
    private val padLandscapeAlpha = mutableStateOf(FrontendSettings().padLandscapeAlpha)
    // The portrait pad's palette (Settings → Appearance → Pad theme), read once per game: it is set from the menu, not in-game.
    private val padTheme by lazy { AndroidAppSettings(this).padTheme }
    // Settings → Controls → Pad vibration, read once per game likewise; every pad ([composePad]) ticks at it.
    private val padVibration by lazy { AndroidAppSettings(this).padVibration }
    /**
     * Local multiplayer ([CoreRegistry.playersFor] > 1): which pad is which player; null for a one-player core, whose
     * pads and on-screen pad all drive port 0. Built at launch.
     */
    private var slots: PlayerSlots? = null
    /**
     * Each port's live input ([PortInputs]); one port for a one-player core. Rebuilt with [slots]. Per port it holds the
     * L2/R2 from trigger axes and keys (with hysteresis, pressed once per pull: every core but the Dreamcast, analog)
     * and the left stick folded into the d-pad ([stickDpadStep], with hysteresis, on a core without a stick).
     */
    private var inputs = PortInputs(1)
    /** Pad keys by device id: a removed device can no longer be asked for its descriptor. */
    private val keysById = HashMap<Int, DeviceKey>()
    /** Pad names by key, so a held (disconnected) slot still shows its pad's name. */
    private val padNames = HashMap<DeviceKey, String>()
    // Settings → Controls → Game rumble, read once per game: each port's rumble on its own pad; port 0 falls back to the phone.
    private val gameRumble by lazy { AndroidAppSettings(this).gameRumble }
    /** Each port's rumble (index = port); empty until [startEmulator] builds it, so every use must tolerate empty. */
    private var rumbles: Array<RumbleMotor> = emptyArray()
    private var overlaySets = OverlaySets()
    private var overlayView: ImageView? = null
    private var overlayFile: OverlayFile? = null            // resolved for the current orientation; null = none
    private var overlayBitmap: Pair<String, Bitmap>? = null // decoded overlay (its path) shown in overlayView
    private var overlayGen = 0                              // drops decodes overtaken by a newer overlay change

    // ---- Nintendo DS screens (NdsLayout regions) ----
    private var ndsRegions: List<NdsRegion> = emptyList()   // the DS screens as laid out now; empty = one picture
    private var ndsSplit = false                            // regions are on: the overlay is off and shown unavailable
    private var ndsFrameWarned = false                      // the "not 256x384" warning is logged once
    private var ndsRenderMode: String? = null               // melonds_render_mode this game started with (the notice's)
    private val ndsHotkeys = NdsHotkeys()        // a new activity, a new instance: stylus mode is off at every launch
    // DS landscape touch mode (L2 with the landscape pad shown): the pad clusters hidden for finger touch on the bottom
    // screen, a "Controls" chip to bring them back. Not persisted: a launch, rotation or controller change ends it.
    private var ndsTouchMode = false
    private var ndsChipView: ComposeView? = null   // the "Controls" chip while in touch mode
    // The chip's corner, clear of the touchable screen ([NdsTouchMode.chipCorner]); the chip keeps clear of a cutout there.
    private val ndsChipCorner = mutableStateOf(NdsTouchMode.ChipCorner.TOP_END)
    private var ndsChipSize: IntSize? = null        // the chip as measured, px (without its margin)
    private var cutoutMax = 0                       // the largest display-cutout inset on any side, px
    // Read once: without a touchscreen there is no finger touch, so L2 stays the stylus toggle.
    private val hasTouchscreen by lazy { packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) }
    private val ndsHat = DpadHat()               // hardware hat directions the core has been given (the forwarded ones)
    private var ndsHatX = 0
    private var ndsHatY = 0
    private var sentPen: NdsPen? = null          // the pen as last sent (null = stylus off)
    private var penJob: Job? = null
    private var penView: View? = null
    private val configWrites = Mutex()           // hotkey saves land in press order
    private val isNds: Boolean get() = NdsOptions.appliesTo(gameTag)

    // ---- Nintendo 3DS controls (N3dsHotkeys): the core draws both screens and its own touch cursor ----
    private val n3dsHotkeys = N3dsHotkeys()     // a new activity, a new instance: stylus mode is off at every launch
    private var n3dsLayout: String? = null      // citra_layout_option as in effect (launch, menu, hotkey, restore)
    private var n3dsHatX = 0
    private var n3dsHatY = 0
    private var n3dsHwRight = 0f to 0f          // the hardware right stick as last read
    private var n3dsPadRight = 0f to 0f         // the on-screen C-stick as last moved (New 3DS controls)
    private var n3dsSentRight = 0f to 0f        // the right stick as last sent to the core
    private var n3dsTickJob: Job? = null        // ticks the hotkeys while they ask ([N3dsHotkeys.ticking])
    private val isN3ds: Boolean get() = N3dsOptions.appliesTo(gameTag)
    private val isDc: Boolean get() = DcInput.appliesTo(gameTag)
    private val isN64: Boolean get() = N64Input.appliesTo(gameTag)
    /**
     * The core reads a pad's left stick as an analog stick ([STICK_CORES]). Any other core gets the stick folded into the
     * d-pad (onGenericMotionEvent).
     */
    private val coreHasStick: Boolean get() = gameCoreId in STICK_CORES   // netplay too: sticks are synced

    /** [padProfile]'s value, built once per game (and again when the resolved Sega pad changes it); null = not built yet. */
    private var padProfileCache: PadProfile? = null
    /** Bumped whenever [padProfileCache] is dropped, so Compose readers of [padProfile] (the menu's hint glyphs) redraw. */
    private val padProfileRevision = mutableStateOf(0)
    private fun dropPadProfile() { padProfileCache = null; padProfileRevision.value++; gcClearTriggers() }
    /** The on-screen pad's profile for this game's system (cluster, face letters, hidden buttons); the intent's tag, for onCreate's layout. */
    private fun padProfile(): PadProfile =
        padProfileCache ?: PadProfile.forTag(intent.getStringExtra(EXTRA_TAG) ?: gameTag, segaPad, n3dsNew, wiiPad, gameCube = wiiGameCube).also { padProfileCache = it }

    /**
     * Controls → Pad layout: this game's console's layout for [shape] on this screen (a Fold's cover or inner screen;
     * else Shared's, else the default pad, [PadLayout.EMPTY]). Read when a pad view is built, which every rotate, fold
     * and controller change does again.
     */
    private fun padLayout(shape: PadShape): PadLayout {
        val screen = PadScreen.of(resources.configuration.smallestScreenWidthDp.toFloat())
        return PadLayouts.resolve(AndroidAppSettings(this).padLayouts, CoreRegistry.consoleOf(intent.getStringExtra(EXTRA_TAG) ?: gameTag)?.id, PadSlot(shape, screen))
    }

    /**
     * This game's key in [PadStickModes]: every layout (rotate, fold, tabletop, a controller coming or going) builds a
     * new pad ComposeView, so the d-pad / stick choice is held per game for the process's life, not inside the pad. The
     * library path (game time's identity) when the launcher gave one,
     * else the content URI, else tag + file name: stable across this activity's recreations, distinct between games.
     */
    private val padStickKey: String
        get() = intent.getStringExtra(EXTRA_GAME_PATH)
            ?: intent.getStringExtra(EXTRA_CONTENT_URI)
            ?: "${intent.getStringExtra(EXTRA_TAG)}/${intent.getStringExtra(EXTRA_FILE_NAME)}"

    private fun onPadStickMode(useJoystick: Boolean) { PadStickModes[padStickKey] = useJoystick }

    // ---- shader presets ----
    private var shaderValues: Map<String, String> = ShaderOptions.fromConfig(emptyMap())   // nx_shader + stored nx_shader_param_*
    private var shaderParams: List<ShaderParam> = emptyList()                              // of the active preset (empty for None)
    private var presetNames: List<String> = emptyList()
    private var shaderGeneration = 0          // drops preset loads overtaken by a newer preset change
    private var loadedPreset: String? = null  // the preset whose chain the view has; null = none
    // The preset last asked for; differs from loadedPreset exactly while its load is in flight.
    private var requestedPreset: String? = null
    private var lastViewport: RectF? = null
    private var hudJob: Job? = null

    // ---- cheats ----
    private var cheatSet: CheatSet = CheatSet.EMPTY   // resolved on IO after start; toggles live here until Save Changes
    private var dolphinPlayerCheats: CheatSet? = null  // the player's own set; kept while Dolphin's built-in file is not written yet
    private var dolphinSavedCheats: List<String> = emptyList()
    private var dolphinBuiltInPath: String? = null     // named after the ROM-cache copy Dolphin loads
    private var cheatsLoad: Job? = null               // done once cheatSet holds the resolved (and saved-state-applied) set
    private var cheatsFolder = ""                     // the folder the Cheats page names (Cheats.folder), set at start

    // ---- netplay (Wi-Fi lockstep, see the "netplay" block) ----
    /**
     * netplay: this launch's session (read in onCreate from the EXTRA_NETPLAY_* extras); null for every other launch. In a
     * session there are no save states or auto-resume, no cheats, achievements, Core Options or local multiplayer slots,
     * and the menu is only "Leave netplay?".
     */
    private var netplay: NetplaySession? = null
    /** netplay: recreated after a process kill with a session's intent: that session is gone, nothing is started. */
    private var netplayRestored = false
    /** netplay over USB: the cable's start this screen ran on; a newer sheet start makes this screen's stop a no-op. */
    private var netplayUsbToken: Long? = null
    /** netplay: the engine session; set just before it starts, so onDestroy always shuts down what was started. */
    private var netplayGame: NetplayRun? = null
    private var netplayPillShown = false   // the countdown pill is the session's (hidden again on Clear)
    /** netplay: the session ended and the game plays on alone (the menu is then Continue / Quit). */
    private var netplayEnded = false
    /** netplay: this screen's id on the session copy it plays on ([NetplaySessionDir.claim]). */
    private val netplayOwner by lazy { java.util.UUID.randomUUID().toString() }
    /** netplay: the engine did not start; the game stays paused and silent behind the failure dialog (closeMenu too). */
    private var netplayStartFailed = false
    @Volatile private var netplayOwnCopyFailed = false   // a host's copy of its own saves failed: it starts without them

    // ---- RetroAchievements (see the "achievements" block) ----
    private var achievements: AchievementsBridge? = null   // null: off, signed out, or a console RA does not cover
    private val notifications = NotificationQueue()
    private val notificationState = mutableStateOf(Pair<List<NxToast>, NxProgressPill?>(emptyList(), null))
    private var notificationsView: ComposeView? = null
    /** The landscape overlay pad (the clusters over the game) is laid out: the notifications move top-centre, clear of it. */
    private val overlayPadShown = mutableStateOf(false)
    // Whether the current layout was built with the portrait band hidden ([portraitPadHidden]): a change re-lays out.
    private var layoutPadHidden = false
    // Portrait with the band hidden: the clusters' top, screen px (they cover from there down); null without them.
    private var portraitClusterTop: Int? = null
    private var tickJob: Job? = null

    // ---- Flex mode (tabletop) ----
    /** The posture the layout follows ([applyLayout]); [Posture.Flat] until a half-opened horizontal hinge is seen. */
    private var posture: Posture = Posture.Flat
    /** Follows the fold (and the hinge angle where the half-open state is hidden), root-relative. */
    private val postureTracker by lazy { PostureTracker(this, area = {
        val root = findViewById<FrameLayout>(R.id.root)
        if (root.height <= 0) null else IntArray(2).also { root.getLocationInWindow(it) }[1] to root.height
    }) { next, angle -> onPosture(next, angle) } }   // lazy: first touched in onCreate ([collectPosture])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) { debugGame = java.lang.ref.WeakReference(this); debugStartedAt = System.currentTimeMillis() }
        // Debug only: a GAME test hook's game runs on a locked test device (NxTestResults.markShowWhenLocked).
        if (BuildConfig.DEBUG && com.nxredux.mobile.test.NxTestResults.takeShowWhenLocked(this)) {
            setShowWhenLocked(true); setTurnScreenOn(true)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        // Cross-fade in from the menu instead of the default slide-from-right: the bottom pad is
        // identical and in the same position in both screens, so a fade keeps it visually steady while
        // only the top (game) area changes. (Back to the menu cross-fades too.) Only while the game shares
        // the menu's task (launcher mode off); in launcher mode it has its own task and the system task animation runs,
        // as Android ignores these overrides for a task change (LibretroRunner's gameLaunchFlags).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        // The game's own Recents entry (launcher mode) carries its title; sharing the menu's task, the entry stays the app's.
        if (isTaskRoot) {
            val recentsLabel = intent.getStringExtra(EXTRA_TITLE) ?: "Game"
            if (Build.VERSION.SDK_INT >= 33) setTaskDescription(ActivityManager.TaskDescription.Builder().setLabel(recentsLabel).build())
            else @Suppress("DEPRECATION") setTaskDescription(ActivityManager.TaskDescription(recentsLabel))
        }
        setContentView(R.layout.activity_game)
        volumeControlStream = AudioManager.STREAM_MUSIC   // volume keys set the game's (media) volume, even between sounds
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, findViewById(R.id.root)).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // Portrait: start the game below a top camera cutout. Returned unconsumed so children still get it.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val top = cutout.top
            val max = maxOf(cutout.left, cutout.top, cutout.right, cutout.bottom)
            if (max != cutoutMax) {
                cutoutMax = max
                placeNdsChip()
            }
            if (top != cutoutTop) {
                cutoutTop = top
                applyGameTopMargin(resources.configuration.orientation)
            }
            insets
        }
        inputManager = (getSystemService(Context.INPUT_SERVICE) as InputManager).also {
            it.registerInputDeviceListener(inputDeviceListener, Handler(Looper.getMainLooper()))
        }
        menuRouter = InputRouter(lifecycleScope, ::onMenuPress)
        applyLayout(resources.configuration.orientation)
        collectPosture()
        // Back as a gesture or a key on Android 16+ (targetSdk 36: predictive back, KEYCODE_BACK does not reach
        // onKeyDown): what the key's press did there, the in-game menu toggles (B inside it). Before 16 the gesture and
        // the key both arrive as KEYCODE_BACK, which onKeyDown consumes, so this never runs twice for one Back.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = backPressed()
        })
        // netplay: a session launches cold: no saved or requested resume slot.
        netplay = netplaySessionFrom(
            intent.getStringExtra(EXTRA_NETPLAY_ROLE), intent.getStringExtra(EXTRA_NETPLAY_PEER),
            intent.getStringExtra(EXTRA_CORE_ID) ?: "", intent.getStringExtra(EXTRA_NETPLAY_FOLDER_CORE),
            intent.getBooleanExtra(EXTRA_NETPLAY_EMPTY_SAVES, false),
            intent.getBooleanExtra(EXTRA_NETPLAY_USB, false),
        )
        netplayRestored = netplay != null && savedInstanceState != null
        if (netplay?.overUsb == true) netplayUsbToken = UsbCables.get(this).token()
        // Decided here, not after startEmulator's async setup, so a stop in between still records the slot.
        // A saved slot (recreated after a process kill) wins over the launch intent's.
        val savedSlot = savedInstanceState?.getInt(KEY_RESUME_SLOT, -1)?.takeIf { it in 0..NxPaths.AUTO_SLOT }
        resumeSlot = if (netplay != null) null else savedSlot ?: intent.getIntExtra(EXTRA_RESUME_SLOT, -1).takeIf { it in 0..NxPaths.AUTO_SLOT }
        resumePending = resumeSlot != null
        lastResumePoint = resumeSlot
        val claim = sessions.claim(::onSessionHandedOff).also { sessionClaim = it }
        lifecycleScope.launch {
            if (claim.mustWait) {
                // Also before the battery load and the RA prepare: the old game's quit writes the same saves and stops its client.
                // The watchdog only informs: the emulator never starts before the old game's native destroy.
                NxLog.i(TAG_LOG, "another game screen is alive: waiting for its quit before starting")
                var hint: android.widget.Toast? = null
                val stuck = claim.awaitWithWatchdog(
                    onSlow = {
                        NxLog.w(TAG_LOG, "the previous game screen is slow to close (${SessionHandoff.SLOW_AFTER_MS} ms)")
                        hint = toast("Closing the previous game…")
                    },
                    onStuck = {
                        NxLog.w(TAG_LOG, "the previous game screen has not closed after ${SessionHandoff.STUCK_AFTER_MS} ms")
                        hint?.cancel()
                        fail("The previous game has not closed yet, so this one cannot start. Go back and open it again.")
                    },
                )
                hint?.cancel()
                // Stuck: the dialog is up and Back is the only way on (finish releases this claim); do not start under it.
                if (stuck) return@launch
                NxLog.i(TAG_LOG, "the previous game screen is torn down: starting")
            }
            startEmulator()
        }
    }

    /**
     * A newer game screen claimed the native session: leave through the normal quit (battery save, auto state, RA stop,
     * then finish; onDestroy's native destroy releases the session). Usually stopped here: the auto state is serialized
     * off the GL thread as onStop does, and the thumbnail is skipped when the stopped surface cannot be copied.
     */
    private fun onSessionHandedOff() {
        if (isDestroyed) return
        NxLog.i(TAG_LOG, "a newer game screen takes the session: quitting ${intent.getStringExtra(EXTRA_TITLE)}")
        quitWithAutoState(chosen = false)
    }

    /**
     * singleTop. In launcher mode launches clear the game task (CLEAR_TASK); sharing the menu's task, the menu (the
     * only launcher) sits under this screen. So this only guards a launch that does reach this running game: it is
     * ignored, and its PendingLaunches entry is dropped, not leaked.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getLongExtra(EXTRA_LAUNCH_ID, PendingLaunches.NONE)
        if (id != launchId) PendingLaunches.take(id)
    }

    override fun onPause() {
        rumbles.forEach { it.pause() }   // no rumble in the background; onResume picks the core's state up again
        // The battery save is written synchronously (the process may be killed once backgrounded).
        // A quit writes it once itself, off the main thread (quitWithAutoState), so nothing here then.
        if (!quitting) writeBatterySave(notify = false)
        netplayGame?.pause()   // netplay: backgrounded: no input goes out; the other side stalls, 30 s ends the session
        syncMotionSensors(foreground = false)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // The core runs again only with the menu closed (closeMenu resumes it then); a quit never does.
        if (!menuOpen && !quitting) rumbles.forEach { it.resume() }
        if (!menuOpen) netplayGame?.resume()   // netplay: input flows again; with the menu open it stays paused until Continue
        syncMotionSensors(foreground = true)
    }

    /**
     * Leaving via Home/recents: grab the frame for the switcher preview now. This runs before the pause,
     * while the GL surface still holds a frame; by onPause GLRetroView has already paused it (no data).
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!quitting && gameReady && !resumePending) {
            val gen = ++homeThumbGen
            homeThumb = null
            captureThumbnail { bmp -> if (gen == homeThumbGen && bmp != null) onHomeThumb(bmp) }
        }
    }

    override fun onStart() {
        super.onStart()
        autoWrittenOnStop = false   // a later Home must write (and record) a fresh auto state
        autoWriteOk = null
        autoWriteFailed = null
        homeThumb = null
        homeThumbGen++   // a capture still in flight belongs to the previous stop
        pendingOutState = null
        if (isNds) syncPen()   // restarts the pen tick onStop stopped, if stylus mode is on
        if (isN3ds) syncN3dsCursor()
        // onStop closed the session: back in the foreground, a new one on the same game (background time is not play)
        gameTime?.let { if (!isFinishing && !quitting) { it.resume(); startGameTimeTicker(it) } }
    }

    /** Main thread. Before onStop's auto state: keep it for onStop. After it: queue it behind that state. */
    private fun onHomeThumb(bmp: Bitmap) {
        if (autoWrittenOnStop) queueHomeThumb(bmp) else homeThumb = bmp
    }

    /** Writes the slot-9 thumb after this stop's auto state, and only if that write succeeded (they must match). */
    private fun queueHomeThumb(bmp: Bitmap) {
        val ss = saveStates ?: return
        val ok = autoWriteOk ?: return
        autoWriter.execute {
            if (ok.get()) runCatching { ss.writeAutoThumb(bmp) }.onFailure { NxLog.w("GameActivity", "auto-resume thumb on stop failed", it) }
        }
    }

    override fun onStop() {
        gameTime?.pause()   // the session ends here: a kill in the background loses nothing, a return opens a new one
        gameTimeTicker?.cancel()
        gameTimeTicker = null
        // Leaving via Home/recents: the process may be killed in the background, so keep the spot. Also when
        // finishing without a quit: sharing the menu's task, a launch of the singleTask menu (app icon, notification)
        // clears this screen. A quit writes its own; a game that never got ready writes nothing (writeAutoStateOnStop);
        // one that failed after starting and leaves through its dialog keeps the last good resume point.
        if (!quitting && !relaunching && !(isFinishing && failShown)) writeAutoStateOnStop()
        // API 26–27: onSaveInstanceState already ran; record the outcome of the write in its bundle.
        pendingOutState?.let(::putResumeSlot)
        pendingOutState = null
        if (isFinishing) { gcClearTriggers(); stopAchievements() }
        penJob?.cancel()   // no 60 Hz pen tick in the background; onStart restarts it (stylus mode itself is kept)
        penJob = null
        if (isN3ds) {   // backgrounded: no held-back SELECT or forwarded key left hanging, and no tick
            n3dsHotkeys.releaseAll()
            sendN3dsKeys()
        }
        n3dsTickJob?.cancel()
        n3dsTickJob = null
        super.onStop()
    }

    /**
     * After a process kill the system recreates this activity with its original intent, so the slot to
     * resume from must travel in the instance state: the auto state onStop just wrote, or the requested
     * slot while its load is still pending. On API 28+ this runs after onStop; on 26–27 before it, so the
     * bundle is also updated from onStop (see [pendingOutState]).
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        putResumeSlot(outState)
        pendingOutState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) outState else null
    }

    /**
     * [autoWrittenOnStop] means the auto state was serialized and its write queued, not that it landed. When the write
     * is already known to have failed, the bundle keeps the previous resume point ([lastResumePoint]) rather than naming
     * a slot 9 this stop never replaced. Usually the write is still in flight here (onSaveInstanceState follows onStop at
     * once on API 28+, the write takes tens of ms on [autoWriter]) and the bundle is parcelled when this returns, so slot
     * 9 is recorded optimistically. The write is an atomic rename, so a failure leaves the previous slot-9 file whole: a
     * recreation resumes an older auto state (or toasts "Resume failed" when there was none), never a torn one. Blocking
     * the main thread on the write is not worth it for a rare failure.
     */
    private fun putResumeSlot(outState: Bundle) {
        val slot = when {
            autoWrittenOnStop && autoWriteFailed?.get() == true -> lastResumePoint
            autoWrittenOnStop -> NxPaths.AUTO_SLOT
            resumePending -> resumeSlot
            else -> null
        }
        if (slot != null) outState.putInt(KEY_RESUME_SLOT, slot) else outState.remove(KEY_RESUME_SLOT)
    }

    /**
     * Debug builds: game.json "stopped" once this screen finishes, queued behind any auto-state write on [autoWriter]
     * (a quit wrote its own before finish), with the slot-9 state file when this screen wrote it, else null.
     */
    private fun debugReportStopped() {
        if (debugGame?.get() === this) debugGame = null
        val app = applicationContext
        val auto = if (::sdRoot.isInitialized && ::paths.isInitialized) File(sdRoot, paths.state(NxPaths.AUTO_SLOT)) else null
        val since = debugStartedAt
        autoWriter.execute {
            val written = auto?.takeIf { it.isFile && it.lastModified() >= since }?.absolutePath
            NxTestResults.write(app, "game", "state" to "stopped", "autoState" to written)
        }
    }

    private fun writeAutoStateOnStop() {
        val view = retroView ?: return
        val ss = saveStates ?: return
        if (!gameReady || resumePending) return
        // Serialize synchronously on the GL thread: paused, it still runs queued events, and native makes the paused
        // view's context current for the call. A core may run emulation and GL inside it (Azahar's RunLoop drain), which
        // crashes on a thread without the context. The file writes go to [autoWriter] to keep them off the
        // main thread; they are atomic renames, so a reader sees the previous state or the new one.
        val t0 = SystemClock.elapsedRealtime()
        val bytes = runCatching { view.serializeState() }
            .onFailure { NxLog.e("GameActivity", "auto-resume state on stop failed", it) }.getOrNull()
        val serializeMs = SystemClock.elapsedRealtime() - t0
        if (bytes == null || bytes.isEmpty()) return
        autoWrittenOnStop = true
        val ok = AtomicBoolean(false).also { autoWriteOk = it }
        val failed = AtomicBoolean(false).also { autoWriteFailed = it }
        autoWriter.execute {
            val t1 = SystemClock.elapsedRealtime()
            runCatching {
                ss.writeAuto(bytes)
                ok.set(true)   // the state is replaced: its thumb must follow even if the slot pointer fails
                lastResumePoint = NxPaths.AUTO_SLOT
            }.onFailure { failed.set(true); NxLog.e("GameActivity", "auto-resume state on stop failed", it) }
            if (ok.get()) runCatching { ss.markAuto(onlyIfNotManual = true) }
                .onFailure { NxLog.e("GameActivity", "auto-resume slot pointer on stop failed", it) }
            runCatching { lastSlotCache = ss.lastSlot() }   // the pointer may name slot 9 (shown as slot 1)
            NxLog.i("GameActivity", "auto-state on stop: serialize=${serializeMs}ms write=${SystemClock.elapsedRealtime() - t1}ms bytes=${bytes.size}")
        }
        homeThumb?.let { queueHomeThumb(it) }
        homeThumb = null
    }

    override fun onDestroy() {
        rumbles.forEach { it.release() }
        motionSensors?.stop()
        if (isWii) LibretroDroid.setSensorEnabled(0, false)   // a next Wii game sets its own on its first frame
        if (isFinishing || relaunching) endGameTime()   // Back/finish without Quit (quit already ended it), or the GLES relaunch
        if (isFinishing) PendingLaunches.take(launchId)   // quit before the first frame: the launch never started
        if (BuildConfig.DEBUG && isFinishing) debugReportStopped()
        gcClearTriggers()   // a latched soft press never outlives the game
        stopAchievements()
        inputManager?.unregisterInputDeviceListener(inputDeviceListener)
        cacheHoldsDrained = true
        releaseCacheHolds()
        // ON_DESTROY goes to the lifecycle observers before this body (API 29+, onActivityPreDestroyed) or from within
        // super.onDestroy() (older): either way GLRetroView's LibretroDroid.destroy() has run by the release below.
        super.onDestroy()
        // netplay: after the native destroy (above), off the main thread: the engine shut down, a session copy deleted.
        // Only this screen's own claim is deleted: a newer launch's copy (committed before this teardown) is kept.
        netplay?.let { np ->
            val owner = netplayOwner
            NetplayGame.release(netplayGame, if (np.onCopy) ({ NetplaySessionDir.release(filesDir, owner) }) else null)
            // The cable carried this session (the sheet handed it over at launch), unless a newer launch's start took it over.
            netplayUsbToken?.takeIf { np.overUsb }?.let { UsbCables.get(this).stopIf(it) }
        }
        sessionClaim?.let { sessions.release(it.token) }
        sessionClaim = null
    }

    /** A foreground kill (crash, OOM) closes the session at the last touch: keep it at most a minute old while started. */
    private fun startGameTimeTicker(rec: GameTimeRecorder) {
        gameTimeTicker?.cancel()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) { gameTimeTicker = null; return }   // onStart restarts it
        gameTimeTicker = lifecycleScope.launch { while (true) { delay(60_000); rec.touch() } }
    }

    private fun endGameTime() {
        gameTimeTicker?.cancel(); gameTimeTicker = null
        gameTime?.end(); gameTime = null
    }

    private suspend fun startEmulator() {
        val coreFile = intent.getStringExtra(EXTRA_CORE_FILE) ?: return fail("No core given")
        // netplay: extras that make no session must not start an ordinary game on the real saves; a recreated session is over.
        if (intent.hasExtra(EXTRA_NETPLAY_ROLE) && netplay == null) return fail(NetplayText.FAILED, NetplayText.FAILED)
        if (netplayRestored) return fail(NetplayText.ENDED, NetplayText.FAILED)
        val np = netplay
        val tag = intent.getStringExtra(EXTRA_TAG) ?: "UNKNOWN"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "game"
        val coreId = intent.getStringExtra(EXTRA_CORE_ID) ?: coreFile.removePrefix("lib").substringBefore("_libretro")
        // The tag's own descriptor: a tag view (SGB) carries its own extensions and defaults; the id alone would give plain mgba.
        val descriptor = CoreRegistry.forTag(tag)?.takeIf { it.id == coreId } ?: CoreRegistry.forId(coreId)
        val players = CoreRegistry.playersFor(tag, descriptor)
        playerCount = players
        // netplay: no slots, so every pad and the touch pad drive native port 0, the engine's local word.
        slots = if (players > 1 && np == null) PlayerSlots(players, AndroidAppSettings(this).benchedPads.map(::DeviceKey).toSet()) else null
        // onCreate laid out before the slots existed: a pad benched in an earlier game counted there. Lay out again (only
        // when a benched pad is attached: one player, slots null, never re-lays out here).
        if (layoutControllerNames() != controllerNames()) applyLayout(resources.configuration.orientation)
        // Kept for one player: a key held through the load keeps its recorded send.
        if (inputs.count != players) inputs = PortInputs(players)
        rumbles = Array(players) { RumbleMotor(this, gameRumble, phoneFallback = it == 0) }
        cheatsSupported = np == null && (descriptor?.cheats ?: true)   // netplay: cheats would desync the other device
        // A SAF document URI, or a file ref with All files access: opened only through [refs], never Uri.parse.
        val content = ContentRef(intent.getStringExtra(EXTRA_CONTENT_URI) ?: return fail("No content given"))
        val romFileName = intent.getStringExtra(EXTRA_FILE_NAME)
            ?: withContext(Dispatchers.IO) { runCatching { refs.displayName(content) }.getOrNull() } ?: title
        sdRoot = NxStorage.root(this)
        cheatsFolder = Cheats.folder(tag, romFileName)
        // The battery-save, clock and state names follow the handheld's save settings. The save setup waits
        // for the unpack (after the inner-file check below), since Use extracted file name names them after the
        // archive's inner file; the config and marker names never depend on the settings.
        paths = NxPaths(tag, coreId, romFileName, AndroidAppSettings(this).saveNaming)
        val home = AndroidAppSettings(this).treeRef   // a SAF tree or a file ref: BiosImport picks the tree
        // netplay: a session on a copy never touches the real saves: its session folder is its own.
        val sessionDir = if (np?.onCopy == true) NetplaySessionDir.dir(filesDir) else null
        gameTag = tag
        gameCoreId = coreId
        // Normally done by MainActivity; here too for a restore from recents in a fresh process. Not awaited:
        // it is synchronized, and a first-run or post-update copy (~10 MB) must not hold up the game. Re-read
        // once it is done.
        val extraction = lifecycleScope.async(Dispatchers.IO) { Overlays.extractBundled(this@GameActivity) }
        val shaderExtraction = lifecycleScope.launch(Dispatchers.IO) { Shaders.extractBundled(this@GameActivity) }
        restartOnlyOptions = descriptor?.restartOnlyOptions ?: emptySet()
        forcedOptions = descriptor?.lockedOptions ?: emptySet()
        hiddenValues = descriptor?.hiddenValues ?: emptyMap()
        val realPath = intent.getStringExtra(EXTRA_LOAD_MODE) == "REAL_PATH"
        val companionNames = intent.getStringArrayExtra(EXTRA_COMPANION_NAMES) ?: emptyArray()
        val companionUris = intent.getStringArrayExtra(EXTRA_COMPANION_URIS) ?: emptyArray()
        val companions = companionNames.zip(companionUris).map { (name, ref) -> name to ContentRef(ref) }
        descriptorDefaults = (intent.getStringArrayExtra(EXTRA_OPTIONS) ?: emptyArray())
            .mapNotNull { kv -> kv.indexOf('=').takeIf { it > 0 }?.let { kv.substring(0, it) to kv.substring(it + 1) } }
            .toMap()
        // melonDS DS: GPU where the device has GLES 3.2, as a lowest-priority default; a stored value wins, none is written.
        if (coreId == NdsRenderer.CORE_ID_MELONDSDS) {
            val gles = (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).deviceConfigurationInfo.reqGlEsVersion
            descriptorDefaults = NdsRenderer.defaults(gles) + descriptorDefaults
        }
        val store = CoreConfigStore(File(sdRoot, paths.consoleConfig), File(sdRoot, paths.gameConfig)).also { configStore = it }
        val resolved = withContext(Dispatchers.IO) {
            val (console, game) = store.read()
            CoreConfig.resolve(descriptorDefaults, console, game, forcedOptions, hiddenValues)
        }
        configSource = resolved.source
        lockedOptions = resolved.locked
        storedValues = resolved.values
        ndsRenderMode = resolved.values[NdsRenderer.RENDER_MODE]
        n3dsCoreValues(resolved.values)
        val savedCheats = CheatSet.savedFrom(resolved.values, resolved.source)
        // The file Dolphin loads, once known: an archive's unpacked disc names its built-in cheats (completed below).
        val dolphinContent = CompletableDeferred<String>()
        // Off the critical path: the menu shows the set once it lands, the first-frame apply waits for it.
        cheatsLoad = if (np != null) null else lifecycleScope.launch {   // netplay: no cheats (a saved nx_cheats set included)
            val extensions = descriptor?.extensions.orEmpty()
            val resolvedSet = withContext(Dispatchers.IO) {
                // PSP cheat files are named by serial ("[ULUS-10466]"), which the disc knows and the file name rarely does.
                val serial = if (descriptor?.id == "ppsspp" && PspSerial.readable(romFileName)) PspSerialReader.read(refs, content, romFileName) else null
                Cheats.resolve(sdRoot, tag, romFileName, title, extensions, serial)
            }
            val combined = if (isDolphin) {
                dolphinBuiltInPath = DolphinCheats.builtInFile(filesDir.absolutePath, CoreRegistry.contentName(tag, dolphinContent.await()))
                val builtIn = readDolphinBuiltIn()
                // Dolphin writes its list at boot, only if absent: a first launch has none yet (see the first-frame re-resolve).
                if (builtIn == null) { dolphinPlayerCheats = resolvedSet; dolphinSavedCheats = savedCheats.orEmpty() }
                DolphinCheats.combine(builtIn, resolvedSet)
            } else resolvedSet
            cheatSet = savedCheats?.let(combined::withSaved) ?: combined
            menuModel?.let { it.setCheats(cheatSet.entries, cheatsFolder, cheatSet.expected); menuState.value = it.state }
        }
        // Resolved and decoded before the view exists, so the first frame already has its overlay (the configured one;
        // with none stored the default is None).
        // A DS game starts split (every DS layout draws its screens as regions), which shows no overlay: none is
        // preloaded, and it counts as split until its first frame says otherwise, so the configured one never flashes.
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val ndsAtStart = NdsOptions.appliesTo(tag)
        val (sets, preloaded) = withContext(Dispatchers.IO) {
            val found = Overlays.sets(sdRoot, tag)
            val overlay = FrontendSettings.parse(FrontendOptions.fromConfig(resolved.values)).overlay
            val file = if (ndsAtStart) null else OverlayResolver.resolve(tag, found, overlay, landscape)
            found to file?.let { f -> decodeOverlay(f)?.let { f.path to it } }
        }
        overlaySets = sets
        overlayBitmap = preloaded
        ndsSplit = ndsAtStart
        // nx_* (and legacy minarch_*) keys are frontend settings: never passed to the core.
        frontendValues = FrontendOptions.fromConfig(resolved.values, tag)
        // netplay: both devices run the console's default Sega pad (Auto), whatever this device's console or game setting;
        // the core then gets the handheld pak's pad for this tag and core (NetplayPads).
        if (np != null && PadMap.isSegaPadTag(tag)) frontendValues = frontendValues + (PadOptions.SEGA_PAD to SegaPadType.AUTO)
        val netplayPad = if (np != null) NetplayPads.forSession(tag, descriptor?.id) else null
        shaderValues = ShaderOptions.fromConfig(resolved.values)
        presetNames = withContext(Dispatchers.IO) { Shaders.presets(sdRoot) }
        val options = FrontendOptions.coreValues(resolved.values).map { (k, v) -> Variable(k, v) }
        // netplay link: gambatte gets its link options; a GBA link engine starts now, before the core loads, so
        // a joiner learns the host's gpsp_serial from its READY and runs with it (the handheld reloads instead).
        val linkKind = np?.let { NetplayCores.link(it.coreId) }
        val linkOptions: Map<String, String> = if (np == null || linkKind == null) emptyMap() else when (linkKind) {
            NetplayLinkKind.GBLINK -> NetplayLinkOptions.forSession(np, null)
            NetplayLinkKind.NETPACKET -> {
                val own = options.firstOrNull { it.key == NetplayLinkOptions.GPSP_SERIAL }?.value
                val mode = connectLink(np, own) ?: return fail(NetplayText.FAILED_CONNECT, NetplayText.FAILED)
                NetplayLinkOptions.forSession(np, mode)
            }
        }
        val frontend = FrontendSettings.parse(frontendValues)

        // An arcade set's BIOS that was not beside it must be in the system dir (Bios/FBN/ is copied there); a missing parent is
        // not refused (a non-merged clone carries its parent's ROMs), so only BIOS sets arrive here.
        val systemSets = intent.getStringArrayExtra(EXTRA_SYSTEM_SETS)?.toList() ?: emptyList()
        // flycast's set table: a DC zip/7z named after one of its NAOMI/Atomiswave sets is loaded as is and needs its BIOS
        // archive (naomi.zip, awbios.zip ...) in <system>/dc/; any other DC zip/7z (a zipped disc) is unpacked below.
        val naomiSets = if (descriptor?.id == "flycast" && Names.isArchive(romFileName)) {
            withContext(Dispatchers.IO) { NaomiSetsAsset.get(this@GameActivity) }
        } else NaomiSets.EMPTY
        val archiveAsIs = CoreRegistry.loadsArchive(tag, romFileName, naomiSets)
        val naomiBios = if (archiveAsIs) naomiSets.biosFor(Names.stripExtension(romFileName)) else null
        // The files this launch needs, looked up by name when a Bios folder will not list.
        val biosNames = CoreRegistry.requiredBios(tag).flatMap { it.anyOf } +
            (systemSets + listOfNotNull(naomiBios)).flatMap { listOf("$it.zip", "$it.7z") }
        // Usually a stamp check: MainActivity pre-warms the copy after an update; if it is still running, this waits on its lock.
        val systemDir = withContext(Dispatchers.IO) { SystemFiles.ensure(this@GameActivity) }
        // Bios/<TAG>/ lands in the core's BIOS folder: the system dir, or a subfolder of it (flycast's <system>/dc/).
        val biosDir = BiosFolders.subdir(tag).let { if (it.isEmpty()) systemDir else File(systemDir, it) }
        withContext(Dispatchers.IO) { BiosImport.sync(this@GameActivity, home, BiosFolders.forTag(tag), biosDir, biosNames) }
        // An archive is unpacked into the cache first; the core gets the inner file by real path, and everything keyed by
        // romFileName (config, cheats, thumbnails; saves and states unless Use extracted file name is on) keeps the archive
        // name, as on the Brick; unless the tag's core loads the archive as is (fbneo's romsets and flycast's known sets:
        // the zip is the game).
        // Only a real unpack outlasts the delay and shows the toast; it is taken down with the unpack, not left over the game.
        val unpacked: Unpacked? = if (!Names.isArchive(romFileName) || archiveAsIs) null else {
            var unpackToast: android.widget.Toast? = null
            val unpacking = lifecycleScope.launch { delay(400); unpackToast = toast("Unpacking $romFileName…") }
            try {
                val romExtensions = descriptor?.extensions ?: CoreRegistry.coreExtensions
                withContext(Dispatchers.IO) {
                    // flycast names the VMU after the file it is given: unpacked as <archive stem>.<ext>, the VMU is <stem>.A1.bin.
                    Archives.extract(this@GameActivity, content, romFileName, romExtensions, descriptor?.savesNamedAfterContent == true)
                        .also { keepCacheHold(it.file.parentFile!!.name) }
                }
            } catch (e: ArchiveException) {
                NxLog.w(TAG_LOG, "unpack failed: ${e.kind}", e)
                return fail(e.message ?: "Could not unpack $romFileName")
            } finally {
                unpacking.cancel()
                unpackToast?.cancel()
            }
        }
        val inner: File? = unpacked?.file
        val unpackedEntry = unpacked?.entryName
        dolphinContent.complete(inner?.name ?: romFileName)   // Dolphin names its cheat file after the inner disc, not the archive
        if (inner != null && descriptor != null && Names.extensionOf(inner.name) !in descriptor.extensions) {
            // Name a tag of the same console whose core runs the file only when one exists; otherwise the file is in the wrong folder.
            val alt = CoreRegistry.consoleOf(tag)?.tags?.firstOrNull { t ->
                t != tag && CoreRegistry.forTag(t)?.let { Names.extensionOf(inner.name) in it.extensions } == true
            }
            return fail(
                "$romFileName holds ${inner.name}, which ${descriptor.id} does not run. " +
                    if (alt != null) "Pick ${CoreRegistry.handheldTag(alt)} from the game's Emulator menu." else "Move it to the folder of the console it belongs to.",
            )
        }
        // minarch names saves after the unpacked file when Use extracted file name is on (ma_game.c:75-76): known only now.
        paths = paths.withInner(inner?.let { unpackedEntry })
        saveStates = if (np == null) SaveStates(sdRoot, paths) else null   // netplay: no save states, no auto-resume
        refreshSlots()   // warm for the first menu open
        dropStaleNdsResume(tag, coreId, romFileName)
        np?.let { prepareNetplaySaves(it, tag, romFileName) }
        // netplay: on a copy the battery save is the session folder's (read only: writeBatterySave skips it), under the
        // default names (the folder is mobile's own), with no clock file.
        val sessionPaths = if (sessionDir != null) NxPaths(tag, coreId, romFileName) else null
        // melonDS DS: a handheld Saves/NDS/<rom>.dsv that is the game's only save is played in place (not on a netplay copy).
        val dsvStem = if (coreId == NdsDsvSave.CORE_ID && sessionDir == null) paths.saveStem else null
        val battery = BatterySaves(sessionDir ?: File(sdRoot, paths.savesDir), sessionPaths ?: paths, dsvStem).also { batterySaves = it }
        val clock = if (sessionDir != null) null else ClockSave(File(sdRoot, paths.clock)).also { clockSave = it }
        val (sram, rtc) = withContext(Dispatchers.IO) {
            battery.load() to clock?.load()
        }
        NxLog.i(TAG_LOG, "battery save: ${battery.loadedFrom ?: "none"}")
        if (BuildConfig.DEBUG) NxTestResults.write(this, "battery", "loadedFrom" to battery.loadedFrom, "bytes" to (sram?.size ?: 0))
        // A damaged configured file is set aside as a .bak at the first save, then the game saves again. Not said for a
        // netplay copy: it is never written, so the file stays as it is.
        if (battery.damaged && sessionDir == null) toast("Save file is damaged — it will be kept as a .bak when the game saves")
        if (!battery.writable) toast(if (battery.loadedFrom == null) "Save file is damaged — not loaded" else "Save file is damaged — loaded ${battery.loadedFrom}, not saving")
        // Sega Controller Type (Console Settings): the cart header's I/O field under Auto, else the forced pad.
        val segaSetting = if (PadMap.isSegaPadTag(tag)) frontendValues[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO else null
        if (segaSetting != null) {
            val headerName = inner?.name ?: romFileName
            val header = if (!SegaHeader.readable(headerName)) null else withContext(Dispatchers.IO) {
                runCatching {
                    (inner?.inputStream() ?: refs.openInput(content))?.use { s ->
                        val buf = ByteArray(SegaHeader.READ_LENGTH)
                        var n = 0
                        while (n < buf.size) { val r = s.read(buf, n, buf.size - n); if (r < 0) break; n += r }
                        SegaHeader.padType(buf.copyOf(n))
                    }
                }.getOrNull()
            }
            segaHeader = header
            // The current value, not segaSetting: the menu may have changed it while the header was read.
            segaPad = netplayPad?.pad ?: SegaPadType.resolve(frontendValues[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO, header)
            dropPadProfile()   // the profile follows the resolved pad
        }
        // New 3DS Controls (Console Settings): Auto reads the title from the file's NCSD / NCCH header, off the main thread.
        if (N3dsOptions.appliesTo(tag)) {
            val headerName = inner?.name ?: romFileName
            n3dsTitle = if (!N3dsTitle.readable(headerName)) null else withContext(Dispatchers.IO) {
                runCatching { (inner?.inputStream() ?: refs.openInput(content))?.use(::readN3dsTitle) }
                    .onFailure { NxLog.w(TAG_LOG, "reading the 3DS header failed", it) }
                    .getOrNull()
            }
            NxLog.i(TAG_LOG, "3DS title: ${n3dsTitle?.titleId?.toString(16)} ${n3dsTitle?.productCode}")
            // The current value: the menu may have changed it while the header was read.
            val resolved = N3dsNewControls.resolve(frontendValues[N3dsOptions.NEW_CONTROLS], n3dsTitle)
            if (resolved != n3dsNew) {
                n3dsNew = resolved
                dropPadProfile()
                applyLayout(resources.configuration.orientation)   // the pad was composed without them in onCreate
            }
        }
        // Wii Controller and Motion Controls (Console Settings): Auto reads the disc ID and looks it up in GameTDB's table.
        val wiiDisc = if (!WiiOptions.appliesTo(tag) || !WiiDiscId.readable(inner?.name ?: romFileName)) null else withContext(Dispatchers.IO) {
            inner?.let(WiiDiscIdReader::read) ?: WiiDiscIdReader.read(refs, content, romFileName)
        }
        // A GameCube image under the WII tag (outside a gc folder): the plain pad, the C-stick as the right stick.
        if (wiiDisc?.gameCube == true) {
            wiiGameCube = true
            dropPadProfile()   // the GameCube pad, not the plain one
            applyLayout(resources.configuration.orientation)   // the pad was composed from the intent tag in onCreate
            NxLog.i(TAG_LOG, "GameCube disc ${wiiDisc.id} under the Wii tag: no Wii controls")
            menuModel?.let { refreshWiiMenu(it); menuState.value = it.state }   // a menu opened during the read drops its Wii rows
        }
        if (WiiOptions.appliesTo(tag) && !wiiGameCube) {
            val id = wiiDisc?.id
            wiiControls = withContext(Dispatchers.IO) { WiiControlsAsset.get(this@GameActivity).lookup(id) }
            val sensors = motionSensors ?: MotionSensors(this@GameActivity) { a, g ->
                LibretroDroid.setSensorValues(0, a.x, a.y, a.z, g.x, g.y, g.z)
            }.also { motionSensors = it }
            LibretroDroid.setSensorAvailable(sensors.hasAccel, sensors.hasGyro)
            // The rotation the level starts in: only a real rotation recentres, not the first unrelated configuration
            // change (a pad attached, a fold posture).
            lastRotation = displayRotation()
            // The current values: the menu may have changed them while the image was read.
            val pad = WiiController.resolve(frontendValues[WiiOptions.CONTROLLER], wiiControls)
            wiiPad = pad
            wiiMotionOn = WiiMotion.resolve(frontendValues[WiiOptions.MOTION], pad, wiiControls, sensors.hasSensors)
            LibretroDroid.setSensorEnabled(0, wiiMotionOn)
            NxLog.i(TAG_LOG, "Wii disc $id: $wiiControls -> $pad, motion $wiiMotionOn")
            dropPadProfile()
            applyLayout(resources.configuration.orientation)
            // A menu opened during the read: its Motion Controls row (left out until the sensors were known) and its Wii rows.
            menuModel?.let { refreshWiiMenu(it); menuState.value = it.state }
        }
        padMap = if (isDc) PadMap.IDENTITY else PadMap.forTag(tag, segaPad)
        // Only 3-button draws differently from the six the pad was composed with in onCreate: recompose it.
        if (segaPad == SegaPad.THREE) applyLayout(resources.configuration.orientation)
        val contentExtension = Names.extensionOf(inner?.name ?: romFileName)
        // A missing required BIOS: say which file goes where instead of letting the core fail silently.
        val present = withContext(Dispatchers.IO) { systemDir.list()?.toSet() ?: emptySet() }
        val biosPresent = if (biosDir == systemDir) present else withContext(Dispatchers.IO) { biosDir.list()?.toSet() ?: emptySet() }
        BiosCheck.missing(CoreRegistry.requiredBios(tag), contentExtension, biosPresent).firstOrNull()?.let {
            return fail(BiosCheck.message(tag, it))
        }
        // A NAOMI/Atomiswave set's BIOS archive (flycast opens <system>/dc/<bios>.zip or .7z); an unknown set is not checked.
        if (archiveAsIs) naomiSets.missingBios(romFileName, biosPresent)?.let { return fail(NaomiSets.biosMessage(title, it)) }
        // A missing arcade BIOS set (systemSets, read above) is refused here.
        if (systemSets.isNotEmpty()) {
            val fbneoFiles = withContext(Dispatchers.IO) { File(systemDir, "fbneo").list()?.toSet() ?: emptySet() }
            ArcadeCompanions.missing(systemSets, present, fbneoFiles)?.let { set ->
                return fail("$title needs $set.zip: put it next to the game or in Bios/FBN/")
            }
        }
        val savesDir = (sessionDir ?: File(sdRoot, paths.savesDir)).apply { mkdirs() }
        if (np == null) prepareAchievements(tag, romFileName, content, inner)   // netplay: no achievements

        // A REAL_PATH core's copy into the cache gets the unpack's toast: only one that outlasts the delay shows it.
        var copyToast: android.widget.Toast? = null
        val copying = if (realPath && inner == null) lifecycleScope.launch { delay(400); copyToast = toast("Copying $romFileName…") } else null
        // What the core is launched with, read now (a change during the header read or the BIOS checks included); a
        // change after this reaches the core at the first frame.
        val launchSega = if (segaSetting != null) frontendValues[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO else null
        val launchSegaPad = segaPad
        launchedSegaSetting = launchSega
        val data = try {
            withContext(Dispatchers.IO) {
                // The name the core sees: an arcade zip in lowercase (fbneo matches the driver name case-sensitively).
                val fileName = CoreRegistry.contentName(tag, romFileName)
                // Console Settings → Graphics API: the core options for the API and whether the runner offers Vulkan.
                // Vulkan that could not start here before, or this is the relaunch after it did: OpenGL ES (VulkanFallback).
                val vulkanFailed = intent.getBooleanExtra(EXTRA_FORCE_GLES, false) ||
                    descriptor?.id in AndroidAppSettings(this@GameActivity).vulkanFailedCores
                val graphics = GraphicsApiOptions.resolve(frontendValues[GraphicsApiOptions.KEY], descriptor?.vulkan, { LibretroDroid.isVulkanSupported() }, vulkanFailed)
                launchedOnVulkan = graphics.preferVulkan
                launchEnv = graphics.env
                graphicsForcedOptions = graphics.forcedOptions.keys
                GLRetroViewData(this@GameActivity).apply {
                    coreFilePath = coreFile
                    systemDirectory = systemDir.absolutePath
                    savesDirectory = savesDir.absolutePath
                    // netplay: the handheld pak's pad options (NetplayPads) replace the Controller Type's, and a link
                    // session's (NetplayLinkOptions) the user's, set even when absent.
                    val forced = netplayPad?.options.orEmpty() + linkOptions + wiiScreenOption() + graphics.forcedOptions
                    variables = (options.map { v ->
                        forced[v.key]?.let { Variable(v.key, it) }
                            ?: if (launchSega != null && (v.key == SegaPadType.PICODRIVE_INPUT1 || v.key == SegaPadType.PICODRIVE_INPUT2))
                                Variable(v.key, SegaPadType.picodriveInput(launchSegaPad)) else v
                    } + forced.filterKeys { k -> options.none { it.key == k } }.map { (k, v) -> Variable(k, v) }).toTypedArray()
                    rumbleEventsEnabled = gameRumble != GameRumble.OFF   // Game rumble Off: the runner does not even read the core's state
                    preferLowLatencyAudio = true
                    // GL driver messages and a frame probe to tag NXGL, with Settings → Developer → Debug log on at
                    // launch.
                    glDebugOutput = NxLog.enabled
                    // Console Settings → Graphics API ([graphics]): Vulkan frames are bridged into the GLES presentation.
                    preferVulkan = graphics.preferVulkan
                    vulkanSwapchainFrames = graphics.swapchainFrames
                    // gpsp: never unload the core between games, so its recompiler cache stays mapped near it.
                    keepCoreLoaded = descriptor?.keepLoaded == true
                    shader = shaderFor(frontend)
                    saveRAMState = sram
                    rtcState = rtc   // the clock file, applied after the battery save
                    // Set after load and before the first frame: pcsx_rearmed's DualShock (a gamepad's sticks reach the
                    // core), flycast's joypad (its ports start empty, so the BIOS and game would see no controller) with
                    // its unused ports set to none, which completes its device setup (the VMU in slot 1). Player 2+
                    // ports get the same device, as does a forced genesis_plus_gx Controller Type.
                    descriptor?.let { controllerPorts = it.controllerPortsFor(players) }
                    // Dolphin: port 0 and the slots a pad already holds get the game's device (Wii: the resolved controller,
                    // Dolphin's Wiimote + Nunchuk, Classic, MotionPlus); the other ports stay unplugged until a pad claims
                    // them (connectDolphinPorts), or Dolphin connects Wiimotes 2-4 and the game shows a co-star cursor.
                    if (descriptor?.id == DOLPHIN_ID) controllerPorts = dolphinPorts(dolphinDevice(), claimedPorts(), players)
                    if (descriptor?.id == SegaPadType.GPGX) SegaPadType.gpgxPorts(launchSega, players)?.let { controllerPorts = it }
                    netplayPad?.ports?.takeIf { it.isNotEmpty() }?.let { controllerPorts = controllerPorts + it }   // netplay: the pak's ports 0-1
                    if (inner != null) {
                        gameFilePath = inner.absolutePath
                    } else if (realPath) {
                        // A file ref (All files access) is opened in place when it and its companions are readable there;
                        // a SAF game is copied: a disc set's files go next to its sheet in the cache, under the names the sheet uses.
                        val inPlace = RealPathLaunch.inPlace(content, fileName, companions)
                            ?.takeIf { p -> File(p).canRead() && companions.all { (name, _) -> File(File(p).parentFile, name).canRead() } }
                        if (inPlace != null) {
                            gameFilePath = inPlace
                        } else {
                            for ((name, ref) in companions) copyToCache(ref, name)
                            gameFilePath = copyToCache(content, fileName).absolutePath
                        }
                    } else {
                        val pfd = refs.openFd(content, "r")
                            ?: throw IOException("Cannot open file descriptor")
                        // With more than one virtual file the runner serves the core through its VFS (libretrodroid.cpp:391),
                        // so the core opens /rom/<track>.bin by the name its cue gives.
                        val extra = companions.map { (name, ref) ->
                            VirtualFile("/rom/$name", refs.openFd(ref, "r") ?: throw IOException("Cannot open $name"))
                        }
                        gameVirtualFiles = listOf(VirtualFile("/rom/$fileName", pfd)) + extra
                    }
                }
            }
        } catch (e: Exception) {
            fail("Cannot open $title: ${e.javaClass.simpleName}: ${e.message}")
            return
        } finally {
            copying?.cancel()
            copyToast?.cancel()
        }

        // Graphics API's process environment for the core (ParaLLEl-RDP's switches), before it loads; unset otherwise.
        launchEnv.forEach { (k, v) ->
            runCatching { if (v != null) android.system.Os.setenv(k, v, true) else android.system.Os.unsetenv(k) }
                .onFailure { NxLog.w("GameActivity", "could not set $k", it) }
        }
        if (isN3ds) offerN3dsMotion()
        val view = GLRetroView(this, data)
        retroView = view
        // Collected before the view is added (its ON_CREATE) and long before the first frame: the runner reports a
        // change only, from an unbuffered flow, so a missed event would leave the motor wrong until the next one.
        if (gameRumble != GameRumble.OFF) lifecycleScope.launch {
            view.getRumbleEvents().collect { e -> if (!quitting) rumbles.getOrNull(e.port)?.onRumble(e.strengthWeak, e.strengthStrong) }
        }
        // The core asked to end (Dolphin: the game's HOME menu → Wii Menu, which is not installed): close as Quit does,
        // without a resume state, since the game has stopped itself by then. The resume point is HOME's ([pressWiiHome]).
        lifecycleScope.launch {
            view.getShutdownRequests().first()
            // Left without HOME this session (a game's own "Wii Menu", a channel launch): the resume point on disk is an
            // older session's, behind the game's own save on the NAND. Dropped, so the next launch boots fresh.
            if (!wiiHomeSaved) withContext(Dispatchers.IO) { saveStates?.let { ss -> autoWriter.submit { ss.dropAuto() }.get() } }
            quitWithAutoState(chosen = true, autoState = false)
        }
        // Subscribed before the login (startSession, after the first frame): events are not replayed.
        achievements?.let { b ->
            b.attach(view)
            lifecycleScope.launch { b.events.collect(::onAchievementEvent) }
        }
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        game.addView(view)
        lifecycle.addObserver(view)
        // Added after the view: LifecycleRegistry sends ON_DESTROY newest-first, so this runs before the view's
        // LibretroDroid.destroy(). It waits for a quit's serialize in flight (one call, milliseconds) and stops the next.
        lifecycle.addObserver(LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_DESTROY) synchronized(coreLock) { coreGone = true } })
        ensureOverlayView()
        ensureNotificationsView()
        // Size changes (first layout, rotation, cutout margin) re-lay out the picture and the overlay box.
        game.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) game.post { relayoutFrontend() }
        }
        applyFrontend(frontendValues)
        // Set before the first chain push: a chain built with the view reports its errors through this.
        view.onShaderError = { msg -> toast("Shader: $msg"); dropShaderChain() }
        // A stored preset reads the extracted files: on a first run or after an update, wait for the (~0.5 MB)
        // copy and swap rather than fail or read the tree mid-swap.
        if (ShaderOptions.preset(shaderValues) != null) shaderExtraction.join()
        applyShader(shaderValues)
        // First run or after an update: the bundled sets may still be copying; pick them up once swapped in.
        lifecycleScope.launch {
            val synced = extraction.await()
            val found = withContext(Dispatchers.IO) { Overlays.sets(sdRoot, tag) }
            // After a sync, re-apply even with the same names: an overlay decoded mid-swap may have failed.
            if (synced || found != overlaySets) {
                overlaySets = found   // the Overlay value is unchanged (its default is None whatever the files)
                applyFrontend(frontendValues)
            }
        }
        lifecycleScope.launch {
            shaderExtraction.join()
            presetNames = withContext(Dispatchers.IO) { Shaders.presets(sdRoot) }
            menuModel?.let { it.setShaders(shaderValues, presetNames, shaderParams); menuState.value = it.state }
        }
        lifecycleScope.launch {
            view.getGLRetroEvents().first { it is GLRetroView.GLRetroEvents.FrameRendered }
            gameReady = true
            if (BuildConfig.DEBUG) NxTestResults.write(this@GameActivity, "game", "state" to "running", "tag" to gameTag, "core" to gameCoreId)
            intent.getStringExtra(EXTRA_GAME_PATH)?.let { path ->
                val tag = gameTag
                val name = intent.getStringExtra(EXTRA_TITLE) ?: path.substringAfterLast('/')
                lifecycleScope.launch {
                    val rec = withContext(Dispatchers.IO) { GameTimeService.recorder(this@GameActivity) }
                    rec.start(path, tag, name)
                    gameTime = rec
                    // Ready while stopped (onStop already ran): close it at once, as onStop would; onStart resumes it.
                    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) rec.pause()
                    startGameTimeTicker(rec)
                }
            }
            // A Controller Type change made while the game loaded was held back (applySegaPad): send it now.
            launchedSegaSetting?.let { launched ->
                val now = frontendValues[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO
                if (now != launched) sendSegaPad(now)
            }
            if (isDolphin && !isWii) sendWiiDevice()   // GameCube: a slot claimed while the game loaded
            if (isWii) {
                view.stickyPointer = true   // the right stick moves the IR pointer, which stays where it was left
                // Always, not only on a change made during load: a previous game's core unload (its onDestroy can run
                // after this screen's onCreate) resets the native sensor state, and resending the device makes Dolphin
                // ask for the sensors again (cores/patches/dolphin.patch).
                motionSensors?.let { LibretroDroid.setSensorAvailable(it.hasAccel, it.hasGyro) }
                LibretroDroid.setSensorEnabled(0, wiiMotionOn)
                sendWiiDevice()
                syncMotionSensors()
                if (wiiControls?.motionPlus == true && motionSensors?.hasSensors != true) {
                    toast("Motion actions in this game need a phone with a gyroscope")
                }
            }
            if (isN3ds) {
                offerN3dsMotion()   // again: a previous game's unload can reset the native sensor state after the launch's offer
                syncMotionSensors()
            }
            relayoutFrontend()   // the core's geometry (a DS game's 256x384) is only known now
            // melonDS DS falls back to software when its GL renderer can't start: say so once (nothing is saved, the
            // next launch tries the GPU again). ~30 frames lets the core's first hardware frames arrive.
            if (NdsOptions.appliesTo(gameTag) && ndsRenderMode == NdsRenderer.OPENGL) {
                lifecycleScope.launch {
                    view.getGLRetroEvents().filter { it is GLRetroView.GLRetroEvents.FrameRendered }.take(30).collect {}
                    if (NdsRenderer.showFallbackNotice(ndsRenderMode, view.isHardwareRendered())) {
                        toast("GPU renderer unavailable — using software")
                    }
                }
            }
            // A frame means the core loaded the game (a load error aborts the view before any): the launcher may record it.
            PendingLaunches.take(launchId)?.invoke()
            // netplay: the engine starts once the core has loaded and can serialize, so a handheld's 10 s state
            // waits begin only now. The host listens (its 45 s CONNECTING limit counts from here); the client connects
            // with its 60 × 500 ms retries. A menu or background already in place pauses it at once.
            if (np != null) {
                if (!startNetplay(np)) {
                    // The game must not play on (or be heard) behind the dialog, a menu closed meanwhile included.
                    netplayStartFailed = true
                    retroView?.frameSpeed = 0
                    retroView?.audioEnabled = false
                    fail(NetplayText.FAILED, NetplayText.FAILED)
                    return@launch
                }
                if (menuOpen || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) netplayGame?.pause()
                val now = SystemClock.elapsedRealtime()
                netplayFirstFrameNotices(np, descriptor?.name ?: coreId, netplayOwnCopyFailed).forEach {
                    notifications.push(it, null, NETPLAY_NOTICE_MS, now)
                }
                publishNotifications()
            }
            val slot = resumeSlot
            if (slot != null) resumeFrom(view, slot) else resumePending = false
            // Cheats sit on top of the resumed state, as minarch applies them after load.
            cheatsLoad?.join()
            refreshDolphinCheats()
            if (cheatSet.enabledCodes().isNotEmpty()) applyCheats(view)
            achievements?.startSession()
        }
        // A refused game: the core's own reason (flycast names the NAOMI/Atomiswave BIOS or ROM file it could not find).
        val naomiParent = if (archiveAsIs) naomiSets.parent(Names.stripExtension(romFileName)) else null
        observeErrors(view, coreFile) { lines ->
            LoadFailure.message(title, lines, romFileName.takeIf { archiveAsIs }, naomiBios, naomiParent)
        }
        // Debug builds: a file named simulate_vulkan_fail in the app's files dir makes a Vulkan launch take the fallback
        // as a failed bridge setup would (no phone at hand fails it): `adb shell run-as <package> touch files/simulate_vulkan_fail`.
        if (BuildConfig.DEBUG && launchedOnVulkan && !intent.hasExtra(EXTRA_NETPLAY_ROLE) && File(filesDir, SIMULATE_VULKAN_FAIL).exists()) {
            relaunchOnGles()
        }
    }

    /**
     * NDS moved from melonds to melonDS DS: a resume marker left pointing at an old-core state (which melonDS DS can't
     * load) is deleted with its thumbnails, and this launch boots fresh instead of toasting "Resume failed". Once per
     * game, since the marker is gone afterwards; the old core's states stay untouched.
     */
    private suspend fun dropStaleNdsResume(tag: String, coreId: String, romFileName: String) {
        if (tag != NdsResumeCleanup.TAG || coreId != NdsResumeCleanup.CORE_ID) return
        val dropped = withContext(Dispatchers.IO) {
            awaitAutoWrites()   // a Home write of this game's auto state may still be landing
            runCatching {
                val marker = File(sdRoot, paths.lastSlot).takeIf { it.exists() }?.readText()
                val victims = NdsResumeCleanup.victims(tag, coreId, romFileName, marker, paths.naming, paths.innerFileName) { File(sdRoot, it).exists() }
                victims.forEach { File(sdRoot, it).delete() }
                victims.isNotEmpty()
            }.onFailure { NxLog.w("GameActivity", "stale NDS resume cleanup failed", it) }.getOrDefault(false)
        }
        if (dropped) {
            resumeSlot = null
            resumePending = false
        }
    }

    /** Loads [slot] right after start, like minarch's State_resume; a failure keeps the fresh boot. */
    private suspend fun resumeFrom(view: GLRetroView, slot: Int) {
        var ok = false
        try {
            val ss = saveStates ?: return
            ok = withContext(Dispatchers.IO) {
                awaitAutoWrites()   // a Home write of this game may still be landing
                runCatching { ss.readSlotState(slot)?.let { view.unserializeState(it) } ?: false }.getOrDefault(false)
            }
            if (!ok) toast("Resume failed")
        } finally {
            if (BuildConfig.DEBUG) {
                NxTestResults.write(this, "resume", "slot" to slot, "ok" to ok, "path" to File(sdRoot, paths.state(slot)).absolutePath)
            }
            resumePending = false
        }
    }

    private fun observeErrors(view: GLRetroView, coreFile: String, loadFailure: (List<String>) -> String?) {
        lifecycleScope.launch {
            view.getGLRetroErrors().flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { code ->
                if (code == GLRetroView.ERROR_LOAD_LIBRARY) {
                    val lines = runCatching { view.getRecentCoreErrors() }.getOrDefault(emptyList())
                    fail(CoreLoadFailure.message(coreFile, lines))
                    return@collect
                }
                val name = when (code) {
                    GLRetroView.ERROR_LOAD_GAME -> "ERROR_LOAD_GAME"
                    GLRetroView.ERROR_GL_NOT_COMPATIBLE -> "ERROR_GL_NOT_COMPATIBLE"
                    GLRetroView.ERROR_SERIALIZATION -> "ERROR_SERIALIZATION"
                    GLRetroView.ERROR_CHEAT -> "ERROR_CHEAT"
                    else -> "ERROR_GENERIC($code)"
                }
                if (code == GLRetroView.ERROR_LOAD_GAME) {
                    val lines = runCatching { view.getRecentCoreErrors() }.getOrDefault(emptyList())
                    lines.forEach { NxLog.i(TAG_LOG, "core said: $it") }
                    loadFailure(lines)?.let { fail(it); return@collect }
                }
                if (code == GLRetroView.ERROR_GL_NOT_COMPATIBLE && VulkanFallback.relaunchOnGles(launchedOnVulkan)) {
                    // netplay: a relaunch would start without the session (it is gone), so the dialog says what to do.
                    if (intent.hasExtra(EXTRA_NETPLAY_ROLE)) {
                        fail("Vulkan could not start for this game on this device. Set Console Settings → Graphics API to OpenGL ES.")
                    } else {
                        relaunchOnGles()
                    }
                    return@collect
                }
                fail("The core reported $name")
            }
        }
    }

    /**
     * Vulkan passed the support probe but could not start ([VulkanFallback]): the core is recorded on this
     * phone and the screen is recreated with this launch's extras plus [EXTRA_FORCE_GLES] (the same game, resume slot
     * and launch, which the new screen reports to the launcher). Recreated, not finished and started again: a start from
     * a finishing activity is forced into a new task (the game would leave the menu's task), and singleTop would hand a
     * live one the intent. The old screen's onDestroy (the core's native destroy, the session release) runs before the
     * new onCreate; not finishing, it keeps the launch for it.
     */
    private fun relaunchOnGles() {
        if (isFinishing || relaunching || failShown) return
        relaunching = true
        AndroidAppSettings(this).recordVulkanFailure(gameCoreId)
        NxLog.w(TAG_LOG, "Vulkan could not start for $gameCoreId: starting again on OpenGL ES")
        android.widget.Toast.makeText(applicationContext, VulkanFallback.NOTICE, android.widget.Toast.LENGTH_LONG).show()
        intent = Intent(intent).putExtra(EXTRA_FORCE_GLES, true)
        recreate()
    }

    /** [title]: "Netplay failed" for a netplay session's failures, which may come mid-game. */
    private fun fail(message: String, title: String = "Could not start the game") {
        NxLog.e(TAG_LOG, message)
        if (relaunching) return   // a late error from the Vulkan attempt: the recreated screen owns the launch now
        PendingLaunches.take(launchId)   // refused: the launcher must not record it
        if (isFinishing || failShown) return   // observeErrors can report more than once: one dialog, never a stack
        failShown = true
        rumbles.forEach { it.release() }
        // A controller's A only focuses the button (Android clicks on ENTER): A, B and BACK leave on release. A release
        // needs its press here first, so a button held from the game as the dialog opens does not close it unseen.
        var pressed = -1
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Back") { _, _ -> finish() }
            .setOnKeyListener { dialog, keyCode, event ->
                val leaves = keyCode == KeyEvent.KEYCODE_BACK || PadKeymap.buttonFor(keyCode).let { it == NxButton.A || it == NxButton.B }
                if (leaves) when (event.action) {
                    KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) pressed = keyCode
                    KeyEvent.ACTION_UP -> if (pressed == keyCode) { dialog.dismiss(); finish() }
                }
                leaves
            }
            .show()
            .also { dialog ->
                // Android 16+ (targetSdk 36) sends Back to the dialog's callbacks, not its key listener: leave the same way.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) dialog.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                ) { dialog.dismiss(); finish() }
            }
    }

    /**
     * Records a RomCache hold this game took, for onDestroy to release. The blocking unpack/copy that took it runs on
     * after a back-out cancels lifecycleScope, so it may land after onDestroy drained: then it is released here. Either
     * this sees [cacheHoldsDrained] after its add, or onDestroy's drain (after setting it) sees the add; poll releases once.
     */
    private fun keepCacheHold(name: String) {
        cacheHolds += name
        if (cacheHoldsDrained) releaseCacheHolds()
    }

    private fun releaseCacheHolds() {
        while (true) RomCache.release(cacheHolds.poll() ?: break)
    }

    /** Opens the game's content and companions, SAF documents or file refs. */
    private val refs by lazy { AndroidRefs(contentResolver) }

    /**
     * A top-level RomCache entry, held for this game; a copy whose stamp still matches the source is reused (and touched:
     * its mtime is its last use). The stamp, `<file name>.nxstamp` beside it, is the source's size
     * and last-modified ([realPathCopyStamp]): a same-size patched ROM is copied again. The copy goes to `<file name>.part`
     * and is renamed into place, the old stamp dropped first, so a kill mid-copy leaves no torn copy that passes for whole.
     */
    private fun copyToCache(ref: ContentRef, fileName: String): File {
        val root = RomCache.root(this).apply { mkdirs() }
        val target = File(root, fileName)
        val stampFile = File(root, cacheSiblingName(fileName, REAL_PATH_STAMP))
        val part = File(root, cacheSiblingName(fileName, ".part"))
        synchronized(RomCache.lockFor(target.name)) {
            RomCache.hold(target.name); keepCacheHold(target.name)
            RomCache.hold(stampFile.name); keepCacheHold(stampFile.name)   // a swept stamp would only cost a recopy, but keep it
            val size = refs.size(ref) ?: -1L
            val stamp = realPathCopyStamp(size, refs.lastModified(ref))
            val stored = runCatching { stampFile.takeIf { it.isFile }?.readText() }.getOrNull()
            if (reuseRealPathCopy(target.isFile, target.length(), size, stored, stamp)) {
                RomCache.touch(stampFile)
                return target.also { RomCache.touch(it) }
            }
            // The .part is its own top-level name to the sweep: held while it grows, so a big ISO's is never trimmed mid-copy.
            RomCache.hold(part.name)
            try {
                val input = refs.openInput(ref) ?: throw IOException("Cannot open input stream")
                input.use { source -> part.outputStream().use { source.copyTo(it, 1 shl 20) } }
                stampFile.delete()   // describes the copy being replaced; a kill before the new stamp only costs a recopy
                if (!part.renameTo(target)) throw IOException("Cannot move ${part.name} into place")
                // An unknown size never matches (reuseRealPathCopy), so its stamp is not worth writing.
                if (size >= 0) runCatching { stampFile.writeAtomically(stamp.encodeToByteArray()) }
                    .onFailure { NxLog.w("GameActivity", "could not stamp the cache copy of $fileName", it) }
            } finally {
                part.delete()
                RomCache.release(part.name)
            }
            return target
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ndsTouchMode = false   // a rotation starts with the pad shown
        applyLayout(newConfig.orientation)   // also re-sizes the menu and the notifications to the game area
        // The motion level is captured in the display's frame: a new rotation takes the phone's attitude as level again.
        if (isWii) displayRotation().let { r -> if (r != lastRotation) { lastRotation = r; motionSensors?.recenter() } }
        if (retroView != null) {
            // The overlay set follows the orientation (768p portrait, 720p landscape); the size change re-lays out.
            updateOverlay(FrontendSettings.parse(frontendValues))
            findViewById<FrameLayout>(R.id.gameContainer).post { relayoutFrontend() }
        }
    }

    /**
     * The one landscape test for the pad layout ([applyLayout]) and the DS layout ([ndsLayout], its hotkeys): anything not
     * portrait, so the landscape pad clusters and the DS landscape layout (with its pad-over-game touch rule) always agree.
     */
    private fun isLandscape(orientation: Int = resources.configuration.orientation): Boolean =
        orientation != Configuration.ORIENTATION_PORTRAIT

    /** Positions the game container and (re)builds the Compose pad(s) for [orientation]. */
    private fun applyLayout(orientation: Int) {
        val root = findViewById<FrameLayout>(R.id.root)
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        padViews.forEach { root.removeView(it) }
        padViews.clear()
        ndsChipView = null
        dpadHat.clear()
        overlayPadShown.value = false   // set again below where the landscape clusters are added
        layoutPadHidden = portraitPadHidden()
        portraitClusterTop = null

        val portrait = !isLandscape(orientation)
        val pads = controllers()
        val attached = pads.map { it.name }
        if (attached != loggedControllers) {
            loggedControllers = attached
            NxLog.i(TAG_LOG, "controllers: $attached")
        }
        // The hints follow any attached pad (a Not playing one still drives the menu).
        controllerConnected.value = pads.isNotEmpty()
        // What the layout goes by: a multiplayer game's Not playing pads leave the on-screen pad up for Player 1.
        val names = layoutControllerNames(pads)
        val tt = tabletop(names)
        if (tt != null) {
            applyTabletopLayout(root, game, tt, names, portrait, orientation)
            return
        }
        if (portrait) {
            // Portrait keeps the bottom pad band for a clip-on controller, so it has its placeholder and the game stays in
            // the top area. Any other controller drops it (PadBandRule): no pad, and the game area runs to the screen bottom.
            // A DS / 3DS player can hide the band (Portrait Pad): the game runs to the bottom too, under the clusters.
            val bandPx = portraitBandPx(names)
            game.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { bottomMargin = bandPx; topMargin = cutoutTop }
            if (bandPx > 0) {
                val bandLayout = padLayout(PadShape.PORTRAIT)
                val pad = composePad { PadThemeProvider(padTheme) { VirtualPad(::onPad, Modifier.fillMaxSize(), analog = true, onStick = ::onStick, profile = padProfile(), useJoystick = PadStickModes[padStickKey, padProfile().stickByDefault], onUseJoystickChange = ::onPadStickMode, onCStick = padCStick(), motion = padMotion(), latched = gcLatched.value, layout = bandLayout) } }
                addPad(root, pad, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, bandPx, Gravity.BOTTOM))
            } else if (layoutPadHidden && names.isEmpty()) {
                // The clusters over the lower half only: the upper screen (the touch screen, once swapped up) stays free to touch.
                // A controller hides them, as in landscape.
                val height = resources.displayMetrics.heightPixels / 2
                addOverlayClusters(root, height)
                portraitClusterTop = resources.displayMetrics.heightPixels - height
            }
            if (PadOptions.hasPortraitPadRow(layoutTag()) && PadBandRule.keepPadBand(portrait = true, names)) {
                val hidden = layoutPadHidden
                val toggle = composePad { PortraitPadToggle(hidden, if (hidden) padLandscapeAlpha.value else 1f, ::togglePortraitPad) }
                addPad(root, toggle, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END))
            }
        } else {
            // Landscape: full-screen game. Show the transparent corner clusters only when no controller
            // is attached; a connected controller hides them for an unobstructed screen.
            game.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            if (names.isEmpty() && ndsTouchMode) {
                // DS touch mode: no clusters, only the chip that brings them back (and turns bottom-screen touch off). Its
                // corner follows the touch-mode regions once relayoutFrontend lays them out (placeNdsChip).
                val chip = composePad {
                    ControlsChip(NdsTouchMode.chipAlpha(padLandscapeAlpha.value), ndsChipCorner.value, ::onNdsChipSize, ::leaveNdsTouchMode)
                }
                ndsChipView = chip
                addPad(root, chip, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, ndsChipCorner.value.gravity()))
            } else if (names.isEmpty()) {
                addOverlayClusters(root, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        }
        updateTouchModeAvailable(names, landscape = !portrait)
        game.requestLayout()
        updatePadVisibility()
        menuView?.layoutParams = menuLayoutParams(orientation)
        notificationsView?.layoutParams = menuLayoutParams(orientation)
        // The DS layouts follow the new area (gameContainer's size listener does too, once it is laid out).
        game.post { relayoutFrontend() }
    }

    /**
     * The two pad clusters over the game, [height] px tall from the bottom: the landscape pad, and portrait's with the
     * band hidden ([portraitPadHidden]).
     */
    private fun addOverlayClusters(root: FrameLayout, height: Int) {
        // Half the width each, so the two never overlap; the unit is the height's ([clusterUnit]), and only a
        // screen too narrow for two of those (the Flip's cover, portrait) fits it to the half.
        val halfW = (resources.displayMetrics.widthPixels * CLUSTER_MAX_WIDTH_SHARE).toInt()
        // The clusters overlay the game: drawn at the Pad Opacity (landscape) setting, touch targets unchanged.
        val overlayLayout = padLayout(PadShape.OVERLAY)
        val left = composePad {
            VirtualPadLeft(::onPad, ::onStick, Modifier.fillMaxSize().clearOfCutoutBothSides(WindowInsetsSides.Start).alpha(padLandscapeAlpha.value), profile = padProfile(),
                useJoystick = PadStickModes[padStickKey, padProfile().stickByDefault], onUseJoystickChange = ::onPadStickMode, overGame = true, motion = padMotion(), latched = gcLatched.value, layout = overlayLayout)
        }
        val right = composePad {
            VirtualPadRight(::onPad, Modifier.fillMaxSize().clearOfCutoutBothSides(WindowInsetsSides.End).alpha(padLandscapeAlpha.value), profile = padProfile(), overGame = true, onCStick = padCStick(), latched = gcLatched.value, layout = overlayLayout)
        }
        addPad(root, left, FrameLayout.LayoutParams(halfW, height, Gravity.START or Gravity.BOTTOM))
        addPad(root, right, FrameLayout.LayoutParams(halfW, height, Gravity.END or Gravity.BOTTOM))
        overlayPadShown.value = true
    }

    /** The tag the layout goes by: the intent's until startEmulator sets [gameTag] (onCreate lays out before it). */
    private fun layoutTag(): String = intent.getStringExtra(EXTRA_TAG) ?: gameTag

    /** DS / 3DS Portrait Pad is Hidden: portrait drops the band for the clusters over the game ([PadOptions.portraitPadRow]). */
    private fun portraitPadHidden(): Boolean = PadOptions.portraitPadHidden(layoutTag(), frontendValues)

    /**
     * The corner icon's long press: the portrait band hidden or back, applied now and saved for the console (and the
     * game's own file, when it has one, so the change is not hidden by it).
     */
    private fun togglePortraitPad() {
        val key = PadOptions.PORTRAIT_PAD
        if (key in lockedOptions) { toast("Portrait Pad is locked in the config"); return }
        val next = if (portraitPadHidden()) PadOptions.SHOWN else PadOptions.HIDDEN
        releaseAllGameInput()   // the pad goes away under the fingers: nothing it held may stick in the core
        applyFrontend(frontendValues + (key to next))
        val store = configStore ?: return
        lifecycleScope.launch {
            val written = configWrites.withLock {
                withContext(Dispatchers.IO) {
                    runCatching { store.saveConsoleKey(key, next) }
                        .onFailure { NxLog.w("GameActivity", "saving $key failed", it) }.getOrNull()
                }
            }
            if (written == null) { toast("Could not save Portrait Pad"); return@launch }
            if (configSource == ConfigSource.NONE) configSource = ConfigSource.CONSOLE
            lockedOptions = written.locked + forcedOptions
        }
    }

    /**
     * Flex mode: the game in [tt]'s area above the hinge, the controls (portrait pad or the two clusters, drawn solid
     * since they do not cover the game here) filling the half below it, and nothing there with a controller attached.
     */
    private fun applyTabletopLayout(
        root: FrameLayout, game: FrameLayout, tt: TabletopLayout, names: List<String>, portrait: Boolean, orientation: Int,
    ) {
        game.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, tt.gameBottom - tt.gameTop, Gravity.TOP)
            .apply { topMargin = tt.gameTop }
        val padH = root.height - tt.padTop
        when (tt.pad) {
            TabletopPad.PORTRAIT -> {
                val portraitLayout = padLayout(PadShape.PORTRAIT)
                val pad = composePad { PadThemeProvider(padTheme) { VirtualPad(::onPad, Modifier.fillMaxSize(), analog = true, onStick = ::onStick, profile = padProfile(), useJoystick = PadStickModes[padStickKey, padProfile().stickByDefault], onUseJoystickChange = ::onPadStickMode, onCStick = padCStick(), motion = padMotion(), latched = gcLatched.value, layout = portraitLayout) } }
                addPad(root, pad, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, padH, Gravity.BOTTOM))
            }
            TabletopPad.CLUSTERS -> {
                val halfW = (root.width * CLUSTER_MAX_WIDTH_SHARE).toInt()
                val left = composePad {
                    VirtualPadLeft(::onPad, ::onStick, Modifier.fillMaxSize().clearOfCutout(WindowInsetsSides.Start), profile = padProfile(),
                        useJoystick = PadStickModes[padStickKey, padProfile().stickByDefault], onUseJoystickChange = ::onPadStickMode, motion = padMotion(), latched = gcLatched.value)
                }
                val right = composePad {
                    VirtualPadRight(::onPad, Modifier.fillMaxSize().clearOfCutout(WindowInsetsSides.End), profile = padProfile(), onCStick = padCStick(), latched = gcLatched.value)
                }
                addPad(root, left, FrameLayout.LayoutParams(halfW, padH, Gravity.START or Gravity.BOTTOM))
                addPad(root, right, FrameLayout.LayoutParams(halfW, padH, Gravity.END or Gravity.BOTTOM))
            }
            TabletopPad.NONE -> Unit
        }
        updateTouchModeAvailable(names, landscape = !portrait)
        game.requestLayout()
        updatePadVisibility()
        menuView?.layoutParams = menuLayoutParams(orientation)
        notificationsView?.layoutParams = menuLayoutParams(orientation)
        game.post { relayoutFrontend() }
    }

    /** The tabletop split for the current [posture] and root size, or null (flat, a DS game, root not laid out yet). */
    private fun tabletop(names: List<String> = layoutControllerNames()): TabletopLayout? {
        if (posture == Posture.Flat) return null
        val root = findViewById<FrameLayout>(R.id.root)
        if (root.width <= 0 || root.height <= 0) return null
        return tabletopLayout(posture, root.width, root.height, cutoutTop, controllerConnected = names.isNotEmpty(), isDs = isNds)
    }

    /**
     * Starts [postureTracker], and re-reads the posture when the root's size changes (the first report can come before
     * the root is laid out; a rotation resizes it). Each posture change re-runs [applyLayout] ([onPosture]).
     */
    private fun collectPosture() {
        val root = findViewById<FrameLayout>(R.id.root)
        root.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or_, ob ->
            if (r - l != or_ - ol || b - t != ob - ot) root.post { postureTracker.update() }
        }
        postureTracker.start()
    }

    private fun onPosture(next: Posture, angle: Float?) {
        posture = next
        val root = findViewById<FrameLayout>(R.id.root)
        NxLog.i(TAG_LOG, "posture: $next angle=$angle (root ${root.width}x${root.height})")
        applyLayout(resources.configuration.orientation)
    }

    /**
     * The portrait pad band's height: the [PAD_PORTRAIT_CM] band, or 0 when [PadBandRule] drops it (a non-clip-on
     * controller, [names] from [layoutControllerNames]) or the player hid it ([portraitPadHidden]).
     */
    private fun portraitBandPx(names: List<String> = layoutControllerNames()): Int {
        if (!PadBandRule.keepPadBand(portrait = true, names) || portraitPadHidden()) return 0
        val dm = resources.displayMetrics
        return padBandPx(PAD_PORTRAIT_CM, dm.ydpi, dm.density, dm.heightPixels.toFloat()).toInt()
    }

    /** Portrait keeps the game below a top camera cutout; landscape leaves the (centred) picture full-screen. */
    private fun applyGameTopMargin(orientation: Int) {
        if (tabletop() != null) {   // the tabletop game area starts at the cutout too: lay it out again whole
            applyLayout(orientation)
            return
        }
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        val lp = game.layoutParams as? FrameLayout.LayoutParams ?: return
        val top = if (orientation == Configuration.ORIENTATION_PORTRAIT) cutoutTop else 0
        if (lp.topMargin != top) {
            lp.topMargin = top
            game.layoutParams = lp
        }
    }

    /**
     * Landscape: the corner pads overlap the full-screen menu, so hide them while it is open and let touch
     * reach the menu; so does portrait with the band hidden. Portrait keeps its pad band: it sits below the menu and drives it.
     */
    private fun updatePadVisibility() {
        // Tabletop keeps its controls too: they sit below the hinge, under the menu's area.
        val overGame = resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT || layoutPadHidden
        val hide = menuOpen && overGame && tabletop() == null
        padViews.forEach { it.visibility = if (hide) View.GONE else View.VISIBLE }
    }

    /** The non-virtual gamepads/joysticks currently attached (wired or Bluetooth); empty for none. */
    private fun controllers(): List<InputDevice> {
        val im = inputManager ?: return emptyList()
        return im.inputDeviceIds.toList().mapNotNull { id -> im.getInputDevice(id) }.filter(::isPad)
    }

    /** The names of every attached [controllers]: logged, and a change ends DS touch mode ([onInputDevicesChanged]). */
    private fun controllerNames(): List<String> = controllers().map { it.name }

    /**
     * The names of the [pads] that count as attached for the layout (the on-screen pad, the band rule, tabletop, DS touch
     * mode; the hint glyphs follow every pad): in a multiplayer game not the Not playing ones ([PadPresence]); one player, all.
     */
    private fun layoutControllerNames(pads: List<InputDevice> = controllers()): List<String> =
        PadPresence.attached(pads, slots?.outOfPlay) { DeviceKey(it.descriptor) }.map { it.name }

    private fun composePad(content: @Composable () -> Unit): ComposeView =
        ComposeView(this).apply {
            setContent {
                NxTheme { PadVibrationProvider(padVibration) { content() } }
            }
        }

    /**
     * Moves the touch-mode "Controls" chip to the first game-area corner clear of the touchable screen ([ndsRegions], so
     * called whenever they change). The game area is the whole root in landscape, so its corners are the chip's.
     */
    private fun placeNdsChip() {
        val chip = ndsChipView ?: return
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        if (game.width <= 0 || game.height <= 0) return
        val d = resources.displayMetrics.density
        val size = ndsChipSize ?: IntSize((NDS_CHIP_EST_W_DP * d).roundToInt(), (NDS_CHIP_EST_H_DP * d).roundToInt())
        val corner = NdsTouchMode.chipCorner(
            ndsRegions, game.width.toFloat(), game.height.toFloat(), size.width.toFloat(), size.height.toFloat(),
            margin = NDS_CHIP_MARGIN_DP * d + cutoutMax,   // conservative: any cutout could sit beside the chip
        )
        ndsChipCorner.value = corner
        val lp = chip.layoutParams as? FrameLayout.LayoutParams ?: return
        if (lp.gravity != corner.gravity()) {
            lp.gravity = corner.gravity()
            chip.layoutParams = lp
        }
    }

    /** The chip's measured size: placed again (posted, out of the layout pass) when it differs from what was used. */
    private fun onNdsChipSize(size: IntSize) {
        if (size == ndsChipSize) return
        ndsChipSize = size
        findViewById<FrameLayout>(R.id.root).post { placeNdsChip() }
    }

    private fun addPad(root: FrameLayout, view: ComposeView, lp: FrameLayout.LayoutParams) {
        // Added after gameContainer -> overlays it; below the notifications (a rotation re-adds the pads).
        val below = notificationsView?.let { root.indexOfChild(it) }?.takeIf { it >= 0 } ?: -1
        root.addView(view, below, lp)
        padViews += view
    }

    // ------------------------------------------------------------------ frontend (display) settings

    /** The overlay file names offered for this console (the Overlay setting's list). */
    private fun overlayNames(): List<String> = OverlayResolver.names(overlaySets)

    /** Applies frontend [values] (every frontend key) to the view: scaling, sharpness, overlay, offsets, HUD. */
    private fun applyFrontend(values: Map<String, String>) {
        val oldSegaSetting = frontendValues[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO
        frontendValues = values
        val segaSetting = values[PadOptions.SEGA_PAD] ?: SegaPadType.AUTO
        if (segaSetting != oldSegaSetting && PadMap.isSegaPadTag(gameTag)) applySegaPad(segaSetting)
        if (isN3ds) applyN3dsNewControls(values[N3dsOptions.NEW_CONTROLS])
        if (isWii && wiiPad != null) applyWii(values)   // before the launch resolves it, the launch reads the values itself
        // Portrait Pad changed (the menu, the corner icon, or the launch's values after onCreate's layout). Posted: the
        // icon's own view is still dispatching its long press.
        if (portraitPadHidden() != layoutPadHidden) findViewById<FrameLayout>(R.id.root).post { applyLayout(resources.configuration.orientation) }
        val s = FrontendSettings.parse(values)
        padLandscapeAlpha.value = s.padLandscapeAlpha
        val view = retroView ?: return
        view.scaleMode = when (s.scaling) {
            ScreenScaling.NATIVE -> ScaleMode.INTEGER
            ScreenScaling.ASPECT -> ScaleMode.FIT
            ScreenScaling.FULLSCREEN -> ScaleMode.STRETCH
        }
        val shader = shaderFor(s)
        if (view.shader != shader) view.shader = shader
        updateOverlay(s)
        relayoutFrontend()
        setHud(s.debugHud)
    }

    /**
     * A Controller Type change mid-game (menu row, Restore Defaults): the pad is resolved again (Auto from the header
     * read at launch), the table and on-screen pad follow, and the core gets it now on the emulation thread
     * (genesis_plus_gx's port device, picodrive's `picodrive_input1`). The game keeps running.
     */
    private fun applySegaPad(setting: String) {
        if (!menuOpen) releaseAllGameInput()   // game keys held through the old table let go before it changes
        segaPad = SegaPadType.resolve(setting, segaHeader)
        padMap = PadMap.forTag(gameTag, segaPad)
        dropPadProfile()
        // Before the first frame the core may not be loaded (a device set then is overwritten by the launch's ports):
        // the first-frame block sends the setting if it differs from the one the core was launched with.
        if (gameReady) sendSegaPad(setting)
        // Posted: the pad view whose press made the change is still dispatching it, and the menu router arms that
        // press's repeat only after this returns. The rebuilt pad never sends the press's release, so the repeat is
        // stopped here, or it would keep cycling the row.
        findViewById<FrameLayout>(R.id.root).post {
            if (menuOpen) menuRouter.releaseAll()
            applyLayout(resources.configuration.orientation)
        }
    }

    /**
     * A New 3DS Controls change (menu row, Restore Defaults): resolved again (Auto from the title read at launch) and, when
     * that changes what the pad draws, the pad is rebuilt at once. The core needs nothing: ZL, ZR and its right stick are
     * always read.
     */
    private fun applyN3dsNewControls(setting: String?) {
        val resolved = N3dsNewControls.resolve(setting, n3dsTitle)
        if (resolved == n3dsNew) return
        n3dsNew = resolved
        if (!menuOpen) releaseAllGameInput()
        n3dsPadRight = 0f to 0f
        syncN3dsCursor()
        dropPadProfile()
        // Posted, as for the Sega pad (applySegaPad): the press that made the change is still being dispatched.
        findViewById<FrameLayout>(R.id.root).post {
            if (menuOpen) menuRouter.releaseAll()
            applyLayout(resources.configuration.orientation)
        }
    }

    /**
     * A Wii Controller or Motion Controls change (menu row, Restore Defaults, the pad's motion button): resolved again,
     * the pad rebuilt, and the core given the new device on every port now (Dolphin rebuilds a port's mapping live, and
     * re-asks the runner for sensors before it does; cores/patches/dolphin.patch).
     */
    private fun applyWii(values: Map<String, String>) {
        val pad = WiiController.resolve(values[WiiOptions.CONTROLLER], wiiControls)
        val motion = WiiMotion.resolve(values[WiiOptions.MOTION], pad, wiiControls, motionSensors?.hasSensors == true)
        if (pad == wiiPad && motion == wiiMotionOn) return
        if (pad != wiiPad) {
            // Only a pad change lets go of everything: a motion toggle (the pad's MOTION, tapped with one thumb) must
            // not drop the stick the other thumb holds.
            if (!menuOpen) releaseAllGameInput()
            // A stick held through the change would keep the pointer drifting (a Classic Controller never stops it), and
            // a right stick left tilted under Classic would stay stuck as the Nunchuk's tilt, which the right stick does
            // not reach (Dolphin binds it to tilt under IR mode 2): both are zeroed on every port.
            retroView?.let { v ->
                for (p in 0 until maxOf(playerCount, 1)) {
                    v.setPointerVelocity(p, 0f, 0f)
                    v.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, 0f, 0f, p)
                }
            }
        }
        wiiPad = pad
        wiiMotionOn = motion
        LibretroDroid.setSensorEnabled(0, motion)
        dropPadProfile()
        // Before the first frame the core may not be loaded: the first-frame block sends the device then, whatever it is.
        if (gameReady) { sendWiiDevice(); syncMotionSensors() }
        // Posted, as for the Sega pad (applySegaPad): the press that made the change is still being dispatched.
        findViewById<FrameLayout>(R.id.root).post {
            if (menuOpen) menuRouter.releaseAll()
            applyLayout(resources.configuration.orientation)
        }
    }

    /** The Dolphin device for [pad]: MotionPlus added when the game takes it ([WiiController.motionPlus]). */
    private fun wiiDevice(pad: WiiPad): Int = WiiController.device(pad, WiiController.motionPlus(pad, wiiControls))

    /** Dolphin's device for its connected ports: the resolved Wii controller, or the descriptor's plain pad (GameCube). */
    private fun dolphinDevice(): Int =
        wiiPad?.let(::wiiDevice) ?: CoreDescriptor.JOYPAD

    /** The ports a pad holds in [slots] (live or held for it): with port 0, the ones Dolphin has connected ([dolphinPorts]). */
    private fun claimedPorts(): Set<Int> =
        slots?.slots().orEmpty().withIndex().filter { it.value != SlotView.Empty }.map { it.index }.toSet()

    /**
     * Sends the device to port 0 and every claimed port on the emulation thread. Never plugs in an unclaimed port, and a
     * pad that unplugs keeps its port (its slot is held), so the game is not asked to reconnect a remote.
     */
    private fun sendWiiDevice() {
        if (!isDolphin) return
        val view = retroView ?: return
        val ports = dolphinPorts(dolphinDevice(), claimedPorts(), playerCount).filterValues { it != CoreDescriptor.NONE }
        view.queueEvent {
            for ((p, device) in ports) {
                runCatching { view.setControllerType(p, device) }.onFailure { NxLog.w(TAG_LOG, "setting the Dolphin controller failed", it) }
            }
        }
    }

    /** A pad took player slot N on Dolphin: that port is connected now (before this, it was unplugged). */
    private fun connectDolphinPorts() {
        if (isDolphin && gameReady) sendWiiDevice()
    }

    /**
     * The sensors listen only while motion is on and the game runs (not paused, menu closed, in front). [foreground]:
     * onResume and onPause pass it, as the lifecycle state there does not say yet which side of the pause this is.
     */
    private fun syncMotionSensors(foreground: Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        val sensors = motionSensors ?: return
        val n3ds = isN3ds && sensors.hasAccel
        val run = ((isWii && wiiMotionOn) || n3ds) && gameReady && !menuOpen && !quitting && foreground
        when {
            !run -> sensors.stop()
            n3ds -> sensors.startN3ds(::displayRotation)
            else -> sensors.start(::displayRotation, { wiiPad ?: WiiPad.SIDEWAYS })
        }
    }

    /**
     * 3DS motion: the phone's sensors offered to the core. Before the load, as Azahar asks for them once, when its HID
     * service builds the motion device (citra_libretro/input/input_factory.cpp), each sensor on its own: without a gyroscope
     * the tilt still comes through and the gyro reads still; without an accelerometer the console reads as a 3DS at rest.
     */
    private fun offerN3dsMotion() {
        val sensors = motionSensors ?: MotionSensors(this) { a, g ->
            LibretroDroid.setSensorValues(0, a.x, a.y, a.z, g.x, g.y, g.z)
        }.also { motionSensors = it }
        LibretroDroid.setSensorAvailable(sensors.hasAccel, sensors.hasGyro)
        LibretroDroid.setSensorEnabled(0, sensors.hasAccel)
    }

    /** The display's rotation (Surface.ROTATION_*), which [MotionSensors] turns the phone's axes by. */
    @Suppress("DEPRECATION")
    private fun displayRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation ?: 0 else windowManager.defaultDisplay.rotation

    /** The pad's motion button: tap toggles Motion Controls (saved for this game), long press recentres. */
    private fun toggleWiiMotion() {
        if (WiiOptions.MOTION in lockedOptions) return   // locked in the .cfg: left alone, as the hotkeys do (setNdsKey)
        val next = if (wiiMotionOn) WiiMotion.OFF else WiiMotion.ON
        applyFrontend(frontendValues + (WiiOptions.MOTION to next))
        persistGameKey(WiiOptions.MOTION, next, "Could not save Motion Controls")
    }

    /** Takes the phone's attitude at the next reading as the Wiimote's level. */
    private fun recenterWiiMotion() { motionSensors?.recenter() }

    /**
     * Console Settings → Wii Screen for a Wii game: Dolphin's widescreen system setting, chosen from the orientation the
     * game starts in (games read it once at boot), and the touch pointer's geometry for that shape. Empty for any other
     * console.
     */
    private fun wiiScreenOption(): Map<String, String> {
        if (!WiiOptions.appliesTo(gameTag) || wiiGameCube) return emptyMap()
        val portrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val wide = WiiScreen.widescreen(frontendValues[WiiOptions.SCREEN], portrait)
        val pointer = WiiScreen.pointerOptions(
            wide, frontendValues[WiiOptions.POINTER_WIDTH], frontendValues[WiiOptions.POINTER_HEIGHT], frontendValues[WiiOptions.POINTER_OFFSET],
        )
        return mapOf(WiiScreen.CORE_KEY to WiiScreen.coreValue(wide)) + pointer
    }

    /**
     * The Wii's HOME (the pad's, or a controller's right-stick click): saves the resume point, then taps the core's Home
     * (R3). Choosing Wii Menu in the game's HOME menu closes the game ([GLRetroView.getShutdownRequests]) with no state of
     * its own, since the game has stopped itself by then: resuming lands here, just before the HOME menu opened. A press
     * while one is still being saved is dropped.
     */
    private fun pressWiiHome() {
        if (wiiHomeSaving) return
        val view = retroView ?: return
        val ss = saveStates
        if (ss == null || !gameReady || resumePending) { tapWiiHome(view); return }
        wiiHomeSaving = true
        captureThumbnail { bmp ->
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val bytes = view.serializeState()
                    check(bytes.isNotEmpty()) { "core returned no state" }
                    // On [autoWriter], so it lands in order with an on-stop write of the same files.
                    autoWriter.submit {
                        if (bmp != null) ss.writeAutoThumb(bmp)
                        ss.writeAuto(bytes)
                        ss.markAuto(onlyIfNotManual = false)
                        lastResumePoint = NxPaths.AUTO_SLOT
                    }.get()
                    wiiHomeSaved = true
                }.onFailure { NxLog.w("GameActivity", "resume point before Wii HOME failed", it) }
                withContext(Dispatchers.Main) { wiiHomeSaving = false; if (!quitting) tapWiiHome(view) }
            }.also { trackSlotJob(it) }
        }
    }

    private fun tapWiiHome(view: GLRetroView) {
        view.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_THUMBR, 0)
        lifecycleScope.launch { delay(WII_HOME_PRESS_MS); view.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_THUMBR, 0) }
    }

    /** The pad's motion button ([PadMotion]): Wiimote types on a phone with both sensors; none without them. */
    private fun padMotion(): PadMotion? =
        if (isWii && padProfile().motionButton && motionSensors?.hasSensors == true) PadMotion(wiiMotionOn, ::toggleWiiMotion, ::recenterWiiMotion) else null

    /** The pad's small right stick: the New 3DS C-stick, or on Wii the Classic Controller's right stick only. */
    private fun padCStick(): ((Float, Float) -> Unit)? = when {
        gcPad -> ::onWiiRightStick   // the GameCube C-stick is the same right analog on port 0
        !isWii -> ::onCStick
        wiiPad == WiiPad.CLASSIC -> ::onWiiRightStick
        else -> null
    }

    /** Dolphin is running a GameCube game, so the pad is the GameCube's ([PadProfile.Kind.GC]) and L / R are analog triggers. */
    private val gcPad: Boolean get() = isDolphin && padProfile().kind == PadProfile.Kind.GC

    /** The GameCube pad's L and R: one tap/hold gesture, applied state, hold timer each. */
    private val gcGestures = mapOf(NxButton.L2 to TriggerGesture(), NxButton.R2 to TriggerGesture())
    private val gcStates = mutableMapOf(NxButton.L2 to TriggerGesture.State.OFF, NxButton.R2 to TriggerGesture.State.OFF)
    private val gcHoldTimers = mutableMapOf<NxButton, Job>()
    /** The soft-pressed sides, which the pad draws lit. */
    private val gcLatched = mutableStateOf(emptySet<NxButton>())

    /** An on-screen L or R ([btn]) touch: a tap latches a soft press, holding presses it fully while held. */
    private fun gcTrigger(btn: NxButton, down: Boolean) {
        val gesture = gcGestures[btn] ?: return
        val now = SystemClock.uptimeMillis()
        if (down) {
            gcHoldTimers.remove(btn)?.cancel()
            gcApply(btn, gesture.down(now))
            gcHoldTimers[btn] = lifecycleScope.launch {
                delay(gesture.holdMs)
                gcApply(btn, gesture.tick(now + gesture.holdMs))   // the timer is the hold: FULL even if delay resumed a hair early
            }
        } else {
            gcHoldTimers.remove(btn)?.cancel()
            gcApply(btn, gesture.up(now))
        }
    }

    /** Sends [btn]'s trigger at [state]: soft is the analog half only, full also holds the digital key, off lets both go. */
    private fun gcApply(btn: NxButton, state: TriggerGesture.State) {
        val was = gcStates[btn]
        gcStates[btn] = state
        gcLatched.value = gcStates.filterValues { it == TriggerGesture.State.SOFT }.keys
        val view = retroView ?: return
        fun level(b: NxButton) = when (gcStates[b]) { TriggerGesture.State.SOFT -> 0.5f; TriggerGesture.State.FULL -> 1f; else -> 0f }
        view.sendTriggers(0, level(NxButton.L2), level(NxButton.R2))
        val full = state == TriggerGesture.State.FULL
        if (full != (was == TriggerGesture.State.FULL)) {
            PadKeymap.keyCodeFor(btn)?.let { view.sendKeyEvent(if (full) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, it) }
        }
    }

    /** Lets both GameCube triggers go and forgets the latches (the menu opening, a profile change, quitting). */
    private fun gcClearTriggers() {
        gcHoldTimers.values.forEach { it.cancel() }
        gcHoldTimers.clear()
        for ((btn, gesture) in gcGestures) {
            gesture.clear()
            if (gcStates[btn] != TriggerGesture.State.OFF) gcApply(btn, TriggerGesture.State.OFF)
        }
    }

    /** The on-screen Classic Controller's (or GameCube C-) right stick: the core's right analog on port 0. */
    private fun onWiiRightStick(x: Float, y: Float) {
        if (menuOpen) return
        retroView?.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, x, y, 0)
    }

    /**
     * The keys the menu leaves out: the .cfg's locks, Motion Controls on a Wii game without both sensors, and both Wii
     * rows for a GameCube disc under the WII tag.
     */
    private fun menuLocked(): Set<String> = when {
        wiiGameCube -> lockedOptions + WiiOptions.table.map { it.key }
        isWii && motionSensors?.hasSensors != true -> lockedOptions + WiiOptions.MOTION
        else -> lockedOptions
    }

    /** The open menu's Wii rows: Motion Controls, once the sensors are known. */
    private fun refreshWiiMenu(model: InGameMenuModel) {
        model.setFrontend(frontendValues, overlayNames(), menuLocked(), gameTag, overlayAvailable = !ndsSplit)
    }

    /** Sends Controller Type [setting] to the running core on the emulation thread (each player's port device or core option). */
    private fun sendSegaPad(setting: String) {
        if (netplay != null) return   // netplay: the pad type is fixed for the session (startEmulator)
        val view = retroView
        val changes = SegaPadType.liveChanges(gameCoreId, setting, segaHeader, playerCount)
        val ports = changes.filterIsInstance<SegaPadChange.Port>()
        if (ports.isNotEmpty()) view?.queueEvent {
            ports.forEach { c ->
                runCatching { view.setControllerType(c.port, c.device) }
                    .onFailure { NxLog.w(TAG_LOG, "setting the Sega pad failed", it) }
            }
        }
        changes.filterIsInstance<SegaPadChange.Option>().takeIf { it.isNotEmpty() }
            ?.let { opts -> applyOptions(opts.map { Variable(it.key, it.value) }) }
    }

    /** The Default shader with the chosen sharpness; a preset ([applyShader]) replaces it through [GLRetroView.shaderChain]. */
    private fun shaderFor(s: FrontendSettings): ShaderConfig = ShaderConfig.Default(linear = s.linear)

    /**
     * Applies shader [values]: a changed preset is parsed and its LUTs decoded off the main thread, then handed to
     * the view; a changed parameter goes straight to the view. A stale load (a newer preset requested meanwhile)
     * is dropped. Failures toast and leave the stored value as it is (shown raw in the menu).
     */
    private fun applyShader(values: Map<String, String>) {
        val previous = shaderValues
        shaderValues = values
        val view = retroView ?: return
        val preset = ShaderOptions.preset(values)
        // Compared with the last request, not the last completed load: any change (None included) must cancel a
        // pending load, or it would land over the newer choice.
        if (preset != requestedPreset) {
            requestedPreset = preset
            val generation = ++shaderGeneration
            if (preset == null) {
                dropShaderChain()
                return
            }
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val chain = GlslPreset.parse(preset) { Shaders.read(sdRoot, it) }
                        chain.warnings.forEach { NxLog.i(TAG_LOG, "shader $preset: $it") }
                        val luts = chain.luts.mapNotNull { lut -> decodeLut(Shaders.file(sdRoot, lut.path), lut) }
                        chain to luts
                    }
                }
                if (generation != shaderGeneration) return@launch
                val (chain, luts) = result.getOrElse { e ->
                    NxLog.w(TAG_LOG, "shader $preset failed", e)
                    toast("Shader: ${e.message ?: e.javaClass.simpleName}")
                    requestedPreset = null   // this load is the newest request, so forget it for a retry
                    dropShaderChain()
                    return@launch
                }
                loadedPreset = preset
                shaderParams = chain.params
                // Every parameter with its value: the view reads one absent from the map as 0, not its default.
                view.shaderChain = ShaderChainData(
                    passes = chain.passes.map { p ->
                        ShaderPassData(p.source, p.filterLinear, wrapOf(p.wrap), scaleOf(p.scaleTypeX), scaleOf(p.scaleTypeY), p.scaleX, p.scaleY, p.frameCountMod, p.alias)
                    },
                    luts = luts,
                    params = ShaderOptions.paramValues(shaderValues, chain.params),
                )
                menuModel?.let { it.setShaders(shaderValues, presetNames, shaderParams); menuState.value = it.state }
            }
            return
        }
        // Same request, still loading: the load reads shaderValues when it lands, so the new values go with it.
        if (preset != loadedPreset) return
        // Same preset: push only the parameters that changed.
        val current = ShaderOptions.paramValues(values, shaderParams)
        val before = ShaderOptions.paramValues(previous, shaderParams)
        current.forEach { (id, v) -> if (before[id] != v) view.setShaderParameter(id, v) }
    }

    /**
     * Back to the default shader with no parameters, re-feeding the menu; [shaderValues] is kept (the stored
     * preset is shown raw). Also forgets the request, so re-selecting a preset that failed retries it, unless a
     * newer load is pending: that one still lands (the generation is not bumped).
     */
    private fun dropShaderChain() {
        if (requestedPreset == loadedPreset) requestedPreset = null
        loadedPreset = null
        shaderParams = emptyList()
        retroView?.shaderChain = null
        menuModel?.let { it.setShaders(shaderValues, presetNames, shaderParams); menuState.value = it.state }
    }

    private fun wrapOf(w: WrapMode) = when (w) { WrapMode.EDGE -> ShaderWrap.EDGE; WrapMode.REPEAT -> ShaderWrap.REPEAT; WrapMode.MIRRORED -> ShaderWrap.MIRRORED }
    private fun scaleOf(s: ScaleType) = when (s) { ScaleType.NONE -> ShaderScaleType.NONE; ScaleType.SOURCE -> ShaderScaleType.SOURCE; ScaleType.VIEWPORT -> ShaderScaleType.VIEWPORT; ScaleType.ABSOLUTE -> ShaderScaleType.ABSOLUTE }

    /** RGBA8 bytes of a LUT PNG (Bitmap ARGB_8888 is RGBA in memory order); null with a log when unreadable. */
    private fun decodeLut(file: File, lut: ShaderLut): ShaderLutData? {
        var bmp = runCatching { BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inPremultiplied = false }) }.getOrNull()
        if (bmp == null) { NxLog.w(TAG_LOG, "shader texture ${lut.id} unreadable: ${file.path}"); return null }
        // The decoder may ignore inPreferredConfig (16-bit, hardware); the view needs exactly 4 bytes per pixel.
        if (bmp.config != Bitmap.Config.ARGB_8888) {
            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
            bmp.recycle()
            if (copy == null) { NxLog.w(TAG_LOG, "shader texture ${lut.id} could not be converted: ${file.path}"); return null }
            bmp = copy
        }
        if (bmp.byteCount != bmp.width * bmp.height * 4) {
            NxLog.w(TAG_LOG, "shader texture ${lut.id} has an unexpected size: ${file.path}")
            bmp.recycle()
            return null
        }
        val buf = ByteBuffer.allocate(bmp.byteCount)
        bmp.copyPixelsToBuffer(buf)
        val data = ShaderLutData(lut.id, bmp.width, bmp.height, buf.array(), lut.linear, wrapOf(lut.wrap))
        bmp.recycle()
        return data
    }

    /** Resolves the overlay for the current orientation and shows it; an unreadable file counts as None. */
    private fun updateOverlay(s: FrontendSettings) {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        // Split DS screens have no single picture to frame: no overlay (the menu shows the row as unavailable).
        val file = if (ndsSplit) null else OverlayResolver.resolve(gameTag, overlaySets, s.overlay, landscape)
        if (!ndsSplit && s.overlay != null && file == null) NxLog.w(TAG_LOG, "overlay '${s.overlay}' not found in any overlay set; showing none")
        overlayFile = file
        val iv = ensureOverlayView()
        val gen = ++overlayGen
        val cached = overlayBitmap
        when {
            file == null -> { iv.setImageDrawable(null); iv.visibility = View.GONE }
            cached != null && cached.first == file.path -> { iv.setImageBitmap(cached.second); iv.visibility = View.VISIBLE }
            else -> {
                // Hide the old art until the new one is decoded, so it is never stretched into the new box.
                iv.setImageDrawable(null)
                iv.visibility = View.GONE
                lifecycleScope.launch {
                    val bmp = withContext(Dispatchers.IO) { decodeOverlay(file) }
                    if (gen != overlayGen) return@launch
                    if (bmp == null) {
                        overlayFile = null
                        relayoutFrontend()
                    } else {
                        overlayBitmap = file.path to bmp
                        iv.setImageBitmap(bmp)
                        iv.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /** Decodes an overlay PNG; null (logged) when missing or unreadable. */
    private fun decodeOverlay(file: OverlayFile): Bitmap? =
        runCatching { BitmapFactory.decodeFile(File(sdRoot, file.path).absolutePath) }.getOrNull()
            .also { if (it == null) NxLog.w(TAG_LOG, "overlay not readable: ${file.path}") }

    /** The overlay image: inside the game container above the GLRetroView, so pads and the menu stay above it. */
    private fun ensureOverlayView(): ImageView = overlayView ?: ImageView(this).also { iv ->
        iv.scaleType = ImageView.ScaleType.FIT_XY
        iv.visibility = View.GONE
        findViewById<FrameLayout>(R.id.gameContainer).addView(iv, FrameLayout.LayoutParams(0, 0, Gravity.TOP or Gravity.START))
        overlayView = iv
    }

    /**
     * Lays the picture out in the game area. DS games (a plain 256x384 frame) get their screens as regions of the view
     * ([NdsLayout]), with the viewport the whole area and no offsets or overlay: the fork ignores the screen offset and
     * the scaling mode for regions, so the Screen Offset and Scaling settings have no effect while the screens are split.
     * Other games: the overlay box (or the whole area) becomes the GL viewport and the overlay view's bounds; the offsets
     * (scaled to the box) go to the view. No-op until laid out.
     */
    private fun relayoutFrontend() {
        val view = retroView ?: return
        // Portrait with the band hidden: no touch reaches the game under the clusters (the 3DS core takes the touches whole;
        // the DS screen under them is also off in its regions). The game area starts at the cutout.
        view.touchCutoffY = portraitClusterTop?.let { (it - cutoutTop).toFloat() } ?: Float.POSITIVE_INFINITY
        if (!view.isAttachedToWindow) return   // the GL thread is gone once detached
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        val w = game.width.toFloat()
        val h = game.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val g = view.getGameGeometry()
        val nds = ndsLayout(w, h, g)
        // An area too small for one DS screen pixel (NdsLayout returns no regions) is only ever a transient mid-resize
        // size. Keep whatever is drawn now rather than letting an empty list flip a split game to the whole 256x384
        // frame (and the overlay back on) for that moment; the next size change lays it out again.
        if (nds != null && nds.isEmpty()) return
        // Before its first frame a DS game's frame size is unknown (0x0): it stays split as it started, overlay off.
        if (nds == null && ndsSplit && !gameReady) return
        ndsRegions = nds ?: emptyList()
        positionPen()
        placeNdsChip()
        setNdsSplit(nds != null)
        if (nds != null) {
            val whole = RectF(0f, 0f, 1f, 1f)
            if (lastViewport != whole) {
                lastViewport = whole
                view.viewport = whole
            }
            view.screenOffset = PointF(0f, 0f)
            view.screenRegions = nds.map { it.toScreenRegion(w, h) }
            return
        }
        if (view.screenRegions.isNotEmpty()) view.screenRegions = emptyList()
        val s = FrontendSettings.parse(frontendValues)
        val layout = frontendLayout(w, h, g[0].toInt(), g[1].toInt(), g[2], s.scaling, s.offsetX, s.offsetY, overlayFile?.aspect)
        val box = layout.box
        val viewport = RectF(box.x / w, box.y / h, (box.x + box.width) / w, (box.y + box.height) / h)
        if (viewport != lastViewport) {
            lastViewport = viewport
            view.viewport = viewport
        }
        view.screenOffset = PointF(layout.offsetX, layout.offsetY)
        overlayView?.layoutParams = FrameLayout.LayoutParams(box.width.roundToInt(), box.height.roundToInt()).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = box.x.roundToInt()
            topMargin = box.y.roundToInt()
        }
    }

    /**
     * The DS screen regions for a [w] x [h] game area, or null: not NDS, or the frame is not the plain DS stack (256x384,
     * or 256N x 384N from the GL renderer).
     * Empty when the area is too small for a screen pixel (the caller keeps the current layout then).
     */
    private fun ndsLayout(w: Float, h: Float, g: FloatArray): List<NdsRegion>? {
        if (!NdsOptions.appliesTo(gameTag)) return null
        val frameW = g[0].roundToInt()
        val frameH = g[1].roundToInt()
        if (!NdsLayout.isDsFrame(frameW, frameH)) {
            // Safety net (0x0 before the first frame; anything else is a core or option change): one picture, as for any core.
            if (frameW > 0 && frameH > 0 && !ndsFrameWarned) {
                ndsFrameWarned = true
                NxLog.w(TAG_LOG, "NDS frame is ${frameW}x$frameH, not a 256x384 stack: drawing it as one picture")
            }
            return null
        }
        val landscape = isLandscape()
        return NdsLayout.regions(
            // Touch mode shows the bottom screen big for the fingers: drawn only, never saved (nx_nds_big_screen stands).
            w, h, landscape, NdsTouchMode.settings(NdsSettings.parse(frontendValues), ndsTouchMode), NDS_INSET_MARGIN_DP * resources.displayMetrics.density,
            controllerMode = ndsControllerMode(),
            // The same condition applyLayout shows the landscape pad clusters on (no controller, not in touch mode).
            padOverGame = landscape && layoutControllerNames().isEmpty() && !ndsTouchMode,
            // Portrait with the band hidden: the screen under the clusters takes no touches (the game area starts at the cutout).
            padCoverTop = portraitClusterTop?.let { (it - cutoutTop).toFloat() } ?: Float.POSITIVE_INFINITY,
        )
    }

    /**
     * Portrait with the pad band dropped for a non-clip-on controller ([PadBandRule]): the DS layout and SELECT + Left/Right
     * use the controller portrait layout then. [applyLayout] relays out on connect/disconnect, so this follows live.
     */
    private fun ndsControllerMode(): Boolean {
        val tag = intent.getStringExtra(EXTRA_TAG) ?: gameTag   // set before startEmulator's gameTag, for onCreate's layout
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return PadBandRule.controllerLayout(tag, portrait, layoutControllerNames())
    }

    /** Split DS screens turn the overlay off and the menu shows it unavailable; both follow when that changes. */
    private fun setNdsSplit(split: Boolean) {
        if (split == ndsSplit) return
        ndsSplit = split
        updateOverlay(FrontendSettings.parse(frontendValues))
        menuModel?.let {
            it.setFrontend(frontendValues, overlayNames(), menuLocked(), gameTag, overlayAvailable = !split)
            menuState.value = it.state
        }
    }

    /** NDS hotkeys and stylus for one button event; true when it goes on to the core. A key repeat only asks whether its press did. */
    private fun routeNds(button: NxButton, down: Boolean, repeat: Boolean = false): Boolean {
        if (repeat) return !ndsHotkeys.consumes(button)
        val decision = ndsHotkeys.onButton(button, down)
        decision.action?.let(::applyNdsAction)
        if (!decision.forward) syncPen()
        return decision.forward
    }

    private fun applyNdsAction(action: NdsAction) {
        val landscape = isLandscape()
        when (action) {
            // Ignored in touch mode: the bottom screen is shown big there whatever the setting, so a swap would change nothing
            // visible, only the saved big screen (and touch mode ends on any controller connect, the only other R2 source).
            NdsAction.SwapScreens -> if (!ndsTouchMode) setNdsKey(NdsOptions.swapBigScreen(frontendValues))
            is NdsAction.CycleLayout -> {
                // From the layout showing: Auto resolves for the game area as ndsLayout lays it out.
                val game = findViewById<FrameLayout>(R.id.gameContainer)
                setNdsKey(NdsOptions.cycleLayout(
                    frontendValues, landscape, ndsControllerMode(), action.delta, game.width.toFloat(), game.height.toFloat(),
                ))
            }
            is NdsAction.CycleCorner -> NdsOptions.cycleCorner(frontendValues, action.delta).let { (key, value) ->
                setNdsKey(key to value, "PiP: " + NdsOptions.valueLabel(key, value))
            }
            is NdsAction.Stylus -> {
                if (action.on) releaseStylusInputs()
                updateTouchModeAvailable()   // stylus off with the landscape pad shown: the next L2 enters touch mode
                showNdsLabel(if (action.on) "Stylus on" else "Stylus off")
            }
            NdsAction.TouchMode -> {
                ndsTouchMode = true
                releaseAllGameInput()   // the pad goes away under the finger: nothing it held may stick in the core
                // Posted: L2's own pad view is still dispatching this touch.
                findViewById<FrameLayout>(R.id.root).post { applyLayout(resources.configuration.orientation) }
                showNdsLabel("Touch mode — tap Controls to return")
            }
        }
    }

    /** L2 asks for touch mode exactly while the landscape clusters are shown on a touchscreen and the stylus is off. */
    private fun updateTouchModeAvailable(names: List<String> = layoutControllerNames(), landscape: Boolean = isLandscape()) {
        ndsHotkeys.touchModeAvailable = NdsTouchMode.available(
            hasTouchscreen, landscape, controllers = names.isNotEmpty(), touchMode = ndsTouchMode, stylus = ndsHotkeys.stylus,
        )
    }

    /** The "Controls" chip: the landscape pad back, bottom-screen touch off again. */
    private fun leaveNdsTouchMode() {
        if (!ndsTouchMode) return
        ndsTouchMode = false
        findViewById<FrameLayout>(R.id.root).post { applyLayout(resources.configuration.orientation) }
    }

    /**
     * A hotkey's layout change: applied now, shown as [label] (the new value's by default), and saved to the game's config.
     * A key locked in the .cfg is left alone.
     */
    private fun setNdsKey(change: Pair<String, String>, label: String = NdsOptions.valueLabel(change.first, change.second)) {
        val (key, value) = change
        if (key in lockedOptions) return
        applyFrontend(frontendValues + change)
        showNdsLabel(label)
        persistGameKey(key, value)
    }

    private fun showNdsLabel(text: String) {
        notifications.push(text, null, NDS_LABEL_MS, SystemClock.elapsedRealtime())
        publishNotifications()
    }

    private fun persistGameKey(key: String, value: String, failure: String = "Could not save the screen layout") {
        val store = configStore ?: return
        lifecycleScope.launch {
            val written = configWrites.withLock {
                withContext(Dispatchers.IO) {
                    runCatching { store.saveGameKey(key, value) }
                        .onFailure { NxLog.w("GameActivity", "saving $key failed", it) }.getOrNull()
                }
            }
            if (written == null) { toast(failure); return@launch }
            configSource = ConfigSource.GAME
            lockedOptions = written.locked + forcedOptions
        }
    }

    /** The d-pad, left stick and A now drive the pen: the core must not keep what they held. */
    private fun releaseStylusInputs() {
        val view = retroView ?: return
        releaseDirections(view)
        PadKeymap.keyCodeFor(NxButton.A)?.let { view.sendKeyEvent(KeyEvent.ACTION_UP, it) }
    }

    /** Sends the pen when it changed (pressed, or just lifted), moves the cursor, and keeps the pen ticking in stylus mode. */
    private fun syncPen() {
        val view = retroView ?: return
        val pen = ndsHotkeys.pen.takeIf { ndsHotkeys.stylus }
        if (pen != sentPen) {
            when {
                pen != null && pen.pressed -> view.sendPointer(pen.frameX, pen.frameY, true)
                sentPen?.pressed == true -> view.sendPointer(0f, 0f, false)
            }
            sentPen = pen
        }
        positionPen()
        if (!ndsHotkeys.stylus) {
            penJob?.cancel()
            penJob = null
            return
        }
        if (penJob?.isActive == true) return
        penJob = lifecycleScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (true) {
                delay(PEN_TICK_MS)
                val now = SystemClock.elapsedRealtime()
                if (!menuOpen && ndsHotkeys.tick(now - last)) syncPen()
                last = now
            }
        }
    }

    /** The stylus cursor over the bottom screen wherever it is shown (the inset included); hidden without stylus mode. */
    private fun positionPen() {
        val region = ndsRegions.firstOrNull { it.screen == NdsScreen.BOTTOM }
        if (!ndsHotkeys.stylus || region == null) { penView?.visibility = View.GONE; return }
        val v = ensurePenView()
        val size = (PEN_DP * resources.displayMetrics.density).roundToInt()
        val pen = ndsHotkeys.pen
        val (cx, cy) = NdsLayout.pointIn(region, pen.x, pen.y)
        v.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START).apply {
            leftMargin = (cx - size / 2f).roundToInt()
            topMargin = (cy - size / 2f).roundToInt()
        }
        v.alpha = if (pen.pressed) 1f else 0.6f
        v.visibility = View.VISIBLE
    }

    /** A plain, non-clickable view above the GL surface in the game container: touches pass through to the game. */
    private fun ensurePenView(): View = penView ?: View(this).also { v ->
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(NxDefaults.colors.main.toArgb())   // the UI ink
            setStroke((2 * resources.displayMetrics.density).roundToInt(), 0xFF000000.toInt())
        }
        v.isClickable = false
        v.isFocusable = false
        v.visibility = View.GONE
        findViewById<FrameLayout>(R.id.gameContainer).addView(v, FrameLayout.LayoutParams(0, 0, Gravity.TOP or Gravity.START))
        penView = v
    }

    /**
     * Hardware hat (or, outside stylus mode, left-stick) edges as direction presses through the hotkeys; the forwarded
     * ones reach the core as its d-pad. This replaces the d-pad keys Android would synthesise from the event.
     */
    private fun routeNdsHat(view: GLRetroView, event: MotionEvent) {
        val x = ndsHotkeys.joystickDirection(event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_X))
        val y = ndsHotkeys.joystickDirection(event.getAxisValue(MotionEvent.AXIS_HAT_Y), event.getAxisValue(MotionEvent.AXIS_Y))
        var changed = false
        fun edge(old: Int, new: Int, negative: NxButton, positive: NxButton) {
            if (old == new) return
            val released = when (old) { -1 -> negative; 1 -> positive; else -> null }
            val pressed = when (new) { -1 -> negative; 1 -> positive; else -> null }
            if (released != null && routeNds(released, down = false)) changed = ndsHat.release(released) || changed
            if (pressed != null && routeNds(pressed, down = true)) changed = ndsHat.press(pressed) || changed
        }
        edge(ndsHatX, x, NxButton.LEFT, NxButton.RIGHT)
        edge(ndsHatY, y, NxButton.UP, NxButton.DOWN)
        ndsHatX = x
        ndsHatY = y
        if (changed) view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, ndsHat.x.toFloat(), ndsHat.y.toFloat(), 0)
    }

    // ------------------------------------------------------------------ Nintendo 3DS controls

    /** Picks the 3DS hotkeys' core values out of [values] (launch, a menu change, a restore); other keys are ignored. */
    private fun n3dsCoreValues(values: Map<String, String>) {
        values[N3dsOptions.LAYOUT]?.let { n3dsLayout = it.ifEmpty { null } }
        values[N3dsOptions.DEADZONE]?.let { n3dsHotkeys.deadzone = N3dsOptions.deadzone(it) }
    }

    /**
     * 3DS hotkeys and stylus for one button event; true when it goes on to the core as is. What it becomes instead (L3, R3, a
     * held-back SELECT) is sent here. A key repeat only asks whether its press did.
     */
    private fun routeN3ds(button: NxButton, down: Boolean, repeat: Boolean = false): Boolean {
        if (repeat) return !n3dsHotkeys.consumes(button)
        val decision = n3dsHotkeys.onButton(button, down)
        decision.action?.let(::applyN3dsAction)
        syncN3dsCursor()
        return decision.forward
    }

    /** The keys the 3DS hotkeys produced: L3 (Azahar's swap screens), R3 (its touch at the cursor), the game's SELECT. */
    private fun sendN3dsKeys() {
        val view = retroView
        for (e in n3dsHotkeys.takeKeys()) {
            val code = when (e.key) {
                N3dsKey.L3 -> KeyEvent.KEYCODE_BUTTON_THUMBL
                N3dsKey.R3 -> KeyEvent.KEYCODE_BUTTON_THUMBR
                N3dsKey.SELECT -> KeyEvent.KEYCODE_BUTTON_SELECT
            }
            view?.sendKeyEvent(if (e.down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
        }
    }

    private fun applyN3dsAction(action: N3dsAction) {
        when (action) {
            is N3dsAction.CycleLayout -> {
                val (key, value) = N3dsOptions.cycleLayout(n3dsLayout, action.delta)
                if (key in lockedOptions) return
                n3dsLayout = value
                applyOptions(listOf(Variable(key, value)))   // Azahar re-lays its screens out on its next frame
                showNdsLabel(N3dsOptions.layoutLabel(value))
                persistGameKey(key, value)
            }
            is N3dsAction.Stylus -> {
                // On: the d-pad, left stick and A now drive the cursor, so the core must not keep what they held. Off: the
                // hotkeys lift a touch held, and syncN3dsCursor gives the right stick back.
                if (action.on) releaseStylusInputs()
                showNdsLabel(if (action.on) "Stylus on" else "Stylus off")
            }
        }
    }

    /**
     * Sends the hotkeys' keys and the right stick when it changed: stylus mode's cursor ([N3dsHotkeys.cursor]), else the
     * hardware stick. Keeps ticking the hotkeys while they ask (a SELECT held toward its 1 s cancel or tapping, a held d-pad
     * cursor speeding up).
     */
    private fun syncN3dsCursor() {
        sendN3dsKeys()
        val view = retroView ?: return
        // The on-screen C-stick while it is off centre, else the hardware stick.
        val right = n3dsHotkeys.cursor ?: n3dsPadRight.takeIf { it != (0f to 0f) } ?: n3dsHwRight
        if (right != n3dsSentRight) {
            view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, right.first, right.second, 0)
            n3dsSentRight = right
        }
        if (!n3dsHotkeys.ticking || n3dsTickJob?.isActive == true) return
        n3dsTickJob = lifecycleScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (n3dsHotkeys.ticking) {
                delay(PEN_TICK_MS)
                val now = SystemClock.elapsedRealtime()
                if (!menuOpen) n3dsHotkeys.tick(now - last)
                last = now
                syncN3dsCursor()
            }
            n3dsTickJob = null
        }
    }

    /**
     * The 3DS title from the start of [s] ([N3dsTitle.parse]): the first header, and for a cart image (NCSD) on to its
     * partition 0's NCCH header (0x4000 on dumped carts), a few KB in all.
     */
    private fun readN3dsTitle(s: java.io.InputStream): N3dsTitle? {
        fun readFully(buf: ByteArray, from: Int): Int {
            var n = from
            while (n < buf.size) { val r = s.read(buf, n, buf.size - n); if (r < 0) break; n += r }
            return n
        }
        val head = ByteArray(N3dsTitle.HEADER)
        if (readFully(head, 0) < head.size) return null
        val offset = N3dsTitle.ncchOffset(head) ?: return N3dsTitle.parse(head)
        val all = head.copyOf(offset.toInt() + N3dsTitle.HEADER)
        return N3dsTitle.parse(all.copyOf(readFully(all, head.size)))
    }

    /** Hardware hat edges as direction presses through the 3DS hotkeys; the forwarded ones reach the core as its d-pad. */
    private fun routeN3dsHat(view: GLRetroView, event: MotionEvent) {
        val x = n3dsHotkeys.joystickDirection(event.getAxisValue(MotionEvent.AXIS_HAT_X))
        val y = n3dsHotkeys.joystickDirection(event.getAxisValue(MotionEvent.AXIS_HAT_Y))
        var changed = false
        fun edge(old: Int, new: Int, negative: NxButton, positive: NxButton) {
            if (old == new) return
            val released = when (old) { -1 -> negative; 1 -> positive; else -> null }
            val pressed = when (new) { -1 -> negative; 1 -> positive; else -> null }
            if (released != null && routeN3ds(released, down = false)) changed = ndsHat.release(released) || changed
            if (pressed != null && routeN3ds(pressed, down = true)) changed = ndsHat.press(pressed) || changed
        }
        edge(n3dsHatX, x, NxButton.LEFT, NxButton.RIGHT)
        edge(n3dsHatY, y, NxButton.UP, NxButton.DOWN)
        n3dsHatX = x
        n3dsHatY = y
        if (changed) view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, ndsHat.x.toFloat(), ndsHat.y.toFloat(), 0)
    }

    /**
     * L2/R2 pulled as trigger axes (AXIS_LTRIGGER/AXIS_BRAKE, AXIS_RTRIGGER/AXIS_GAS): each pull, with hysteresis, is one
     * press and one release ([TriggerAxes]), to the DS hotkeys, the 3DS hotkeys (ZL/ZR, always forwarded) or the core as
     * the L2/R2 key. A trigger that has also come as a key event goes by its keys alone ([routeTriggerKey]). Not for the
     * Dreamcast, whose triggers are analog.
     */
    private fun routeTriggerAxes(event: MotionEvent, port: Int = 0) {
        val l = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val r = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
        for ((button, down) in inputs[port].triggers.onAxes(l, r)) {
            when {
                isNds -> routeNds(button, down)
                isN3ds -> if (routeN3ds(button, down)) sendTriggerKey(button, down)
                else -> sendTriggerKey(button, down, port)
            }
        }
    }

    /**
     * L2/R2 as a key event: true when it goes on (the DS: to its hotkeys; others: on to the core as any key). From the
     * first one on, that trigger's axes are ignored, so a controller that sends both a key and an axis for one pull
     * toggles once; a pull already pressed by its axis is not pressed again.
     */
    private fun routeTriggerKey(button: NxButton, down: Boolean, port: Int = 0): Boolean = inputs[port].triggers.onKey(button, down)

    /**
     * A GameCube game drops the L2/R2 key of a pad that also reports that trigger as an axis: Dolphin's full press is
     * `Trigger0+ | (L2 & !Trigger0+)`, so a key sent on a light pull clicked fully at once. The analog alone drives it
     * (onGenericMotionEvent) and Dolphin's own threshold makes the click. A pad without trigger axes keeps its key.
     */
    private fun gcAnalogTriggerKey(button: NxButton, device: InputDevice?): Boolean {
        if (!gcPad || device == null) return false
        val axes = when (button) {
            NxButton.L2 -> intArrayOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
            NxButton.R2 -> intArrayOf(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
            else -> return false
        }
        return axes.any { device.getMotionRange(it) != null }
    }

    /** An axis trigger's press or release to the core, as its L2/R2 key would be sent (by name on N64, else the pad map). */
    private fun sendTriggerKey(button: NxButton, down: Boolean, port: Int = 0) {
        val keyCode = PadKeymap.keyCodeFor(button) ?: return
        val code = if (isN64) N64Input.toRunner(button, keyCode) else padMap.sendKey(keyCode)
        code?.let { retroView?.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, it, port) }
    }

    /** The Debug HUD (top-left): fps, core, core resolution and the scaled picture size, once a second. */
    private fun setHud(on: Boolean) {
        val label = findViewById<TextView>(R.id.fps)
        if (!on) {
            hudJob?.cancel()
            hudJob = null
            label.visibility = View.GONE
            return
        }
        if (hudJob != null) return
        val view = retroView ?: return
        label.text = ""
        label.visibility = View.VISIBLE
        var frames = 0
        var windowStart = SystemClock.elapsedRealtime()
        var pausedShown = false
        var pausedAt = 0L
        hudJob = lifecycleScope.launch {
            view.getGLRetroEvents().flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect { event ->
                if (event !is GLRetroView.GLRetroEvents.FrameRendered) return@collect
                val now = SystemClock.elapsedRealtime()
                if (menuOpen) {
                    // Paused: these frames only redraw the last picture, so don't count them as the game's rate.
                    // Still refresh the sizes about once a second: the adjust strip changes them.
                    if (!pausedShown || now - pausedAt >= 1000) {
                        label.text = hudText(view, null)
                        pausedShown = true
                        pausedAt = now
                    }
                    frames = 0
                    windowStart = now
                    return@collect
                }
                pausedShown = false
                frames++
                if (now - windowStart < 1000) return@collect
                val fps = frames * 1000f / (now - windowStart)
                label.text = hudText(view, fps)
                NxLog.i(TAG_LOG, String.format(java.util.Locale.US, "fps=%.1f core=%s", fps, gameCoreId))
                frames = 0
                windowStart = now
            }
        }
    }

    /** [fps] null = paused (the menu is open). */
    private fun hudText(view: GLRetroView, fps: Float?): String {
        val g = view.getGameGeometry()
        val coreW = g[0].toInt()
        val coreH = g[1].toInt()
        val game = findViewById<FrameLayout>(R.id.gameContainer)
        // Split DS screens: each screen's size as drawn (the big one first), not the single picture's.
        val shown = if (ndsRegions.isNotEmpty()) {
            ndsRegions.sortedBy { it.inset }.joinToString(" + ") { "${it.screen.value} ${it.dst.width.roundToInt()}x${it.dst.height.roundToInt()}" }
        } else {
            val s = FrontendSettings.parse(frontendValues)
            val out = frontendLayout(game.width.toFloat(), game.height.toFloat(), coreW, coreH, g[2], s.scaling, s.offsetX, s.offsetY, overlayFile?.aspect).content
            "${out.width.roundToInt()}x${out.height.roundToInt()}"
        }
        val rate = if (fps == null) "paused" else String.format(java.util.Locale.US, "%.1f fps", fps)
        return String.format(java.util.Locale.US, "%s  %s  %dx%d → %s", rate, gameCoreId, coreW, coreH, shown)
    }

    // ------------------------------------------------------------------ in-game pause menu

    /** Back (the key's press, the gesture): the open in-game menu reads it as B, else it opens. */
    private fun backPressed() { if (menuOpen) onMenuPress(NxButton.B) else toggleMenu() }

    private fun toggleMenu() { if (quitting) return; if (menuOpen) closeMenu() else openMenu() }

    private fun openMenu() {
        if (menuOpen) return
        menuOpen = true
        val model = InGameMenuModel(
            gameTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Paused",
            slotCount = SaveStates.SLOT_COUNT,
            // The cached pointer. Only a menu opened before startEmulator's first read lands reads it here: a
            // one-line file, and the model takes its slot only at construction.
            initialSlot = (lastSlotCache ?: saveStates?.lastSlot()?.also { lastSlotCache = it } ?: 1) - 1,
        ).also {
            it.setStatus(configSource); it.cheatsAvailable = cheatsSupported
            // netplay: the root is "Leave netplay?" only; Continue / Quit once the session has ended
            if (netplay != null) { it.netplay = true; it.netplayEnded = netplayEnded; it.refresh() }
        }
        if (retroView != null) {
            // Re-listed on every open so a bundled tree swapped in since launch is picked up; on IO, the
            // menu showing the cached lists until then (unchanged, as they almost always are, nothing redraws).
            val tag = gameTag
            menuListingJob?.cancel()
            menuListingJob = lifecycleScope.launch {
                val (found, presets) = withContext(Dispatchers.IO) {
                    runCatching { Overlays.sets(sdRoot, tag) }.getOrNull() to runCatching { Shaders.presets(sdRoot) }.getOrNull()
                }
                if (found != null && found != overlaySets) {
                    // The shown overlay may have been added/removed on disk: re-resolve it against the new list.
                    overlaySets = found   // the Overlay value is unchanged (its default is None whatever the files)
                    applyFrontend(frontendValues)
                    menuModel?.takeIf { it === model }?.let {
                        it.setFrontend(frontendValues, overlayNames(), menuLocked(), gameTag, overlayAvailable = !ndsSplit)
                        menuState.value = it.state
                    }
                }
                if (presets != null && presets != presetNames) {
                    presetNames = presets   // same for the bundled presets
                    menuModel?.takeIf { it === model }?.let { it.setShaders(shaderValues, presetNames, shaderParams); menuState.value = it.state }
                }
            }
        }
        // Unconditional: the menu can open while the game is still loading, and must still show the loaded config.
        model.setFrontend(frontendValues, overlayNames(), menuLocked(), gameTag, overlayAvailable = !ndsSplit)
        model.setShaders(shaderValues, presetNames, shaderParams)
        model.setCheats(cheatSet.entries, cheatsFolder, cheatSet.expected)
        publishPlayers(model)
        applyAchievements(model)
        menuModel = model
        // Multi-disc content (an m3u, a multi-disc PBP): the core reports its discs through the disk-control interface.
        retroView?.takeIf { gameReady && netplay == null }?.let { view ->
            lifecycleScope.launch {
                val (count, current) = withContext(Dispatchers.IO) {
                    runCatching { view.getAvailableDisks() to view.getCurrentDisk() }
                        .onFailure { NxLog.w("GameActivity", "disc query failed", it) }.getOrNull()
                } ?: return@launch
                menuModel?.takeIf { it === model }?.let { it.setDiscs(current, count); menuState.value = it.state }
            }
        }
        menuState.value = model.state
        menuAxes.reset()
        stickRouter.onMenuToggled()
        refreshSlots()
        releaseAllGameInput()
        syncMotionSensors()   // no motion while paused
        // netplay: native runs a session at speed 1 whatever frameSpeed says; a local pause stops this device's input instead.
        netplayGame?.takeIf { it.live }?.pause() ?: run { retroView?.frameSpeed = 0 }
        retroView?.audioEnabled = false
        rumbles.forEach { it.pause() }   // the core stops with its rumble latched: still while paused, back on closeMenu
        lifecycleScope.launch(Dispatchers.IO) { writeBatterySave(notify = true) }
        val root = findViewById<FrameLayout>(R.id.root)
        // The accents cannot change while a game runs; read on every open all the same, so a change made since launch shows the next time it opens.
        // The in-game menu is on the accent and the Secondary accent, as the Settings pages.
        val colors = AndroidAppSettings(this).let { nxColorsFor(it.accent, it.secondaryAccent) }
        val overlay = ComposeView(this).apply {
            setContent {
                val config = LocalConfiguration.current
                NxTheme(colors = colors) {
                    // No controller: hints show the on-screen pad's own labels (Sega C, PS ○); one attached: the A/B art.
                    // Keyed on the profile's revision: a live Controller Type change redraws them with the menu open.
                    val connected = controllerConnected.value
                    val hintGlyphs = remember(connected, padProfileRevision.value) {
                        if (connected) HintGlyphs.STANDARD else HintGlyphs.forPad(padProfile())
                    }
                    // The page title's gutter follows the app's: the narrow one while the pad sits below (portrait, or a
                    // tabletop pad under the hinge), as HandheldShell sets it; a controller-dropped band keeps it, with no pad below.
                    val portraitLayout = tabletop()?.let { it.pad != TabletopPad.NONE } ?: (config.orientation == Configuration.ORIENTATION_PORTRAIT)
                    val padBelow = portraitLayout && (tabletop() != null || portraitBandPx() > 0)
                    // The window decides the two panes, as Settings' ([inGameMenuSplit]): portrait never splits.
                    val window = DpSize(config.screenWidthDp.dp, config.screenHeightDp.dp)
                    CompositionLocalProvider(
                        LocalHintGlyphs provides hintGlyphs, LocalPortrait provides portraitLayout, LocalPadBelow provides padBelow,
                        LocalWindowSize provides window,
                        LocalNxOptionText provides AndroidAppSettings(this@GameActivity).optionTextSize.ofListLabel,   // Appearance → Text size
                    ) {
                    menuState.value?.let { s ->
                        InGameMenu(
                            state = s,
                            slots = slotsUi.value,
                            onSelect = { i -> if (!quitting) menuModel?.let { it.select(i); menuState.value = it.state } },
                            onActivate = { i -> if (!quitting) menuModel?.let { m -> val effect = m.activate(i); menuState.value = m.state; handle(effect) } },
                            onDismiss = { if (!quitting) menuModel?.let { val e = it.press(NxButton.B); menuState.value = it.state; handle(e) } },
                            onChange = { d -> if (!quitting) menuModel?.let { val e = it.press(if (d > 0) NxButton.RIGHT else NxButton.LEFT); menuState.value = it.state; handle(e) } },
                            onPress = ::onMenuPress,
                            achievementDetail = menuModel?.achievementDetail(),
                            badgePath = { a -> menuModel?.badgePath(a) },
                            muted = { id -> id in (achievements?.muted ?: emptySet()) },
                            onInfoTruncated = { info, t -> if (!quitting) menuModel?.let { it.setInfoTruncated(info, t); menuState.value = it.state } },
                            // Two panes on a wide area (the unfolded Fold, a phone in landscape), as Settings.
                            preview = { row -> menuModel?.previewFor(row) },
                            onSplit = { split -> menuModel?.split = split },
                            onParentTap = { kind, i -> if (!quitting) menuModel?.let { m -> val e = m.tapParent(kind, i); menuState.value = m.state; handle(e) } },
                            onPreviewTap = { i -> if (!quitting) menuModel?.let { m -> val e = m.tapPreview(i); menuState.value = m.state; handle(e) } },
                        )
                    }
                    }
                }
            }
        }
        menuView = overlay
        root.addView(overlay, menuLayoutParams(resources.configuration.orientation))
        updatePadVisibility()
    }

    /**
     * Portrait: confine the modal to the top game area so the bottom controls stay visible and can drive
     * the menu (the whole height when the band is dropped). Landscape: cover the whole screen. Re-applied by
     * [applyLayout] (rotation, controller change) while the menu is open.
     */
    private fun menuLayoutParams(orientation: Int): FrameLayout.LayoutParams {
        // Tabletop: confined above the hinge, the controls below still drive it.
        tabletop()?.let { tt -> return FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, tt.gameBottom, Gravity.TOP) }
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            val dm = resources.displayMetrics
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dm.heightPixels - portraitBandPx(), Gravity.TOP)
        } else {
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
    }

    private fun closeMenu() {
        if (!menuOpen) return
        menuOpen = false
        menuRouter.releaseAll()
        stickRouter.onMenuToggled()
        menuView?.let { findViewById<FrameLayout>(R.id.root).removeView(it) }
        menuView = null
        menuModel = null
        menuState.value = null
        updatePadVisibility()
        netplayGame?.takeIf { it.live }?.resume()   // netplay: input goes out again (see openMenu)
        if (!netplayStartFailed) {   // netplay: a failed start keeps the game paused behind its dialog
            retroView?.frameSpeed = 1
            retroView?.audioEnabled = true
        }
        if (!quitting) rumbles.forEach { it.resume() }
        syncMotionSensors()
    }

    /** Menu navigation from the InputRouter (hardware/on-screen). */
    private fun onMenuPress(button: NxButton) {
        if (quitting) return   // frozen until finish()
        val model = menuModel ?: return
        val effect = model.press(button)
        menuState.value = model.state   // publish before handling: Close/Quit clear the menu state
        handle(effect)
    }

    private fun handle(effect: MenuEffect?) {
        if (quitting) return   // a quit is in flight: nothing may resume, save or re-quit before finish()
        when (effect) {
            null -> Unit
            MenuEffect.Close -> closeMenu()
            MenuEffect.Quit -> quitWithAutoState(chosen = true)
            // Quit's "Quit": the battery save as ever, the resume point on disk left as it is.
            MenuEffect.QuitWithoutResume -> quitWithAutoState(chosen = true, autoState = false)
            // netplay: DISCONNECT and shut the engine down (off main), then quit; saveStates is null: no auto state.
            MenuEffect.LeaveNetplay -> { netplayGame?.leave(); quitWithAutoState(chosen = true) }
            is MenuEffect.SaveSlot -> saveToSlot(effect.slot)
            is MenuEffect.LoadSlot -> loadFromSlot(effect.slot)
            is MenuEffect.ToggleMute -> toggleMute(effect.id)
            MenuEffect.OpenCoreOptions -> {
                // Always answer the request, even without a view: the model blocks further opens while one is pending.
                if (retroView == null) {
                    menuModel?.let { it.showCoreOptions(emptyList(), lockedOptions + graphicsForcedOptions + restartOnlyOptions); menuState.value = it.state }
                } else {
                    withCoreOptions { all -> menuModel?.showCoreOptions(all ?: emptyList(), lockedOptions + graphicsForcedOptions + restartOnlyOptions) }
                }
            }
            is MenuEffect.SetOption -> {
                val generation = ++optionsGeneration
                if (effect.key in NdsRenderer.APPLY_ON_RESTART) {
                    // Never pushed live (a GL <-> software switch can't happen mid-game): shown and saved, used at the next launch.
                    pendingRestartOptions[effect.key] = effect.value
                    toast("Render mode applies when the game restarts")
                } else {
                    applyOptions(listOf(Variable(effect.key, effect.value)))
                    n3dsCoreValues(mapOf(effect.key to effect.value))
                }
                // Re-read so core-driven visibility changes (and the value the core accepted) show up. A read
                // overtaken by a newer change is dropped, so it can't revert that change on screen.
                withCoreOptions { all ->
                    if (all != null && generation == optionsGeneration) menuModel?.updateOptions(all)
                }
            }
            is MenuEffect.SetFrontend -> applyFrontend(frontendValues + (effect.key to effect.value))
            is MenuEffect.SetShader -> applyShader(shaderValues + (effect.key to effect.value))
            is MenuEffect.SetShaders -> applyShader(shaderValues + effect.entries)
            is MenuEffect.SetCheat -> {
                cheatSet = cheatSet.toggle(effect.index, effect.enabled)
                // A toggle during load must not reach retro_cheat_reset before retro_load_game; the first-frame block re-applies the full set.
                if (gameReady) retroView?.let(::applyCheats)
            }
            is MenuEffect.ChangeDisc -> retroView?.let { view ->
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { view.changeDisk(effect.index) }.onFailure { NxLog.w("GameActivity", "disc change failed", it) }
                }
            }
            MenuEffect.SaveForConsole -> saveOptions(game = false)
            MenuEffect.SaveForGame -> saveOptions(game = true)
            MenuEffect.RestoreDefaults -> restoreOptions()
            is MenuEffect.SetPlayer -> slots?.let { s ->
                s.assign(effect.slot, effect.key?.let(::DeviceKey)).forEach { port ->
                    releasePort(port)
                    // The rumble follows the pad now in the slot (none: silent; port 0 falls back to the phone).
                    val id = (s.slots()[port] as? SlotView.Playing)?.key?.let { k -> keysById.entries.firstOrNull { it.value == k }?.key }
                    rumbles.getOrNull(port)?.useDevice(id)
                }
                connectDolphinPorts()   // a slot filled here is connected; one emptied stays so (no unplug prompt)
                menuModel?.let(::publishPlayers)
            }
            is MenuEffect.SetPlaying -> slots?.let { s ->
                s.setOutOfPlay(DeviceKey(effect.key), out = !effect.playing)?.let { port ->
                    releasePort(port)
                    rumbles.getOrNull(port)?.useDevice(null)
                }
                AndroidAppSettings(this).benchedPads = s.outOfPlay.map { it.value }.toSet()
                // As on a connect or disconnect: the layout again (a Not playing pad does not hide the on-screen pad,
                // [layoutControllerNames]) and the Players page republished.
                onInputDevicesChanged()
            }
        }
    }

    /**
     * Sets core options on the emulation (GL) thread, where the core reads them; queued, so a later
     * [withCoreOptions] read sees them.
     */
    private fun applyOptions(vars: List<Variable>) {
        if (netplay != null) return   // netplay: no core option changes mid-session (the other device would not follow)
        val view = retroView ?: return
        if (vars.isEmpty()) return
        view.queueEvent { runCatching { view.updateVariables(*vars.toTypedArray()) }.onFailure { NxLog.w("GameActivity", "setting options failed", it) } }
    }

    private suspend fun readDolphinBuiltIn(): String? = withContext(Dispatchers.IO) {
        val file = File(dolphinBuiltInPath ?: return@withContext null)
        if (file.isFile) runCatching { file.readText() }.onFailure { NxLog.w("GameActivity", "cannot read Dolphin's cheats", it) }.getOrNull() else null
    }

    /** First launch of a Wii/GameCube game: Dolphin has written its built-in list by the first frame, so combine again. */
    private suspend fun refreshDolphinCheats() {
        val player = dolphinPlayerCheats ?: return
        val builtIn = readDolphinBuiltIn() ?: return
        dolphinPlayerCheats = null
        cheatSet = DolphinCheats.refresh(builtIn, player, cheatSet.enabledKeys(), dolphinSavedCheats)
        menuModel?.let { it.setCheats(cheatSet.entries, cheatsFolder, cheatSet.expected); menuState.value = it.state }
    }

    /**
     * Sends the enabled cheats to the core on the emulation thread in one job: a reset, then only the enabled
     * ones with contiguous indices (PCSX-ReARMed treats the index as a slot and skips a cheat behind a gap,
     * libretro issue #116). Caught so a JNI failure never kills the GL thread.
     */
    private fun applyCheats(view: GLRetroView) {
        val codes = cheatSet.enabledCodes()
        view.queueEvent {
            runCatching {
                LibretroDroid.resetCheat()
                codes.forEachIndexed { i, code -> view.setCheat(i, true, code, useEmulationThread = false) }
            }.onFailure { NxLog.w("GameActivity", "applying cheats failed", it) }
        }
    }

    /**
     * Reads the core's options on the emulation thread, then runs [then] on the main thread and redraws.
     * [then] gets null when the read failed.
     */
    private fun withCoreOptions(then: (List<CoreOption>?) -> Unit) {
        val view = retroView ?: return
        lifecycleScope.launch {
            // Values kept from the running core until a restart show (and save) as set, not as the core runs.
            val all = view.readCoreOptions()
                ?.let { CoreOptionFilter.withHiddenValues(CoreOptionFilter.withPending(it, pendingRestartOptions), hiddenValues) }
            then(all)
            menuModel?.let { menuState.value = it.state }
        }
    }

    private fun saveOptions(game: Boolean) {
        val store = configStore
        // The menu has already popped to Options; say so rather than silently doing nothing.
        if (store == null || retroView == null) { toast("Could not save options"); return }
        withCoreOptions { all ->
            // A failed read must never be written. An empty one (a core without options) still saves the
            // frontend keys; render() keeps every existing line it isn't given.
            if (all == null) { toast("Could not save options"); return@withCoreOptions }
            lifecycleScope.launch {
                // A game save before the cheat load lands would write an empty nx_cheats and wipe the saved list.
                if (game) cheatsLoad?.join()
                // Under the hotkey saves' lock, and with the frontend values read once it is held: a hotkey pressed
                // meanwhile has already applied its value, so this write keeps it instead of bringing back the old one.
                val written = configWrites.withLock {
                    // A key Graphics API forced for this launch keeps its stored value: the running one is the session's only.
                    val values = CoreConfig.valuesToSave(all.map { it.key to it.value }, storedValues, graphicsForcedOptions) + frontendValues.toList() +
                        ShaderOptions.activeParamEntries(shaderValues, shaderParams) +
                        if (game) listOf(FrontendOptions.CHEATS to CheatSet.encode(cheatSet.enabledKeys())) else emptyList()
                    withContext(Dispatchers.IO) {
                        runCatching {
                            if (game) store.saveGame(values) else store.saveConsole(values).also { store.deleteGame() }
                        }.onFailure { NxLog.w("GameActivity", "saving options failed", it) }.getOrNull()
                    }
                }
                if (written == null) toast("Could not save options") else {
                    configSource = if (game) ConfigSource.GAME else ConfigSource.CONSOLE
                    lockedOptions = written.locked + forcedOptions
                }
                val model = menuModel ?: return@launch
                if (written != null) {
                    model.setStatus(configSource)
                    model.showMessage("Save Changes", if (game) "Saved for game." else "Saved for console.")
                }
                menuState.value = model.state
            }
        }
    }

    /** Deletes the config file in use and falls back one layer (game → console → defaults), applied live. */
    private fun restoreOptions() {
        val store = configStore
        // The menu has already popped to Options; say so rather than silently doing nothing.
        if (store == null || retroView == null) { toast("Could not save options"); return }
        withCoreOptions { all ->
            if (all == null) { toast("Could not save options"); return@withCoreOptions }
            lifecycleScope.launch {
                val next = configWrites.withLock {   // after any hotkey save in flight, not interleaved with it
                    withContext(Dispatchers.IO) {
                        runCatching {
                            when (configSource) {
                                ConfigSource.GAME -> store.deleteGame()
                                ConfigSource.CONSOLE -> store.deleteConsole()
                                ConfigSource.NONE -> Unit
                            }
                            val (console, game) = store.read()
                            CoreConfig.resolve(descriptorDefaults, console, game, forcedOptions, hiddenValues)
                        }.onFailure { NxLog.w("GameActivity", "restoring options failed", it) }.getOrNull()
                    }
                }
                if (next == null) {
                    toast("Could not save options")
                } else {
                    // The renderer is never switched live; the restored config picks it at the next launch.
                    pendingRestartOptions.clear()
                    applyOptions(all.mapNotNull { o ->
                        if (o.key in NdsRenderer.APPLY_ON_RESTART) return@mapNotNull null
                        // A forced option keeps what the launch set (picodrive_input1 follows Controller Type through applyFrontend).
                        if (o.key in forcedOptions || o.key in graphicsForcedOptions) return@mapNotNull null
                        val target = next.values[o.key] ?: o.defaultValue
                        if (target.isNotEmpty() && target != o.value) Variable(o.key, target) else null
                    })
                    configSource = next.source
                    lockedOptions = next.locked
                    storedValues = next.values
                    n3dsCoreValues(mapOf(
                        N3dsOptions.LAYOUT to (next.values[N3dsOptions.LAYOUT] ?: ""),
                        N3dsOptions.DEADZONE to (next.values[N3dsOptions.DEADZONE] ?: ""),
                    ))
                    applyFrontend(FrontendOptions.fromConfig(next.values, gameTag))
                    applyShader(ShaderOptions.fromConfig(next.values))
                }
                val model = menuModel ?: return@launch
                if (next != null) {
                    model.setFrontend(frontendValues, overlayNames(), menuLocked(), gameTag, overlayAvailable = !ndsSplit)
                    model.setShaders(shaderValues, presetNames, shaderParams)
                    model.setStatus(configSource)
                    model.showMessage("Save Changes", if (next.source == ConfigSource.CONSOLE) "Restored console config." else "Restored defaults.")
                }
                menuState.value = model.state
            }
        }
    }

    /**
     * minarch's Menu_autosaveQuit: screenshot + slot 9 + point the slot file at it, then leave. The battery save is
     * written once, off the main thread, before finish() (so the write lands before the native destroy); on a handoff
     * this runs inside the new screen's onCreate, which it never blocks (the new game waits for this screen's release).
     * The core is read first (state, then SRAM, back to back under [coreLock]), and only then comes the wait (a queued
     * Home write), so a destroy during the wait never finds the serialize still ahead of it. [chosen]: the player picked
     * Quit, so the slot file points at the auto state as minarch does; a quit nobody chose (a newer game screen took the
     * session) leaves the player's manual slot pointer alone, as leaving via Home does ([writeAutoStateOnStop]).
     */
    private fun quitWithAutoState(chosen: Boolean, autoState: Boolean = true) {
        if (quitting) return   // single-shot: a second job could serialize after the core is destroyed
        quitting = true
        rumbles.forEach { it.release() }
        endGameTime()
        val view = retroView
        val ss = saveStates
        gcClearTriggers()   // a latched soft press never outlives the game
        stopAchievements()
        if (view == null || !gameReady) { finish(); return }   // not running yet: no save to write
        // While a resume is still loading, the running state is the fresh boot: keep the resume point as is. Without
        // [autoState] (the core asked to end) the running state is not worth resuming: keep it as is too.
        if (ss == null || resumePending || !autoState) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    withLiveCore { writeBatterySave(notify = true) }
                }
                finish()
            }
            return
        }
        captureThumbnail { bmp ->
            lifecycleScope.launch {
                // A manual Save/Load still running must finish before the state is serialized.
                slotJobs.toList().forEach { it.join() }
                // true = written, null = skipped (the core can't save states), false = failed.
                val ok: Boolean? = withContext(Dispatchers.IO) {
                    // Both reads of the core before any wait; null = the core was already destroyed (nothing to write).
                    val bytes = withLiveCore {
                        // On the GL thread (a core may draw inside it: Azahar), waited for under the lock: the GL
                        // thread never takes coreLock, so the destroy observer just waits for it.
                        val state = runCatching { view.serializeState() }
                            .onFailure { NxLog.e("GameActivity", "auto-resume state failed", it) }
                        writeBatterySave(notify = true)   // local: always completes, before the native destroy
                        state
                    }
                    val ok = when {
                        bytes == null || bytes.isFailure -> false
                        bytes.getOrThrow().isEmpty() -> null
                        else -> runCatching {
                            // A queued Home write must land first: it writes the same files and is older.
                            awaitAutoWrites()
                            if (bmp != null) ss.writeAutoThumb(bmp)
                            ss.writeAuto(bytes.getOrThrow())
                            ss.markAuto(onlyIfNotManual = !chosen)
                            true
                        }.onFailure { NxLog.e("GameActivity", "auto-resume state failed", it) }.getOrDefault(false)
                    }
                    ok
                }
                if (ok == false) toast("Could not save resume point")
                finish()
            }
        }
    }

    /**
     * Runs [block] (off-main native calls) unless the core is destroyed; null when it is. Holding [coreLock] keeps the
     * destroy (see its observer in startEmulator) from starting under the block. Not for the main thread's own calls:
     * they cannot overlap onDestroy.
     */
    private fun <T> withLiveCore(block: () -> T): T? = synchronized(coreLock) { if (coreGone) null else block() }

    /**
     * Re-reads the slot strip (and the last-used slot) on IO and publishes it to [slotsUi]; main thread. The listing and
     * the thumbnails' bounds stay off the main thread: the previous strip shows until this lands (it differs only after
     * a write, which refreshes it too), and a newer refresh cancels an older one so a stale read never lands last.
     */
    private fun refreshSlots() {
        val ss = saveStates ?: run { slotsUi.value = emptyList(); return }
        slotsJob?.cancel()
        slotsJob = lifecycleScope.launch {
            val (slots, last) = withContext(Dispatchers.IO) {
                runCatching { readSlots(ss) to ss.lastSlot() }.onFailure { NxLog.w("GameActivity", "could not list the slots", it) }.getOrNull()
            } ?: return@launch
            slotsUi.value = slots
            lastSlotCache = last
        }
    }

    /** The slot strip for [ss]: file listings and thumbnail bounds. Blocking I/O. */
    private fun readSlots(ss: SaveStates): List<SlotUi> =
        ss.slots().map { s ->
            val sub = if (s.occupied) formatSlotTime(s.timestampMillis) else "empty"
            val thumb = s.thumb?.let { Uri.fromFile(it).buildUpon().fragment(it.lastModified().toString()).build().toString() }
            SlotUi(
                label = "Slot ${s.index}",
                subtitle = sub,
                thumbPath = thumb,
                occupied = s.occupied,
                aspect = s.thumb?.let { thumbAspect(it) } ?: (4f / 3f),
            )
        }

    /** Reads a thumbnail's width/height (bounds only) to get its aspect, so the preview frame hugs it. */
    private fun thumbAspect(file: File): Float {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
        return if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight else 4f / 3f
    }

    private fun formatSlotTime(ms: Long): String =
        java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US).format(java.util.Date(ms))

    private fun saveToSlot(slot: Int) {
        val view = retroView ?: return
        val ss = saveStates ?: return
        captureThumbnail { bmp ->
            trackSlotJob(lifecycleScope.launch(Dispatchers.IO) {
                val ok = runCatching {
                    // Empty = the core refused (or is gone): keep the slot's old state and report the failure.
                    val bytes = view.serializeState()
                    check(bytes.isNotEmpty()) { "core returned no state" }
                    ss.writeState(slot, bytes)
                    if (bmp != null) ss.writeThumb(slot, bmp)
                }.isSuccess
                withContext(Dispatchers.Main) {
                    if (ok) { refreshSlots(); toast("Saved to slot $slot") } else toast("Save failed")
                }
            })
        }
    }

    private fun loadFromSlot(slot: Int) {
        val view = retroView ?: return
        val ss = saveStates ?: return
        // The read is in the job too, not on main: a state file is up to tens of MB (PSP and DC).
        trackSlotJob(lifecycleScope.launch(Dispatchers.IO) {
            val read = runCatching { ss.readState(slot) }.onFailure { NxLog.e("GameActivity", "could not read slot $slot", it) }
            if (read.getOrNull() == null && read.isSuccess) return@launch   // empty slot: ignore (the menu stays open)
            val ok = read.getOrNull()?.let { bytes -> runCatching { view.unserializeState(bytes) }.getOrDefault(false) } ?: false
            if (ok) runCatching { saveStates?.rememberSlot(slot) }
                .onFailure { NxLog.e("GameActivity", "could not record last slot", it) }
            // The loaded state's rumble may stay latched unreported: start from still rather than re-apply the old one.
            withContext(Dispatchers.Main) { if (ok) { rumbles.forEach { it.forget() }; refreshSlots(); closeMenu() } else toast("Load failed") }   // refresh: the pointer moved
        })
    }

    /** Main thread only (menu handlers and the main-looper PixelCopy callback). */
    private fun trackSlotJob(job: Job) {
        slotJobs.removeAll { !it.isActive }
        slotJobs += job
    }

    /** PixelCopy the current (paused) game frame for a screenshot at the view's native pixel size (no scaling). */
    private fun captureThumbnail(cb: (Bitmap?) -> Unit) {
        val view = retroView ?: return cb(null)
        val w = view.width
        val h = view.height
        if (w <= 0 || h <= 0) return cb(null)
        val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(view, full, { result ->
                if (result == PixelCopy.SUCCESS) {
                    cb(full)
                } else {
                    full.recycle()
                    cb(null)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            full.recycle()
            cb(null)
        }
    }

    /**
     * Persists the battery save locally. Safe from any thread: the native side locks the core, and writes are
     * serialized on the [BatterySaves] instance so they land in call order. [notify] toasts a failure. Returns true
     * when the file changed (unchanged SRAM is not rewritten). Saves live in private storage only. The clock file
     * ([clockSave]) is written with it; either changing returns true.
     */
    private fun writeBatterySave(notify: Boolean): Boolean {
        val view = retroView ?: return false
        val battery = batterySaves ?: return false
        if (!gameReady) return false
        // netplay: only a host on its folder's own core, with its saves served, writes its real saves; a copy never.
        netplay?.let { if (it.onCopy) return false }
        return runCatching {
            synchronized(battery) {
                val wrote = battery.write(view.serializeSRAM(useEmulationThread = false))
                // The clock file alongside (minarch writes both together); its failure is logged, never toasted.
                val clockWrote = clockSave?.let { c ->
                    runCatching { c.write(view.serializeRTC(useEmulationThread = false)) }
                        .onFailure { NxLog.e("GameActivity", "clock save failed", it) }.getOrDefault(false)
                } ?: false
                wrote || clockWrote
            }
        }
            .onFailure {
                NxLog.e("GameActivity", "battery save failed", it)
                if (notify) runOnUiThread { toast("Battery save failed") }
            }.getOrDefault(false)
    }

    // ------------------------------------------------------------------ netplay

    /**
     * netplay: the saves a session plays on, before the battery load. A host on its folder's own core keeps its
     * real saves but loses the game's auto-resume state (handheld 19d3a5e1), so a later resume cannot roll back battery
     * saves written in the session; manual slots stay. A host on another core plays on a fresh copy of its own battery
     * saves in the session folder; one whose saves were not served (emptySaves) on an empty copy. A client plays on the
     * host's saves, committed there before launch (an empty folder when the host had none).
     */
    private suspend fun prepareNetplaySaves(np: NetplaySession, tag: String, romFileName: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                when {
                    !np.onCopy -> {
                        awaitAutoWrites()   // a Home write of this game's auto state may still be landing
                        val marker = File(sdRoot, NxPaths(tag, np.folderCoreId, romFileName).lastSlot).takeIf { it.exists() }?.readText()
                        NetplaySaves.resumeVictims(tag, np.folderCoreId, romFileName, marker, paths.naming, paths.innerFileName) { File(sdRoot, it).exists() }
                            .forEach { File(sdRoot, it).delete() }
                        lastSlotCache = null
                    }
                    // A host on another core plays on a copy of its own saves; one whose saves were not served on an empty
                    // copy, as the joiner does. Under the session folder's lock (prepareHost).
                    np.role == NetplayRole.HOST -> {
                        val savesDir = if (np.emptySaves) null else File(sdRoot, paths.savesDir)
                        val copied = NetplaySessionDir.prepareHost(filesDir, netplayOwner, savesDir, paths) {
                            NxLog.e(TAG_LOG, "netplay: copying the host's own saves failed", it)
                        }
                        if (!copied) netplayOwnCopyFailed = true
                    }
                    // The host's copy committed before launch (kept), or an empty one; a copy an older screen claimed is stale.
                    else -> NetplaySessionDir.claim(filesDir, netplayOwner)
                }
            }.onFailure {
                NxLog.e(TAG_LOG, "netplay saves setup failed", it)
                // A host on a copy whose claim itself failed plays without its saves too: say so (first frame).
                if (np.role == NetplayRole.HOST && np.onCopy) netplayOwnCopyFailed = true
            }
        }
    }

    /**
     * netplay: starts this screen's session once the first frame is up ([netplayGame] set first, so onDestroy shuts down
     * what started). A GBA link's engine started before the load ([connectLink]); its status ticks from now. GB link has
     * no engine: gambatte runs it.
     */
    private suspend fun startNetplay(np: NetplaySession): Boolean = when (NetplayCores.link(np.coreId)) {
        null -> NetplayGame(np, lifecycleScope, SystemClock::elapsedRealtime, ::onNetplayNotice).also { netplayGame = it }.start()
        NetplayLinkKind.NETPACKET -> (netplayGame as? NetplayLinkGame)?.let { it.begin(); it.live } ?: false
        NetplayLinkKind.GBLINK -> true
    }

    /** netplay link: the GBA link engine, started before the core loads; the raw gpsp_serial the core runs with, or null. */
    private suspend fun connectLink(np: NetplaySession, ownSerial: String?): String? =
        NetplayLinkGame(np, lifecycleScope, SystemClock::elapsedRealtime, ::onNetplayNotice).also { netplayGame = it }.connect(ownSerial)

    /** netplay: a status notice from [NetplayGame]'s ticker (main thread). */
    private fun onNetplayNotice(notice: NetplayNotice) {
        val now = SystemClock.elapsedRealtime()
        when (notice) {
            NetplayNotice.Clear -> {
                if (!netplayPillShown) return
                notifications.hideProgress()
                netplayPillShown = false
            }
            is NetplayNotice.Pill -> {
                notifications.progress(notice.text, "", null, NETPLAY_PILL_MS, now)
                netplayPillShown = true
            }
            is NetplayNotice.Ended -> {
                if (netplayPillShown) notifications.hideProgress()
                netplayPillShown = false
                notifications.push(notice.text, null, NETPLAY_NOTICE_MS, now)
                // The ended session holds no frames: an open menu pauses the game as for any other launch.
                if (menuOpen) retroView?.frameSpeed = 0
                netplayEnded = true
                menuModel?.let { it.netplayEnded = true; it.refresh(); menuState.value = it.state }
            }
            is NetplayNotice.Failed -> {
                if (netplayPillShown) notifications.hideProgress()
                netplayPillShown = false
                fail(notice.text, NetplayText.FAILED)
            }
        }
        publishNotifications()
    }

    // ------------------------------------------------------------------ achievements

    /**
     * Before the view exists: an RA console, signed in and enabled, gets a bridge and a background hash. An archive's
     * unpacked [inner] ROM is what is hashed, and its name refines the console.
     */
    private fun prepareAchievements(tag: String, romFileName: String, content: ContentRef, inner: File?) {
        val settings = AndroidAppSettings(this)
        if (!settings.raEnabled || settings.raToken == null) return
        val consoleId = RaConsoles.consoleId(tag, inner?.name ?: romFileName) ?: return
        val controller = AchievementsService.controller(this, settings, AndroidLibraryIndexStore(this))
        val hasher = AndroidRaHasher(this, AndroidRomsSources.of(this))
        val folder = intent.getStringExtra(EXTRA_FOLDER_URI) ?: ""
        val pointer = intent.getStringExtra(EXTRA_GAME_PATH)?.let(GamePath::split)?.let { ref ->
            RaGamePointer(tag, ref.sourceId, ref.relativeDir, ref.fileName, intent.getStringExtra(EXTRA_TITLE) ?: ref.fileName)
        }
        // The launch's place in hashes.json; always completed (null on a failure) so the bridge's await never hangs.
        val launch = CompletableDeferred<RaLaunchFingerprint?>()
        val hashJob = lifecycleScope.async(Dispatchers.IO) {
            try {
                val ref = content.value
                // The shared per-folder hashes.json, so a game is hashed once while its file is unchanged (a disc takes a while).
                val target = pointer?.let {
                    RaLaunchTarget(it, hasher.stamp(ref), consoleId, RaFingerprintKeys.storable(tag, romFileName, consoleId, unpacked = inner != null))
                }
                var computed = false
                val (h, l) = RaLaunchHash.of(controller.fingerprints, target) {
                    computed = true
                    if (inner != null) hasher.hashUnpacked(tag, inner) else hasher.hash(tag, ref, romFileName, folder)
                }
                launch.complete(l)
                NxLog.i(TAG_LOG, if (!computed) "RA hash: cached" else if (h == null) "RA hash: unidentified" else "RA hash: computed")
                h
            } finally {
                launch.complete(null)   // no-op once completed above
            }
        }
        achievements = AchievementsBridge(controller, settings, lifecycleScope).also { it.prepare(consoleId, hashJob, pointer, launch) }
    }

    /** Idempotent; onDestroy, a finishing onStop and the quit path all reach it. */
    private fun stopAchievements() {
        achievements?.stop()
        achievements = null
    }

    /** The Options row and Achievements page: hidden without a session, "Not recognised" while there is no set. */
    private fun applyAchievements(model: InGameMenuModel) {
        val b = achievements?.takeIf { it.available } ?: return model.setAchievementsAvailable(false)
        val summary = b.summary()
        model.setAchievements(if (summary != null) b.list() else null, summary, AndroidAppSettings(this).raSortOrder, b.muted, b.badges)
    }

    private fun refreshMenuAchievements() {
        menuModel?.let { applyAchievements(it); menuState.value = it.state }
    }

    private fun toggleMute(id: Int) {
        val b = achievements ?: return
        lifecycleScope.launch {
            runCatching { b.toggleMute(id) }.onFailure { NxLog.w("GameActivity", "mute toggle failed", it) }
            refreshMenuAchievements()
        }
    }

    private fun onAchievementEvent(e: RaEvent) {
        val b = achievements ?: return
        val settings = AndroidAppSettings(this)
        val now = SystemClock.elapsedRealtime()
        val toastMs = settings.raNotificationSeconds * 1000L
        fun badge(a: RaAchievement) = RaBadgeFolders.fileName(a.badge, locked = false)?.let(b.badges::get)
        fun notice(text: String) { notifications.push(text, null, toastMs, now) }
        // Off: only the session notices (loaded, unknown, sign-in, offline, first-time setup, synced) still show.
        val shown = settings.raShowNotifications
        when (e) {
            is RaEvent.Unlocked -> if (shown) notifications.push("Achievement Unlocked\n${e.a.title}", badge(e.a), toastMs, now)
            is RaEvent.Progress -> if (shown) notifications.progress(e.a.title, e.a.progress ?: "", badge(e.a), settings.raProgressSeconds * 1000L, now)
            RaEvent.ProgressHide -> notifications.hideProgress()
            RaEvent.Mastered -> if (shown) notice("Game Mastered!")
            is RaEvent.ServerError -> if (shown) notice("RA server error: ${e.message}")
            RaEvent.Disconnected -> if (shown) notice("RetroAchievements: connection lost")
            RaEvent.Reconnected -> if (shown) notice("RetroAchievements: reconnected")
            is RaEvent.GameLoaded -> notice("${e.title} · ${e.unlocked}/${e.total} achievements")
            RaEvent.GameUnknown -> notice("No achievements for this game")
            is RaEvent.LoginFailed -> notice("RetroAchievements sign-in failed")
            RaEvent.SessionOffline -> notice("RetroAchievements: offline")
            RaEvent.SessionDisabled -> notice("RetroAchievements needs a network connection for first-time setup")
            is RaEvent.Synced -> notice("${e.count} offline achievement${if (e.count == 1) "" else "s"} synced")
            RaEvent.BadgesReady -> Unit
        }
        // The session changed what the menu shows (row, summary, the set): refresh it if it is open.
        when (e) {
            is RaEvent.GameLoaded, RaEvent.GameUnknown, is RaEvent.LoginFailed, RaEvent.SessionDisabled, RaEvent.SessionOffline,
            is RaEvent.Unlocked, is RaEvent.Synced, RaEvent.BadgesReady -> refreshMenuAchievements()
            else -> Unit
        }
        publishNotifications()
    }

    /** Publishes the queue and keeps a 250 ms expiry tick running while anything is shown. */
    private fun publishNotifications() {
        notificationState.value = notifications.toasts to notifications.pill
        if (tickJob?.isActive == true) return
        tickJob = lifecycleScope.launch {
            while (notifications.toasts.isNotEmpty() || notifications.pill != null) {
                delay(250)
                if (notifications.tick(SystemClock.elapsedRealtime())) notificationState.value = notifications.toasts to notifications.pill
            }
        }
    }

    /** Display-only overlay over the game area (portrait: above the pad band while it is shown), below the menu; takes no input. */
    private fun ensureNotificationsView() {
        if (notificationsView != null) return
        val view = ComposeView(this).apply {
            isClickable = false
            isFocusable = false
            setContent {
                val config = LocalConfiguration.current
                NxTheme {
                    val (toasts, pill) = notificationState.value
                    // The overlay starts at the screen top (under a portrait cutout). In portrait with the pad band shown
                    // it ends above the band, not at the bottom edge, so the bottom is not inset; otherwise (landscape, or
                    // the band dropped for a controller) it reaches the bottom edge and every side is inset.
                    val band = config.orientation == Configuration.ORIENTATION_PORTRAIT && portraitBandPx() > 0
                    val sides = if (band) WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        else WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical
                    // Over the landscape overlay pad, top-centre (its system row holds the top corners, its d-pad
                    // the bottom-left); elsewhere the corners.
                    val placement = nxNotificationPlacement(overlayPadShown.value)
                    // Between the clusters' system rows: the same screen, cutout inset and unit the clusters are laid out with.
                    val density = LocalDensity.current
                    val dir = LocalLayoutDirection.current
                    val cutout = WindowInsets.displayCutout
                    val inset = with(density) {
                        symmetricCutoutInset(cutout.getLeft(density, dir).toFloat(), cutout.getRight(density, dir).toFloat()).toDp().value
                    }
                    // This view's own size: in landscape the root's, which the clusters (MATCH_PARENT tall) fit their unit to.
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val maxW = nxTopCentreMaxWidth(maxWidth.value, maxHeight.value, inset).dp
                        NxNotifications(toasts, pill, safeSides = sides, placement = placement, topCentreMaxWidth = maxW)
                    }
                }
            }
        }
        notificationsView = view
        findViewById<FrameLayout>(R.id.root).addView(view, menuLayoutParams(resources.configuration.orientation))
    }

    /** Shows [msg] briefly; returns the toast so a caller can take it down early. */
    private fun toast(msg: String): android.widget.Toast =
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).also { it.show() }

    /** Release any held game inputs so nothing sticks in the core while the menu is up. */
    private fun releaseAllGameInput() {
        // Port 0 keeps its folded stick d-pad state; the other players' ports are released in full.
        val p0 = inputs[0]
        p0.sent.clear(); padSent.clear()
        p0.triggers.releaseAll()
        p0.selectHeld = false
        p0.startHeld = false
        for (p in 1 until inputs.count) releasePort(p)
        val view = retroView ?: return
        RELEASED_BUTTONS.forEach { b -> PadKeymap.keyCodeFor(b)?.let { view.sendKeyEvent(KeyEvent.ACTION_UP, it) } }
        RELEASED_THUMB_KEYS.forEach { view.sendKeyEvent(KeyEvent.ACTION_UP, it) }
        releaseDirections(view)
        // An analog trigger value wins over the keys in the runner and the menu swallows the release axis: zero it.
        if (isDc || gcPad) view.sendTriggers(0, 0f, 0f)
        if (gcPad) gcClearTriggers()
        if (isWii) view.setPointerVelocity(0, 0f, 0f)   // the menu swallows the stick's return to rest: the pointer stops
        ndsHotkeys.releaseAll()
        ndsHatX = 0
        ndsHatY = 0
        syncPen()
        if (isN3ds) {
            n3dsHotkeys.releaseAll()   // sends up what it held in the core (L3, R3, a tapping SELECT); a held-back SELECT is dropped
            n3dsHatX = 0
            n3dsHatY = 0
            n3dsHwRight = 0f to 0f
            n3dsPadRight = 0f to 0f
            syncN3dsCursor()
        }
    }

    /** Lets go of everything [port] held in the core: its keys, directions, sticks and DC or GameCube analog triggers. */
    private fun releasePort(port: Int) {
        inputs[port].reset()
        val view = retroView ?: return
        RELEASED_BUTTONS.forEach { b -> PadKeymap.keyCodeFor(b)?.let { view.sendKeyEvent(KeyEvent.ACTION_UP, it, port) } }
        RELEASED_THUMB_KEYS.forEach { view.sendKeyEvent(KeyEvent.ACTION_UP, it, port) }
        releaseDirections(view, port)
        view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, 0f, 0f, port)
        if (isWii) view.setPointerVelocity(port, 0f, 0f)   // an unplugged pad's tilted stick must not keep moving the pointer
        if (isDc || gcPad) view.sendTriggers(port, 0f, 0f)
    }

    /**
     * Lets go of every direction [port] holds in the core: the d-pad (as a hat and as keys) and the left stick. Port 0
     * also clears the on-screen and DS hats, which are only ever port 0's.
     */
    private fun releaseDirections(view: GLRetroView, port: Int = 0) {
        if (port == 0) {
            dpadHat.clear()
            ndsHat.clear()
        }
        view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, 0f, 0f, port)
        view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, 0f, 0f, port)
        listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
            .forEach { view.sendKeyEvent(KeyEvent.ACTION_UP, it, port) }
    }

    /** Drives the open menu from a hardware hat / left-stick (vertical = item, horizontal = slot). */
    private fun routeMenuMotion(event: MotionEvent) {
        menuAxes.update(
            event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y),
        )
    }

    // ------------------------------------------------------------------ on-screen pad input

    private fun onPad(event: PadEvent) {
        // The menus follow the letter the on-screen pad shows (Dreamcast draws "A" on NxButton.B); the game gets the raw button.
        rumbles.firstOrNull()?.useDevice(null)   // the on-screen pad: the phone in the player's hands takes the rumble
        if (menuOpen) { menuRouter.onEvent(event.map(padProfile()::menuButton)); return }
        val btn = event.button
        val down = event is PadEvent.Down
        // MENU opens the pause menu (not forwarded to the core).
        if (btn == NxButton.MENU) { if (down) toggleMenu(); return }
        // The Wii's HOME: a resume point first, then a tap of the core's Home ([pressWiiHome]); the release is the tap's.
        if (btn == NxButton.HOME) { if (down && isWii) pressWiiHome(); return }
        // SELECT + START held together also opens it (the on-screen pad is P1, port 0).
        if (inputs.menuChord(0, btn, down)) { toggleMenu(); return }
        if (isNds && !routeNds(btn, down)) return   // R2, L2, SELECT + a direction and the stylus inputs stay out of the core
        if (isN3ds && !routeN3ds(btn, down)) return  // SELECT (held back for chords), its chords and the stylus inputs: routeN3ds sends what they become
        val view = retroView ?: return
        if (gcPad && (btn == NxButton.L2 || btn == NxButton.R2)) { gcTrigger(btn, down); return }
        if (btn.isDirection) {
            if (down) dpadHat.press(btn) else dpadHat.release(btn)
            view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, dpadHat.x.toFloat(), dpadHat.y.toFloat(), 0)
        } else {
            val code = if (isN64) N64Input.toRunner(btn, PadKeymap.keyCodeFor(btn) ?: 0)
                else PadKeymap.keyCodeFor(padMap.sends(btn))
            if (code == null || code == 0) return
            val sent = if (down) padSent.down(btn, code) else padSent.up(btn, code)
            view.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, sent)
        }
    }

    private fun onStick(x: Float, y: Float) = stickRouter.onStick(x, y, menuOpen)

    /** The on-screen New 3DS C-stick: the core's right analog (Azahar's C-stick, and its pointer in the default mode). */
    private fun onCStick(x: Float, y: Float) {
        if (menuOpen || !isN3ds) return
        n3dsPadRight = x to y
        syncN3dsCursor()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        // The rumble follows the pad in use, on its own port (a one-player core: port 0); a pad with no slot yet claims below.
        if (event != null && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) portFor(event.device, claim = false)?.let { noteRumbleDevice(event.device, it) }
        if (menuOpen) { event?.let { routeMenuMotion(it) }; return true }
        val view = retroView ?: return super.onGenericMotionEvent(event)
        if (event != null && isN3ds) {
            if (event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
                routeN3dsHat(view, event)
                routeTriggerAxes(event)     // L2/R2 are ZL/ZR: an axis-only trigger presses them too
                val sx = event.getAxisValue(MotionEvent.AXIS_X)
                val sy = event.getAxisValue(MotionEvent.AXIS_Y)
                if (!n3dsHotkeys.onStick(sx, sy)) view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, sx, sy, 0)
                n3dsHwRight = event.getAxisValue(MotionEvent.AXIS_Z) to event.getAxisValue(MotionEvent.AXIS_RZ)
                syncN3dsCursor()
                return true   // consumed, as for the DS: no synthesised d-pad keys passing routeN3ds twice
            }
            return super.onGenericMotionEvent(event)
        }
        if (event != null && isNds) {
            // Joystick events only: a mouse or hover event reads its hat, trigger and stick axes as 0, which would
            // release a held hat, re-fire a trigger or move the pen.
            if (event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
                routeNdsHat(view, event)
                routeTriggerAxes(event)
                val sx = event.getAxisValue(MotionEvent.AXIS_X)
                val sy = event.getAxisValue(MotionEvent.AXIS_Y)
                if (!ndsHotkeys.onStick(sx, sy)) view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, sx, sy, 0)
                view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ), 0)
                // Consumed: unhandled, Android would synthesise d-pad keys from the hat and stick, and they would pass
                // routeNds a second time (a SELECT chord firing twice). routeNdsHat sends those directions itself.
                return true
            }
            return super.onGenericMotionEvent(event)
        }
        // Joystick events only, and consumed. Unhandled, Android would synthesise d-pad keys from the hat and the left
        // stick, which would reach the core through onKeyDown as a second d-pad (a stick tilt also pressing the d-pad).
        // The hat goes as MOTION_SOURCE_DPAD here, and on a core without an analog stick the stick too (folded in below,
        // coreHasStick); the open menu reads the axes itself (routeMenuMotion, above). A mouse or hover event is not a
        // pad: its AXIS_X/Y are pointer coordinates and its hat reads 0 (it would release a held hat), so it goes to the
        // system.
        if (event != null && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            // A pad with no player slot claims one only with an axis pushed past half (not its resting report on connect).
            // The right stick (AXIS_Z/RZ) never claims: some pads rest those at -1.
            // A one-player core (no slots) builds nothing: portFor gives port 0 whatever the claim.
            val claims = slots != null && axesClaim(floatArrayOf(
                event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y),
                event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_BRAKE), event.getAxisValue(MotionEvent.AXIS_GAS),
            ))
            val port = portFor(event.device, claim = claims) ?: return true   // no slot: dropped
            noteRumbleDevice(event.device, port)
            val inp = inputs[port]
            // Dreamcast triggers are analog (flycast reads RETRO_DEVICE_INDEX_ANALOG_BUTTON).
            if (isDc || gcPad) {   // GameCube triggers too: Dolphin reads them as analog buttons
                val l = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
                val r = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
                view.sendTriggers(port, l, r)
            } else {
                routeTriggerAxes(event, port)   // a digital L2/R2 from a pad that reports its triggers only as axes
            }
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val sx = event.getAxisValue(MotionEvent.AXIS_X)
            val sy = event.getAxisValue(MotionEvent.AXIS_Y)
            if (coreHasStick) {
                view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, hatX, hatY, port)
            } else {
                // The runner's JOYPAD d-pad is the hat axes and keys only, and consuming the event drops the d-pad keys
                // Android would make from the stick, so a digital core's stick is folded in here.
                inp.stickDpadX = stickDpadStep(sx, inp.stickDpadX)
                inp.stickDpadY = stickDpadStep(sy, inp.stickDpadY)
                view.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, hatOrStick(hatX, inp.stickDpadX).toFloat(), hatOrStick(hatY, inp.stickDpadY).toFloat(), port)
            }
            view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, sx, sy, port)
            val rz = event.getAxisValue(MotionEvent.AXIS_Z) to event.getAxisValue(MotionEvent.AXIS_RZ)
            if (isWii && wiiPad != null && wiiPad != WiiPad.CLASSIC) {
                // Wiimote types: the right stick aims the IR pointer (Dolphin's pointer reads POINTER, ir_mode 2).
                val (vx, vy) = WiiPointer.velocity(rz.first, rz.second)
                view.setPointerVelocity(port, vx, vy)
            } else {
                view.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, rz.first, rz.second, port)
            }
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    // Volume keys are never pad buttons (PadKeymap has none): they go to the system, not the core or the menu.
    private fun isVolumeKey(keyCode: Int) = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE

    /** A real controller's input on [port]: that port's rumble follows it (to its vibrator; port 0: else the phone's). */
    private fun noteRumbleDevice(device: InputDevice?, port: Int) {
        if (device != null && !device.isVirtual) rumbles.getOrNull(port)?.useDevice(device.id)
    }

    /** A physical pad: not virtual, with gamepad or joystick sources (a DualSense's touchpad device is neither). */
    private fun isPad(d: InputDevice): Boolean = !d.isVirtual && (
        (d.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (d.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)

    /**
     * The port [device]'s input goes to: 0 for a one-player core, the on-screen pad or a non-pad device; else its
     * player slot, claimed when [claim] (a key-down, an axis past half) and it has none. Null drops the input: every
     * slot is taken, or the pad is Not playing.
     */
    private fun portFor(device: InputDevice?, claim: Boolean): Int? {
        val s = slots ?: return 0
        if (device == null || !isPad(device)) return 0
        val key = DeviceKey(device.descriptor)
        keysById[device.id] = key
        padNames[key] = device.name
        if (!claim) return s.portOf(key)
        val c = s.claim(key) ?: return null
        if (c.joined) { onPlayerJoined(c.port, device); connectDolphinPorts() }
        return c.port
    }

    /** A pad took (or came back to) player slot [port]: a notice says who is who. */
    private fun onPlayerJoined(port: Int, device: InputDevice) {
        notifications.push("Player ${port + 1}: ${device.name}", null, PLAYER_NOTICE_MS, SystemClock.elapsedRealtime())
        publishNotifications()
    }

    /** The Players page's data: each slot (Empty, a live pad, a held one) and each connected pad, names numbered. */
    private fun publishPlayers(model: InGameMenuModel) {
        val s = slots ?: return
        val pads = connectedPads()
        pads.forEach {
            val key = DeviceKey(it.descriptor)
            keysById[it.id] = key   // so a SetPlayer finds the rumble of a pad never pressed
            padNames[key] = it.name
        }
        val keys = pads.map { DeviceKey(it.descriptor) }
        val numbered = keys.zip(PadNames.numbered(pads.map { it.name })).toMap()
        fun name(k: DeviceKey) = numbered[k] ?: padNames[k] ?: "Controller"
        model.setPlayers(
            s.slots().map { v ->
                when (v) {
                    SlotView.Empty -> PlayerSlotRow(null, "", connected = false)
                    is SlotView.Playing -> PlayerSlotRow(v.key.value, name(v.key), connected = true)
                    is SlotView.Held -> PlayerSlotRow(v.key.value, name(v.key), connected = false)
                }
            },
            keys.map { k -> PlayerPadRow(k.value, name(k), playing = k !in s.outOfPlay) },
        )
        menuState.value = model.state
    }

    /** The connected physical pads, one per descriptor (a pad may show as several input devices). */
    private fun connectedPads(): List<InputDevice> {
        val im = inputManager ?: return emptyList()
        return im.inputDeviceIds.toList().mapNotNull(im::getInputDevice).filter(::isPad).distinctBy { it.descriptor }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isVolumeKey(keyCode)) return super.onKeyDown(keyCode, event)
        // The rumble follows the pad in use, on its own port (a one-player core: port 0); a pad with no slot yet claims below.
        portFor(event.device, claim = false)?.let { noteRumbleDevice(event.device, it) }
        if (menuOpen) {
            if (keyCode == KeyEvent.KEYCODE_BACK) { if (event.repeatCount == 0) backPressed(); return true }
            if (event.repeatCount == 0) PadKeymap.buttonFor(printedKey(event, keyCode))?.let { menuRouter.onEvent(PadEvent.Down(it)) }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) { if (event.repeatCount == 0) backPressed(); return true }
        val first = event.repeatCount == 0
        val button = PadKeymap.buttonFor(keyCode)
        // Any pad can pause, before the slot lookup: also one with no slot (table full) or set Not playing. It claims nothing.
        if (button == NxButton.MENU) { if (first) toggleMenu(); return true }
        val port = portFor(event.device, claim = true) ?: return true   // no slot: dropped
        // Wii: the left-stick click recentres the phone's motion, the right-stick click is HOME ([pressWiiHome]); neither
        // reaches the core as itself.
        if (WiiShortcuts.isRecenter(isWii, keyCode)) { if (first) recenterWiiMotion(); return true }
        if (WiiShortcuts.isHome(isWii, keyCode)) { if (first) pressWiiHome(); return true }
        noteRumbleDevice(event.device, port)
        // SELECT + START opens the menu: P1's (port 0's) only. Held state follows every down, repeats too.
        if (button != null && inputs.menuChord(port, button, down = true) && first) { toggleMenu(); return true }
        // The hotkeys and stylus see the console button the key is (its printed letter on DS/3DS), as the core will.
        if (isNds) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->
            if (b == NxButton.L2 || b == NxButton.R2) { if (first && routeTriggerKey(b, down = true)) routeNds(b, down = true); return true }
            if (!routeNds(b, down = true, repeat = !first)) return true
        }
        if (gcPad) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b -> if (gcAnalogTriggerKey(b, event.device)) return true }
        if (!isNds && !isDc && first) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->
            if ((b == NxButton.L2 || b == NxButton.R2) && !routeTriggerKey(b, down = true, port)) return true   // its axis pressed it
        }
        if (isN3ds) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->
            if (!routeN3ds(b, down = true, repeat = !first)) return true
        }
        // As printed (or by position on positional consoles) through the console's pad map; N64 by name ([runnerKey]).
        runnerKey(event, keyCode, port)?.let { retroView?.sendKeyEvent(event.action, it, port) }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isVolumeKey(keyCode)) return super.onKeyUp(keyCode, event)
        if (menuOpen) {
            PadKeymap.buttonFor(printedKey(event, keyCode))?.let { menuRouter.onEvent(PadEvent.Up(it)) }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        val port = portFor(event.device, claim = false) ?: return true
        if (WiiShortcuts.isRecenter(isWii, keyCode) || WiiShortcuts.isHome(isWii, keyCode)) return true
        PadKeymap.buttonFor(keyCode)?.let { inputs.menuChord(port, it, down = false) }
        if (isNds) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->   // as on the down
            if (b == NxButton.L2 || b == NxButton.R2) { if (routeTriggerKey(b, down = false)) routeNds(b, down = false); return true }
            if (!routeNds(b, down = false)) return true
        }
        if (gcPad) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b -> if (gcAnalogTriggerKey(b, event.device)) return true }
        if (!isNds && !isDc) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->
            if ((b == NxButton.L2 || b == NxButton.R2) && !routeTriggerKey(b, down = false, port)) return true   // not held
        }
        if (isN3ds) PadKeymap.buttonFor(positionOf(event, keyCode))?.let { b ->
            if (!routeN3ds(b, down = false)) return true
        }
        // As printed (or by position on positional consoles) through the console's pad map; N64 by name ([runnerKey]).
        runnerKey(event, keyCode, port)?.let { retroView?.sendKeyEvent(event.action, it, port) }
        return true
    }
}

/** The buttons a release lets go of in the core ([GameActivity.releaseAllGameInput], [GameActivity.releasePort]). */
private val RELEASED_BUTTONS = listOf(
    NxButton.A, NxButton.B, NxButton.X, NxButton.Y, NxButton.L1, NxButton.L2,
    NxButton.R1, NxButton.R2, NxButton.START, NxButton.SELECT, NxButton.MENU,
)

/**
 * The thumbstick clicks (L3, R3) a release also lets go of: no [NxButton] has them. A pad's clicks reach the core as these
 * same keycodes on every console (no pad map or face swap touches them), so the release matches what was sent.
 */
private val RELEASED_THUMB_KEYS = listOf(KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR)

/**
 * One axis of a gamepad's left stick as a d-pad direction (-1, 0, 1), given the direction [held] from the last event:
 * pressed past [press], kept until it falls back under [release] (the gap keeps a stick resting near the line from
 * chattering). Pressed straight across, it flips without a release in between.
 */
internal fun stickDpadStep(value: Float, held: Int, press: Float = 0.5f, release: Float = 0.4f): Int = when {
    value >= press -> 1
    value <= -press -> -1
    held > 0 && value >= release -> 1
    held < 0 && value <= -release -> -1
    else -> 0
}

/** One d-pad axis from a hat and the stick's step: a pressed hat wins, else the stick. */
internal fun hatOrStick(hat: Float, stickStep: Int): Int = when {
    hat >= 0.5f -> 1
    hat <= -0.5f -> -1
    else -> stickStep
}

/**
 * What a real-path cache copy was made from ([GameActivity.copyToCache]): the source's size and, when its provider
 * reports one, its last-modified. Without a last-modified it is size only.
 */
internal fun realPathCopyStamp(size: Long, lastModified: Long?): String = "$size:${lastModified ?: "?"}"

/**
 * `<file name><suffix>`, or `<hash of the name><suffix>` when that (plus writeAtomically's `.xxxxxxxx.tmp`) would pass
 * the 255-byte file-name limit: a long ROM name must not fail its copy over a sibling's name.
 */
internal fun cacheSiblingName(fileName: String, suffix: String): String =
    if ((fileName + suffix).encodeToByteArray().size + 14 <= 255) fileName + suffix
    else fileName.hashCode().toUInt().toString(16) + suffix

/** A cache copy is reused only when the source's size is known, the copy has it, and its stored stamp is [stamp]. */
internal fun reuseRealPathCopy(copyExists: Boolean, copyLength: Long, size: Long, storedStamp: String?, stamp: String): Boolean =
    copyExists && size >= 0 && copyLength == size && storedStamp?.trim() == stamp

/**
 * DS touch mode's way back to the pad: a small chip in [corner] (clear of the touchable screen), clear of a cutout on that
 * corner's sides. [onSize]: the chip's own size, without its margin.
 */
@Composable
private fun ControlsChip(alpha: Float, corner: NdsTouchMode.ChipCorner, onSize: (IntSize) -> Unit, onClick: () -> Unit) {
    val sides = (if (corner.end) WindowInsetsSides.Right else WindowInsetsSides.Left) +
        (if (corner.top) WindowInsetsSides.Top else WindowInsetsSides.Bottom)
    Box(
        Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(sides)).padding(12.dp).alpha(alpha).onSizeChanged(onSize)
            .background(Color(0xCC000000), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("Controls", color = LocalNxColors.current.main, fontSize = 14.sp)
    }
}

/**
 * The DS / 3DS portrait pad's hide / show icon in the bottom-right corner: a long press flips the band; a tap does
 * nothing, so a thumb landing on it mid-game changes nothing. The chevron points where the band goes: down to hide it,
 * up (over the game, [hidden]) to bring it back.
 */
@Composable
private fun PortraitPadToggle(hidden: Boolean, alpha: Float, onLongPress: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val action by rememberUpdatedState(onLongPress)
    val ink = LocalNxColors.current.main
    Box(
        Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Right + WindowInsetsSides.Bottom))
            .padding(6.dp).size(36.dp).alpha(alpha)
            .background(Color(0x66000000), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    action()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 14.dp, height = 8.dp)) {
            val (tip, ends) = if (hidden) 0f to size.height else size.height to 0f
            val path = Path().apply {
                moveTo(0f, ends); lineTo(size.width / 2f, tip); lineTo(size.width, ends)
            }
            drawPath(path, ink, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** The FrameLayout gravity for a chip corner (END is the right edge, as in [NdsTouchMode.chipCorner]). */
private fun NdsTouchMode.ChipCorner.gravity(): Int =
    (if (top) Gravity.TOP else Gravity.BOTTOM) or (if (end) Gravity.RIGHT else Gravity.LEFT)

/** Keeps a landscape pad cluster clear of a camera cutout on its own screen edge ([side]). */
@Composable
private fun Modifier.clearOfCutout(side: WindowInsetsSides): Modifier =
    windowInsetsPadding(WindowInsets.displayCutout.only(side))

/**
 * Keeps an over-the-game cluster clear of a camera cutout on EITHER edge: pads its own screen edge ([side],
 * Start or End) by [symmetricCutoutInset] of the cutout's left and right insets, so both clusters sit the same
 * distance from their edges and their half-width boxes stay mirrored.
 */
@Composable
private fun Modifier.clearOfCutoutBothSides(side: WindowInsetsSides): Modifier {
    val density = LocalDensity.current
    val dir = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val inset = with(density) {
        symmetricCutoutInset(cutout.getLeft(density, dir).toFloat(), cutout.getRight(density, dir).toFloat()).toDp()
    }
    return if (side == WindowInsetsSides.Start) padding(start = inset) else padding(end = inset)
}

/** An [NdsRegion] in the fork's terms: dst as 0..1 of the [w] x [h] game area (the view's whole viewport). */
private fun NdsRegion.toScreenRegion(w: Float, h: Float) = ScreenRegion(
    src = ScreenRect(src.left, src.top, src.right, src.bottom),
    dst = ScreenRect(dst.x / w, dst.y / h, (dst.x + dst.width) / w, (dst.y + dst.height) / h),
    shaded = shaded,
    touch = touch,
    alpha = alpha,
)
