package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.gametime.GameTimeFormat

/** The game panel's tabs, in display order; [GamePanelModel.tabs] hides the ones that don't apply. */
enum class GamePanelTab(val label: String) { SHORTCUTS("Shortcuts"), ACHIEVEMENTS("Achievements"), PLAYERS("Players") }

/** The Shortcuts tab's one-tap actions, in display order. The last four are drawn disabled until they exist. */
enum class Shortcut(val label: String) {
    SWAP_DISC("Swap disc"), SECOND_SCREEN("Second screen"), FAST_FORWARD("Fast forward"), REWIND("Rewind"),
    SCREENSHOT("Screenshot"), RESET("Reset"),
}

/**
 * One shortcut button: [enabled] false is drawn dimmed and does nothing; [value] is the display text, "2/3" for discs,
 * "On"/"Off" for the second screen; [on] is the second screen's setting (null for the other shortcuts).
 */
data class ShortcutState(val shortcut: Shortcut, val label: String, val enabled: Boolean, val value: String? = null, val on: Boolean? = null)

/**
 * The header on every tab: the game's [title], its box art ([artPath]; null draws the [consoleTag]'s logo), play time
 * today and in total, the (unlocked, total) RetroAchievements count ([ra] null: no set, no count), and the host's
 * [clockText] and [batteryPercent] (null: unknown), since the game hides the status bar.
 */
data class GamePanelHeader(
    val title: String,
    val artPath: String?,
    val consoleTag: String,
    val todayMs: Long,
    val totalMs: Long,
    val ra: Pair<Int, Int>?,
    val clockText: String,
    val batteryPercent: Int?,
)

/** Where a controller is inside the panel (single-screen in-game menu): the tab row, or the current tab's item [index]. */
sealed interface PanelFocus {
    data object Tabs : PanelFocus
    data class Item(val index: Int) : PanelFocus
}

/**
 * What the game panel shows: the [header], the visible [tabs] and the current [tab], the Shortcuts tab's [shortcuts],
 * this game's [achievements] with the downloaded [badges] (file name → image model), and the Players tab's [players]
 * and [playerPads] (the Players page's types). [touchOnly]: the dual-screen panel, with achievement descriptions in
 * the rows and no controller focus. [focus]: where the controller is (null: not in the panel). [showGamePill]: the
 * DS/3DS panel-up Game pill (bottom-right), which sends [GamePanelEvent.ShowGame].
 */
data class GamePanelState(
    val header: GamePanelHeader,
    val tabs: List<GamePanelTab>,
    val tab: GamePanelTab,
    val shortcuts: List<ShortcutState>,
    val achievements: List<RaAchievement>,
    val badges: Map<String, String>,
    val players: List<PlayerSlotRow>,
    val playerPads: List<PlayerPadRow>,
    val touchOnly: Boolean,
    val focus: PanelFocus? = null,
    val showGamePill: Boolean = false,
)

/** What a tap (or A) on the panel asks the game for. */
sealed interface GamePanelEvent {
    /** Next disc, as the menu's Disc row cycles. */
    data object SwapDisc : GamePanelEvent
    /** The global Second screen setting to [on]. */
    data class SetSecondScreen(val on: Boolean) : GamePanelEvent
    data class SelectTab(val tab: GamePanelTab) : GamePanelEvent
    /** Achievement [id]'s detail (the in-game menu only; the dual-screen panel ignores it). */
    data class OpenAchievement(val id: Int) : GamePanelEvent
    /** Put pad [key] in player [slot] (0-based); null empties it. */
    data class SetPlayer(val slot: Int, val key: String?) : GamePanelEvent
    /** Pad [key] plays (or is ignored in multiplayer games), as the Players page's pad rows flip it. */
    data class SetPlaying(val key: String, val playing: Boolean) : GamePanelEvent
    /** The Game pill: the DS/3DS screen comes back to the second display. */
    data object ShowGame : GamePanelEvent
}

/** The panel's rules, pure so the in-game menu and both hosts share them. */
object GamePanelModel {
    /** Shortcuts always; Achievements only with a RetroAchievements set; Players only with more than one player slot. */
    fun tabs(raAvailable: Boolean, playerSlots: Int): List<GamePanelTab> = buildList {
        add(GamePanelTab.SHORTCUTS)
        if (raAvailable) add(GamePanelTab.ACHIEVEMENTS)
        if (playerSlots > 1) add(GamePanelTab.PLAYERS)
    }

    /** [current] while it is still shown; Shortcuts once it has gone (RA signed out, a pad unplugged). */
    fun resolveTab(current: GamePanelTab, tabs: List<GamePanelTab>): GamePanelTab =
        if (current in tabs) current else GamePanelTab.SHORTCUTS

    /**
     * The Shortcuts tab: Swap disc for a multi-disc game outside netplay (disc swaps aren't synced), Second screen while
     * one is connected, then Fast forward, Rewind, Screenshot and Reset, disabled until they exist. [discIndex] is 0-based.
     */
    fun shortcuts(discCount: Int, discIndex: Int, secondConnected: Boolean, secondOn: Boolean, netplay: Boolean): List<ShortcutState> = buildList {
        if (discCount > 1 && !netplay) add(ShortcutState(Shortcut.SWAP_DISC, Shortcut.SWAP_DISC.label, true, "${discIndex + 1}/$discCount"))
        if (secondConnected) add(ShortcutState(Shortcut.SECOND_SCREEN, Shortcut.SECOND_SCREEN.label, true, if (secondOn) "On" else "Off", on = secondOn))
        for (s in FUTURE) add(ShortcutState(s, s.label, enabled = false))
    }

    /** What activating [s] asks for: nothing while disabled; Swap disc, or the second screen flipped. */
    fun eventFor(s: ShortcutState): GamePanelEvent? = if (!s.enabled) null else when (s.shortcut) {
        Shortcut.SWAP_DISC -> GamePanelEvent.SwapDisc
        Shortcut.SECOND_SCREEN -> s.on?.let { GamePanelEvent.SetSecondScreen(on = !it) }
        Shortcut.FAST_FORWARD, Shortcut.REWIND, Shortcut.SCREENSHOT, Shortcut.RESET -> null
    }

    /** A tap on player [slot]: its next pad among Empty and the playing pads, as the Players page steps it. */
    fun nextPlayer(slot: Int, players: List<PlayerSlotRow>, pads: List<PlayerPadRow>): GamePanelEvent.SetPlayer {
        val candidates = listOf<String?>(null) + pads.filter { it.playing }.map { it.key }
        val at = candidates.indexOf(players.getOrNull(slot)?.key).coerceAtLeast(0)
        return GamePanelEvent.SetPlayer(slot, candidates[(at + 1) % candidates.size])
    }

    /** A tap on pad [pad]'s row: it plays or stops playing, as the Players page flips it. */
    fun togglePlaying(pad: PlayerPadRow): GamePanelEvent.SetPlaying = GamePanelEvent.SetPlaying(pad.key, !pad.playing)

    /** A player slot's value, worded as the Players page words it. */
    fun playerValue(s: PlayerSlotRow): String = when {
        s.key == null -> "Empty"
        s.connected -> s.name
        else -> "${s.name} (disconnected)"
    }

    /** The header's play line: "Today 23m 4s · Total 4h 12m". */
    fun timeLine(h: GamePanelHeader): String =
        "Today ${GameTimeFormat.duration(h.todayMs)} · Total ${GameTimeFormat.duration(h.totalMs)}"

    private val FUTURE = listOf(Shortcut.FAST_FORWARD, Shortcut.REWIND, Shortcut.SCREENSHOT, Shortcut.RESET)
}
