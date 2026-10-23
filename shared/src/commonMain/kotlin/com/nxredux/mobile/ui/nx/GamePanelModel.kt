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
 * One shortcut button: [enabled] false is drawn dimmed and does nothing; [value] is the display text under the label
 * ("2/3" for discs); [on] makes it a toggle (the second screen's setting; null for the action shortcuts), drawn as a
 * quick-settings tile: filled while on, outlined while off, with no On/Off words.
 */
data class ShortcutState(val shortcut: Shortcut, val label: String, val enabled: Boolean, val value: String? = null, val on: Boolean? = null)

/**
 * The header on every tab: the game's [title], its box art ([artPath]; null draws the [consoleTag]'s logo), its total
 * play time before this launch ([totalMs], read once as the game starts; no running count), the (unlocked, total)
 * RetroAchievements count ([ra] null: no set, no count), and the host's [clockText] and [batteryPercent] (null:
 * unknown), since the game hides the status bar.
 */
data class GamePanelHeader(
    val title: String,
    val artPath: String?,
    val consoleTag: String,
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
 * and [playerPads] (the Players page's types: the slots, and the pads a slot can be set to). [focus]: where the controller is (null: not in the panel). [showGamePill]:
 * the DS/3DS panel-up Game pill (bottom-right), which sends [GamePanelEvent.ShowGame].
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
    /** Put pad [key] in player [slot] (0-based); null empties it. */
    data class SetPlayer(val slot: Int, val key: String?) : GamePanelEvent
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

    /** The points badge at the end of an achievement's name; none for one worth no points. */
    fun pointsBadge(a: RaAchievement): String? = a.points.takeIf { it > 0 }?.toString()

    /** [current] while it is still shown; Shortcuts once it has gone (RA signed out, a pad unplugged). */
    fun resolveTab(current: GamePanelTab, tabs: List<GamePanelTab>): GamePanelTab =
        if (current in tabs) current else GamePanelTab.SHORTCUTS

    /**
     * The Shortcuts tab: Swap disc for a multi-disc game outside netplay (disc swaps aren't synced), Second screen while
     * one is connected, then Fast forward, Rewind, Screenshot and Reset, disabled until they exist. [discIndex] is 0-based.
     */
    fun shortcuts(discCount: Int, discIndex: Int, secondConnected: Boolean, secondOn: Boolean, netplay: Boolean): List<ShortcutState> = buildList {
        if (discCount > 1 && !netplay) add(ShortcutState(Shortcut.SWAP_DISC, Shortcut.SWAP_DISC.label, true, "${discIndex + 1}/$discCount"))
        if (secondConnected) add(ShortcutState(Shortcut.SECOND_SCREEN, Shortcut.SECOND_SCREEN.label, true, on = secondOn))
        for (s in FUTURE) add(ShortcutState(s, s.label, enabled = false))
    }

    /** What activating [s] asks for: nothing while disabled; Swap disc, or the second screen flipped. */
    fun eventFor(s: ShortcutState): GamePanelEvent? = if (!s.enabled) null else when (s.shortcut) {
        Shortcut.SWAP_DISC -> GamePanelEvent.SwapDisc
        Shortcut.SECOND_SCREEN -> s.on?.let { GamePanelEvent.SetSecondScreen(on = !it) }
        Shortcut.FAST_FORWARD, Shortcut.REWIND, Shortcut.SCREENSHOT, Shortcut.RESET -> null
    }

    /**
     * Player [slot]'s value [delta] steps on (wrapping) among Empty and every connected pad, benched ones included (picking
     * one puts it back in play); a held slot's pad, not among them, steps from Empty.
     */
    fun cyclePlayer(slot: Int, delta: Int, players: List<PlayerSlotRow>, pads: List<PlayerPadRow>): String? {
        val candidates = listOf<String?>(null) + pads.map { it.key }
        val at = candidates.indexOf(players.getOrNull(slot)?.key).coerceAtLeast(0)
        return candidates[(at + delta).mod(candidates.size)]
    }

    /** A tap on player [slot]: its next value ([cyclePlayer]), as RIGHT on the Players page steps it. */
    fun nextPlayer(slot: Int, players: List<PlayerSlotRow>, pads: List<PlayerPadRow>): GamePanelEvent.SetPlayer =
        GamePanelEvent.SetPlayer(slot, cyclePlayer(slot, 1, players, pads))

    /** A player slot's value: "Empty", the name, or "<name> (disconnected)"; the panel and the Players page both show it. */
    fun playerValue(s: PlayerSlotRow): String = when {
        s.key == null -> "Empty"
        s.connected -> s.name
        else -> "${s.name} (disconnected)"
    }

    /** The disc after [index] (0-based) of [count], wrapping, as each Swap disc tap steps it. */
    fun nextDisc(index: Int, count: Int): Int = if (count <= 0) 0 else (index + 1).mod(count)

    /** The toast a Swap disc tap shows: "Disc 2 of 3" ([index] 0-based). */
    fun discToast(index: Int, count: Int): String = "Disc ${index + 1} of $count"

    /** The header's achievement line, "Achievements 3 / 40"; none without a set (no count, or a total of 0). */
    fun raLine(h: GamePanelHeader): String? = h.ra?.takeIf { it.second > 0 }?.let { (unlocked, total) -> "Achievements $unlocked / $total" }

    /** The header's play line: "Total 4h 12m". */
    fun timeLine(h: GamePanelHeader): String = "Total ${GameTimeFormat.duration(h.totalMs)}"

    private val FUTURE = listOf(Shortcut.FAST_FORWARD, Shortcut.REWIND, Shortcut.SCREENSHOT, Shortcut.RESET)
}
