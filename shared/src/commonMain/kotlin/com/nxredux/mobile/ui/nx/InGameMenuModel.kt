package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.achievements.RaAchievementState
import com.nxredux.mobile.achievements.RaBadgeFolders
import com.nxredux.mobile.achievements.RaSortOrder
import com.nxredux.mobile.achievements.sortedBy
import com.nxredux.mobile.cheats.CheatEntry
import com.nxredux.mobile.netplay.NetplayText
import com.nxredux.mobile.options.ConfigSource
import com.nxredux.mobile.options.BottomScreenOptions
import com.nxredux.mobile.options.ConsoleSettings
import com.nxredux.mobile.options.CoreOption
import com.nxredux.mobile.options.CoreOptionFilter
import com.nxredux.mobile.options.FrontendOption
import com.nxredux.mobile.options.FrontendOptions
import com.nxredux.mobile.options.ShaderOptions
import com.nxredux.mobile.shaders.ShaderParam

enum class MenuAction { CONTINUE, QUIT, SAVE_AND_QUIT, QUIT_ONLY, LEAVE_NETPLAY, SAVE_FOR_CONSOLE, SAVE_FOR_GAME, RESTORE_DEFAULTS, RESET_SHADER_PARAMS }
enum class SlotAction { SAVE, LOAD }

sealed interface PageKind {
    data object Root : PageKind
    data object Options : PageKind
    data object Frontend : PageKind
    /** App-owned rows of this console ([ConsoleSettings]); edited like Frontend rows. */
    data object ConsoleSettings : PageKind
    data object Shaders : PageKind
    data object ShaderParams : PageKind
    /** Live adjust of one visual setting (frontend or shader) over the paused frame (the menu collapses to a strip). */
    data class Adjust(val key: String) : PageKind
    data object CoreOptions : PageKind
    data object Cheats : PageKind
    /** This game's RetroAchievements set, one Option row per achievement. */
    data object Achievements : PageKind
    /** One achievement's detail (no rows); LEFT/RIGHT step through the Achievements page's visible list. */
    data class AchievementDetail(val id: Int) : PageKind
    data class Category(val key: String) : PageKind
    data object SaveChanges : PageKind
    /** Quit's choice: Save & Quit (the resume point) or Quit (the last resume point stays), drawn as a dialog ([menuDialog]). */
    data object QuitGame : PageKind
    data object Message : PageKind
    /** Which pad is which player (multiplayer cores only). */
    data object Players : PageKind
}

sealed interface MenuRow {
    val label: String
    val info: String
    data class Action(override val label: String, val action: MenuAction, override val info: String = "") : MenuRow
    data class Slot(override val label: String, val action: SlotAction, override val info: String = "") : MenuRow
    data class Submenu(override val label: String, val target: PageKind, override val info: String = "") : MenuRow
    /** [visual]: the setting changes the picture, so A enters adjust mode instead of showing its info. */
    data class Option(
        override val label: String,
        val key: String,
        val valueLabel: String,
        override val info: String = "",
        val visual: Boolean = false,
    ) : MenuRow
}

/** A player slot for the Players page ([InGameMenuModel.setPlayers]): [key] null = Empty; [connected] false = held for [name]. */
data class PlayerSlotRow(val key: String?, val name: String, val connected: Boolean)

/** A connected physical pad for the Players page; [name] already numbered when two share one ([com.nxredux.mobile.runner.PadNames]). */
data class PlayerPadRow(val key: String, val name: String, val playing: Boolean)

/** One menu screen. [footer] is the page's static text (status line, or the message of a Message page). */
data class MenuPage(
    val kind: PageKind,
    val title: String,
    val rows: List<MenuRow>,
    val selected: Int = 0,
    val footer: String = "",
    /**
     * Message pages only: UP/DOWN ask the UI to scroll the text one step. [scrollSeq] counts the requests (the UI
     * scrolls once per new value), [scrollDir] is the latest one's direction (-1 up, +1 down).
     */
    val scrollSeq: Int = 0,
    val scrollDir: Int = 0,
)

/** [truncatedInfo]: the last info text the UI reported cut off in the list's info area (see [InGameMenuModel.setInfoTruncated]). */
data class MenuState(val stack: List<MenuPage>, val slotIndex: Int, val truncatedInfo: String? = null) {
    val page: MenuPage get() = stack.last()

    /** The selected row is an option whose description does not fit the info area below the list: X shows it whole. */
    val selectedInfoTruncated: Boolean
        get() = (page.rows.getOrNull(page.selected) as? MenuRow.Option)?.info?.let { it.isNotBlank() && it == truncatedInfo } == true
}

/** What the host must do in response to input; the model never touches the platform or the core. */
sealed interface MenuEffect {
    data object Close : MenuEffect
    /** Quit, writing the auto state (the resume point). */
    data object Quit : MenuEffect
    /** Quit without the auto state: the last resume point stays as it is (the battery save is still written). */
    data object QuitWithoutResume : MenuEffect
    /** netplay: Leave on the "Leave netplay?" root; the host ends the session and quits without an auto state. */
    data object LeaveNetplay : MenuEffect
    data class SaveSlot(val slot: Int) : MenuEffect
    data class LoadSlot(val slot: Int) : MenuEffect
    /** Load the core's options, then call [InGameMenuModel.showCoreOptions]. */
    data object OpenCoreOptions : MenuEffect
    data class SetOption(val key: String, val value: String) : MenuEffect
    /** Apply a frontend (display) setting now; persisted only by Save Changes. */
    data class SetFrontend(val key: String, val value: String) : MenuEffect
    /** Apply a shader setting (preset or parameter) now; persisted only by Save Changes. */
    data class SetShader(val key: String, val value: String) : MenuEffect
    /** Apply several shader settings at once (Reset Parameters). */
    data class SetShaders(val entries: List<Pair<String, String>>) : MenuEffect
    /** Turn one cheat on/off now (the host re-applies the whole enabled set); persisted only by Save Changes > For game. */
    data class SetCheat(val index: Int, val enabled: Boolean) : MenuEffect
    /** Put disc [index] (0-based) in the drive now; not persisted. */
    data class ChangeDisc(val index: Int) : MenuEffect
    /** Turn the second-screen setting [on] or off (already flipped in the model); the host persists it and re-syncs the dual mode. */
    data class SetSecondScreen(val on: Boolean) : MenuEffect
    /** Flip achievement [id]'s notification mute (already flipped in the model); the host persists it. */
    data class ToggleMute(val id: Int) : MenuEffect
    /** Put pad [key] in player [slot] (0-based), swapping as the host's slot model does; null empties it. */
    data class SetPlayer(val slot: Int, val key: String?) : MenuEffect
    /** Pad [key] plays or sits out ("Not playing"); persisted by the host at once. */
    data class SetPlaying(val key: String, val playing: Boolean) : MenuEffect
    data object SaveForConsole : MenuEffect
    data object SaveForGame : MenuEffect
    data object RestoreDefaults : MenuEffect
}

/**
 * The in-game pause menu as a page stack: root (Continue/Save/Load/Options/Quit) → Options → Console Settings (→ Adjust,
 * only for a console with its own rows), Frontend (→ Adjust), Shaders (→ Shader Parameters, → Adjust), Core Options →
 * category, Cheats, and Save Changes. Slots are 0-based internally and 1-based in effects.
 *
 * The host builds a new model each time the menu opens and calls [setFrontend] and [setShaders] right away;
 * [MenuEffect.Close] does not reset the stack.
 */
class InGameMenuModel(private val gameTitle: String, private val slotCount: Int, initialSlot: Int = 0) {

    init {
        require(slotCount > 0) { "slotCount must be positive" }
    }

    /** The core's disc count and current disc (0-based), from [setDiscs]; the Disc row shows when [discCount] > 1. */
    private var discCount = 0
    private var discIndex = 0
    /** From [setSecondScreen]; the root page shows the Second screen row only while a second display is [secondConnected]. */
    private var secondConnected = false
    private var secondOn = true
    /** From [setPlayers]; the root page shows Players when there is more than one slot. */
    private var playerSlots: List<PlayerSlotRow> = emptyList()
    private var playerPads: List<PlayerPadRow> = emptyList()

    var state: MenuState = MenuState(listOf(rootPage()), initialSlot.coerceIn(0, slotCount - 1))
        private set

    private var options: List<CoreOption> = emptyList()
    private var locked: Set<String> = emptySet()
    private var status: ConfigSource = ConfigSource.NONE

    private var frontend: Map<String, String> = FrontendOptions.defaults()
    private var overlays: List<String> = emptyList()
    private var frontendLocked: Set<String> = emptySet()
    private var frontendTag = ""                 // the console's tag: selects the Console Settings rows
    private var overlayAvailable = true          // false while the DS screens are split (the overlay is off)

    private var shaders: Map<String, String> = ShaderOptions.fromConfig(emptyMap())
    private var presets: List<String> = emptyList()
    private var params: List<ShaderParam> = emptyList()

    /**
     * netplay: a Wi-Fi netplay session is running, so the root page is only "Leave netplay?" (Continue / Leave): states,
     * options, cheats and the rest would desync the other device. Set before the menu shows, then [refresh].
     */
    var netplay: Boolean = false

    /**
     * netplay: the session is over and the game plays on alone: the root is the plain Continue / Quit (still no states,
     * options, cheats or discs). Quit leaves without an auto state (a netplay launch has no save states), so it is the
     * plain [MenuAction.QUIT_ONLY], with no Save & Quit.
     */
    var netplayEnded: Boolean = false

    /** Rebuilds the root page after a host-side flag ([netplay]) changed. */
    fun refresh() = refreshRoot()

    /** False for a core that ignores cheats: no Cheats row. */
    var cheatsAvailable: Boolean = true

    private var cheats: List<CheatEntry> = emptyList()
    private var cheatsFolder = ""     // "Cheats/<TAG>", for the empty-page footer
    private var cheatsExpected = ""   // the first file name looked for; blank until the host has resolved

    /** Null while the game is not recognised; the Options row shows only once [achievementsAvailable]. */
    private var achievements: List<RaAchievement>? = null
    private var achievementsAvailable = false
    private var achievementSummary: Pair<Int, Int>? = null
    private var achievementOrder = RaSortOrder.DEFAULT
    private var muted: Set<Int> = emptySet()
    private var badges: Map<String, String> = emptyMap()

    /** Y on the Achievements page: hide unlocked (and pending) achievements. */
    var achievementsLockedOnly = false
        private set

    /**
     * The menu is drawn as two panes ([inGameMenuSplit], set by the host's UI): RIGHT on a row that opens a page opens
     * it, as A (a column browser); value rows still change.
     */
    var split: Boolean = false

    /** An [MenuEffect.OpenCoreOptions] was emitted and its [showCoreOptions] has not arrived yet. */
    private var coreOptionsPending = false

    fun press(button: NxButton): MenuEffect? {
        if (state.page.kind == PageKind.Message) {
            when (button) {
                NxButton.A, NxButton.B, NxButton.X -> { pop(); return null }
                NxButton.UP, NxButton.DOWN -> {
                    val p = state.page
                    replaceTop(p.copy(scrollSeq = p.scrollSeq + 1, scrollDir = if (button == NxButton.UP) -1 else 1))
                    return null
                }
                else -> Unit
            }
        }
        if (state.page.kind is PageKind.Adjust) return pressAdjust(button)
        if (state.page.kind == PageKind.Achievements || state.page.kind is PageKind.AchievementDetail) {
            when (button) {
                NxButton.Y -> { achievementsLockedOnly = !achievementsLockedOnly; refreshAchievementsPage(); return null }
                NxButton.X -> selectedAchievementId()?.let { return toggleMute(it) }
                else -> Unit
            }
        } else if (button == NxButton.X) {
            // A button press on Quit (a pad's, the on-screen pad's) picks the quit itself: X leaves the resume point as it is.
            if (selectedAction() == MenuAction.QUIT) return MenuEffect.QuitWithoutResume
            // X shows an option's full description when the info area cuts it off, also on visual rows where A
            // adjusts. Achievements keep X = Mute above.
            val row = state.page.rows.getOrNull(state.page.selected)
            if (row is MenuRow.Option && state.selectedInfoTruncated) push(message(row.label, row.info))
            return null
        }
        return when (button) {
            NxButton.UP -> { move(-1); null }
            NxButton.DOWN -> { move(1); null }
            NxButton.LEFT -> change(-1)
            NxButton.RIGHT -> if (split && opensPage(state.page.rows.getOrNull(state.page.selected))) activate(state.page.selected) else change(1)
            // A on Quit saves and quits; a touch tap ([activate]) asks how to quit instead.
            NxButton.A -> if (selectedAction() == MenuAction.QUIT) MenuEffect.Quit else activate(state.page.selected)
            NxButton.B -> back()
            NxButton.MENU -> MenuEffect.Close
            else -> null
        }
    }

    /**
     * The UI laid out [info] in the info area below the list and it did ([truncated]) or did not fit. Keyed by the
     * text, so a report for a row that is no longer selected never counts for the new one.
     */
    fun setInfoTruncated(info: String, truncated: Boolean) {
        val next = if (truncated) info else state.truncatedInfo.takeIf { it != info }
        if (next != state.truncatedInfo) state = state.copy(truncatedInfo = next)
    }

    fun select(index: Int) {
        if (index in state.page.rows.indices) replaceTop(state.page.copy(selected = index))
    }

    fun activate(index: Int): MenuEffect? {
        if (state.page.kind is PageKind.Adjust) return null
        val row = state.page.rows.getOrNull(index) ?: return null
        select(index)
        return when (row) {
            is MenuRow.Action -> actionEffect(row.action)
            is MenuRow.Slot -> if (row.action == SlotAction.SAVE) MenuEffect.SaveSlot(state.slotIndex + 1) else MenuEffect.LoadSlot(state.slotIndex + 1)
            is MenuRow.Submenu -> openSubmenu(row.target)
            is MenuRow.Option -> {
                val achievement = if (state.page.kind == PageKind.Achievements) achievementFor(row.key) else null
                if (row.key == SECOND_SCREEN_KEY) return flipSecondScreen()   // a root row only
                if (achievement != null) push(achievementDetailPage(achievement))
                else if (row.visual) push(adjustPage(row.key))
                else if (row.info.isNotBlank()) push(message(row.label, row.info))
                null
            }
        }
    }

    /** The action of the top page's highlighted row, or null for another row. */
    private fun selectedAction(): MenuAction? = (state.page.rows.getOrNull(state.page.selected) as? MenuRow.Action)?.action

    /** [row] of the top page opens a page of rows: a submenu, or an achievement (its detail). */
    private fun opensPage(row: MenuRow?): Boolean = when (row) {
        is MenuRow.Submenu -> true
        is MenuRow.Option -> state.page.kind == PageKind.Achievements && achievementFor(row.key) != null
        else -> false
    }

    /**
     * The page [row] would open, built without opening it, for the two-pane root's preview ([menuPanes]); null for a
     * row that opens none, or whose page is not ready without the host (Core Options loads from the core).
     */
    fun previewFor(row: MenuRow): MenuPage? = when (val target = (row as? MenuRow.Submenu)?.target) {
        PageKind.Options -> optionsPage()
        PageKind.ConsoleSettings -> consoleSettingsPage()
        PageKind.Frontend -> frontendPage()
        PageKind.Shaders -> shadersPage()
        PageKind.ShaderParams -> shaderParamsPage()
        PageKind.Cheats -> cheatsPage()
        PageKind.Achievements -> if (achievements.isNullOrEmpty()) null else achievementsPage()
        PageKind.SaveChanges -> saveChangesPage()
        PageKind.Players -> playersPage()
        is PageKind.Category -> categoryPage(target.key)
        else -> null
    }

    /**
     * A tap on row [index] of the dimmed left pane, the page of kind [parent] under the top one ([menuPanes]): that level
     * comes back, as B would, on [index]; a row that opens a page opens it in place of the old one, as A there would. The
     * row already open, or a [parent] no longer under the top page, does nothing: a one-tap list ([nxTap]) calls this
     * twice, and the second call finds the row open (or the level focused).
     */
    fun tapParent(parent: PageKind, index: Int): MenuEffect? {
        val stack = state.stack
        if (stack.size < 2 || stack[stack.size - 2].kind != parent || state.page.kind is PageKind.Adjust) return null
        if (index == stack[stack.size - 2].selected || index !in stack[stack.size - 2].rows.indices) return null
        pop()
        select(index)
        return if (opensPage(state.page.rows[index])) activate(index) else null
    }

    /**
     * A tap on row [index] of the root's preview pane ([menuPanes]): opens the highlighted row's page on that row. Only
     * while the root is the top page, so the second call of a one-tap list does nothing.
     */
    fun tapPreview(index: Int): MenuEffect? {
        if (state.stack.size != 1) return null
        val row = state.page.rows.getOrNull(state.page.selected) ?: return null
        if (previewFor(row) == null) return null
        val effect = activate(state.page.selected)
        if (state.stack.size > 1) select(index)
        return effect
    }

    /**
     * Called by the host after [MenuEffect.OpenCoreOptions] with every option the core declared. Ignored when
     * nothing is pending or the user has left the Options page meanwhile (the host loads asynchronously).
     */
    fun showCoreOptions(all: List<CoreOption>, locked: Set<String>) {
        val pending = coreOptionsPending
        coreOptionsPending = false
        if (!pending || state.page.kind != PageKind.Options) return
        options = all
        this.locked = locked
        val page = coreOptionsPage()
        push(if (page.rows.isEmpty()) message("Core Options", NO_LIVE_OPTIONS) else page)
    }

    /** Refreshes option pages after values or visibility changed, keeping selections in range. */
    fun updateOptions(all: List<CoreOption>) {
        options = all
        state = state.copy(stack = state.stack.map { p ->
            when (val k = p.kind) {
                PageKind.CoreOptions -> rebuilt(p, coreOptionsPage())
                is PageKind.Category -> rebuilt(p, categoryPage(k.key))
                else -> p
            }
        })
    }

    /**
     * The frontend values in use (missing keys take their defaults for [overlays] and [tag]), the overlay file names
     * offered for the Overlay setting, the config's locked keys (locked frontend settings are hidden), the console's
     * [tag] (it selects the console's own rows, shown on Options → Console Settings), and whether overlays can show
     * ([overlayAvailable] is false while the DS screens are split). Refreshes open Console Settings/Frontend/Adjust pages.
     */
    fun setFrontend(values: Map<String, String>, overlays: List<String>, locked: Set<String>, tag: String = "", overlayAvailable: Boolean = true) {
        frontendTag = tag
        this.overlayAvailable = overlayAvailable
        frontend = FrontendOptions.fromConfig(values, tag)
        this.overlays = overlays
        frontendLocked = locked
        refreshFrontendPages()
    }

    /**
     * The shader values in use (preset + stored parameters), the presets offered, and the active preset's
     * parameters. Refreshes open Shaders/ShaderParams/Adjust pages.
     */
    fun setShaders(values: Map<String, String>, presets: List<String>, params: List<ShaderParam>) {
        shaders = ShaderOptions.fromConfig(values)
        this.presets = presets
        this.params = params
        refreshFrontendPages()
    }

    /**
     * The cheats resolved for this game, the folder they were looked for in and the first file name tried (both
     * only for the empty-page footer). Refreshes an open Cheats page, keeping its selection.
     */
    fun setCheats(entries: List<CheatEntry>, folder: String, expected: String) {
        cheats = entries
        cheatsFolder = folder
        cheatsExpected = expected
        refreshCheatsPage()
    }

    /**
     * This game's achievements ([list] null: the game is not recognised), the (unlocked, total) [summary] for the
     * Options row, the sort [order], the [muted] ids and the [badges] listing (file name -> image model). Makes the
     * Achievements row visible and refreshes open Options/Achievements pages.
     */
    fun setAchievements(list: List<RaAchievement>?, summary: Pair<Int, Int>?, order: RaSortOrder, muted: Set<Int>, badges: Map<String, String>) {
        achievements = list
        achievementSummary = summary
        achievementOrder = order
        this.muted = muted
        this.badges = badges
        achievementsAvailable = true
        refreshOptionsPage()
        refreshAchievementsPage()
    }

    /** Shows or hides the Achievements row (hidden when RetroAchievements is off or signed out). */
    fun setAchievementsAvailable(available: Boolean) {
        achievementsAvailable = available
        refreshOptionsPage()
    }

    /** The achievement shown by the top AchievementDetail page, or null. */
    fun achievementDetail(): RaAchievement? =
        (state.page.kind as? PageKind.AchievementDetail)?.let { k -> achievements?.firstOrNull { it.id == k.id } }

    /** The badge image model for [a] (the greyed `_lock` variant while locked); null until downloaded. */
    fun badgePath(a: RaAchievement): String? =
        RaBadgeFolders.fileName(a.badge, a.state == RaAchievementState.LOCKED)?.let(badges::get)

    fun isMuted(id: Int): Boolean = id in muted

    fun setStatus(source: ConfigSource) {
        status = source
        state = state.copy(stack = state.stack.map {
            if (it.kind == PageKind.Options || it.kind == PageKind.SaveChanges) it.copy(footer = statusText()) else it
        })
    }

    fun showMessage(title: String, text: String) = push(message(title, text))

    /**
     * The disc state the core reports (disk-control interface): with more than one disc the root page shows "Disc N/M"
     * under Continue. Called by the host after the menu opens; keeps the selected root row.
     */
    fun setDiscs(current: Int, count: Int) {
        discCount = count.coerceAtLeast(0)
        discIndex = current.coerceIn(0, (discCount - 1).coerceAtLeast(0))
        refreshRoot()
    }

    /**
     * A second display is [connected] and the setting is [on]; shows (or hides) the root's Second screen row and Console
     * Settings' Bottom screen row, keeping the selection.
     */
    fun setSecondScreen(connected: Boolean, on: Boolean) {
        val changed = connected != secondConnected
        secondConnected = connected
        secondOn = on
        refreshRoot()
        if (!changed) return
        // Console Settings may come or go with its Bottom screen row (a console whose other rows are all locked).
        state = state.copy(stack = state.stack.map { p -> if (p.kind == PageKind.Options) rebuilt(p, optionsPage()) else p })
        refreshFrontendPages()
    }

    private fun flipSecondScreen(): MenuEffect {
        secondOn = !secondOn
        refreshRoot()
        return MenuEffect.SetSecondScreen(secondOn)
    }

    private fun changeDisc(delta: Int): MenuEffect? {
        if (discCount < 2) return null
        discIndex = ((discIndex + delta) % discCount + discCount) % discCount
        refreshRoot()
        return MenuEffect.ChangeDisc(discIndex)
    }

    /**
     * The host's player slots and connected pads. Called when the menu opens and after every [MenuEffect.SetPlayer] /
     * [MenuEffect.SetPlaying] the host applied; refreshes the root and an open Players page, keeping their rows.
     */
    fun setPlayers(slots: List<PlayerSlotRow>, pads: List<PlayerPadRow>) {
        playerSlots = slots
        playerPads = pads
        refreshRoot()
        state = state.copy(stack = state.stack.map { p -> if (p.kind == PageKind.Players) rebuilt(p, playersPage()).copy(footer = PLAYERS_FOOTER) else p })
    }

    private fun playersPage() = MenuPage(
        PageKind.Players, PLAYERS_LABEL,
        playerSlots.mapIndexed { i, s ->
            val value = when {
                s.key == null -> EMPTY
                s.connected -> s.name
                else -> "${s.name} (disconnected)"
            }
            MenuRow.Option("Player ${i + 1}", "$PLAYER_KEY_PREFIX$i", value)
        } + playerPads.map { MenuRow.Option(it.name, "$PAD_KEY_PREFIX${it.key}", if (it.playing) PLAYING else NOT_PLAYING, PAD_INFO) },
        footer = PLAYERS_FOOTER,
    )

    private fun changePlayer(slot: Int, delta: Int): MenuEffect? {
        val candidates = listOf<String?>(null) + playerPads.filter { it.playing }.map { it.key }
        val at = candidates.indexOf(playerSlots.getOrNull(slot)?.key).coerceAtLeast(0)
        val next = candidates[((at + delta) % candidates.size + candidates.size) % candidates.size]
        return MenuEffect.SetPlayer(slot, next)
    }

    private fun togglePlaying(key: String): MenuEffect? =
        playerPads.firstOrNull { it.key == key }?.let { MenuEffect.SetPlaying(key, !it.playing) }

    /** Rebuilds the root page, keeping the selection on the row with the same label (root labels are unique). */
    private fun refreshRoot() {
        state = state.copy(stack = state.stack.map { p ->
            if (p.kind != PageKind.Root) p else {
                val fresh = rootPage()
                val label = p.rows.getOrNull(p.selected)?.label
                fresh.copy(selected = fresh.rows.indexOfFirst { it.label == label }.coerceAtLeast(0))
            }
        })
    }

    private fun pressAdjust(button: NxButton): MenuEffect? = when (button) {
        NxButton.UP -> { stepVisual(-1); null }
        NxButton.DOWN -> { stepVisual(1); null }
        NxButton.LEFT -> change(-1)
        NxButton.RIGHT -> change(1)
        NxButton.B -> { pop(); null }
        NxButton.MENU -> MenuEffect.Close
        else -> null
    }

    private fun actionEffect(action: MenuAction): MenuEffect? = when (action) {
        MenuAction.CONTINUE -> MenuEffect.Close
        MenuAction.QUIT -> { push(quitPage()); null }
        MenuAction.SAVE_AND_QUIT -> MenuEffect.Quit
        MenuAction.QUIT_ONLY -> MenuEffect.QuitWithoutResume
        MenuAction.LEAVE_NETPLAY -> MenuEffect.LeaveNetplay
        MenuAction.SAVE_FOR_CONSOLE -> { popTo(PageKind.Options); MenuEffect.SaveForConsole }
        MenuAction.SAVE_FOR_GAME -> { popTo(PageKind.Options); MenuEffect.SaveForGame }
        MenuAction.RESTORE_DEFAULTS -> { popTo(PageKind.Options); MenuEffect.RestoreDefaults }
        MenuAction.RESET_SHADER_PARAMS -> {
            val entries = ShaderOptions.resetEntries(params)
            shaders = shaders + entries
            refreshFrontendPages()
            push(message("Shaders", ShaderOptions.RESET_DONE))
            MenuEffect.SetShaders(entries)
        }
    }

    private fun openSubmenu(target: PageKind): MenuEffect? {
        when (target) {
            PageKind.Options -> push(optionsPage())
            PageKind.ConsoleSettings -> push(consoleSettingsPage())
            PageKind.Frontend -> push(frontendPage())
            PageKind.Shaders -> push(shadersPage())
            PageKind.ShaderParams -> push(shaderParamsPage())
            PageKind.CoreOptions -> {
                if (coreOptionsPending) return null
                coreOptionsPending = true
                return MenuEffect.OpenCoreOptions
            }
            PageKind.Cheats -> push(cheatsPage())
            PageKind.Achievements ->
                if (achievements.isNullOrEmpty()) push(message(ACHIEVEMENTS_LABEL, NO_ACHIEVEMENTS)) else push(achievementsPage())
            PageKind.SaveChanges -> push(saveChangesPage())
            PageKind.Players -> push(playersPage())
            is PageKind.Category -> push(categoryPage(target.key))
            is PageKind.Adjust, is PageKind.AchievementDetail, PageKind.Root, PageKind.Message, PageKind.QuitGame -> Unit
        }
        return null
    }

    private fun back(): MenuEffect? {
        if (state.stack.size == 1) return MenuEffect.Close
        pop()
        return null
    }

    private fun change(delta: Int): MenuEffect? = if (state.page.kind is PageKind.AchievementDetail) {
        stepAchievement(delta); null
    } else when (val row = state.page.rows.getOrNull(state.page.selected)) {
        is MenuRow.Slot -> {
            state = state.copy(slotIndex = ((state.slotIndex + delta) % slotCount + slotCount) % slotCount)
            null
        }
        is MenuRow.Option -> when {
            row.key.startsWith(PLAYER_KEY_PREFIX) -> changePlayer(row.key.removePrefix(PLAYER_KEY_PREFIX).toInt(), delta)
            row.key.startsWith(PAD_KEY_PREFIX) -> togglePlaying(row.key.removePrefix(PAD_KEY_PREFIX))
            row.key == DISC_KEY -> changeDisc(delta)
            row.key == SECOND_SCREEN_KEY -> flipSecondScreen()
            state.page.kind == PageKind.Cheats -> toggleCheat(row.key)
            isSettingsPage(state.page.kind) -> changeFrontend(row.key, delta)
            else -> {
                val option = options.firstOrNull { it.key == row.key }
                val next = option?.let { CoreOptionFilter.cycle(it, delta) }
                if (option == null || next == null) null else {
                    updateOptions(options.map { if (it.key == option.key) it.copy(value = next) else it })
                    MenuEffect.SetOption(option.key, next)
                }
            }
        }
        else -> null
    }

    /** Pages whose Option rows are console, frontend or shader settings (not core options). */
    private fun isSettingsPage(kind: PageKind) =
        kind == PageKind.ConsoleSettings || kind == PageKind.Frontend || kind == PageKind.Shaders || kind == PageKind.ShaderParams || kind is PageKind.Adjust

    private fun changeFrontend(key: String, delta: Int): MenuEffect? {
        if (ShaderOptions.isShaderKey(key)) {
            // An unstored parameter shows its default, so it cycles from there too.
            val current = shaders[key]
                ?: params.firstOrNull { ShaderOptions.paramKey(it.id) == key }?.let { ShaderOptions.format2(it.default) }
                ?: return null
            val next = ShaderOptions.cycle(key, current, delta, presets, params)
            if (next == null || next == current) return null
            shaders = shaders + (key to next)
            refreshFrontendPages()
            return MenuEffect.SetShader(key, next)
        }
        if (key == FrontendOptions.OVERLAY && !overlayAvailable) return null
        val current = frontend[key] ?: return null
        val next = FrontendOptions.cycle(key, current, delta, overlays)
        if (next == null || next == current) return null
        frontend = frontend + (key to next)
        refreshFrontendPages()
        return MenuEffect.SetFrontend(key, next)
    }

    /** Left and Right both flip the row: a cheat has only two values. */
    private fun toggleCheat(key: String): MenuEffect? {
        val index = key.removePrefix(CHEAT_KEY_PREFIX).toIntOrNull() ?: return null
        val entry = cheats.getOrNull(index) ?: return null
        val enabled = !entry.enabled
        cheats = cheats.mapIndexed { i, e -> if (i == index) e.copy(enabled = enabled) else e }
        refreshCheatsPage()
        return MenuEffect.SetCheat(index, enabled)
    }

    private fun refreshCheatsPage() {
        state = state.copy(stack = state.stack.map { p -> if (p.kind == PageKind.Cheats) rebuilt(p, cheatsPage()) else p })
    }

    private fun cheatsPage(): MenuPage {
        val rows = cheats.mapIndexed { i, e ->
            MenuRow.Option(e.desc, CHEAT_KEY_PREFIX + i, if (e.enabled) "On" else "Off", e.info, visual = false)
        }
        return MenuPage(PageKind.Cheats, CHEATS_LABEL, rows, footer = if (rows.isEmpty()) noCheatsText() else "")
    }

    private fun noCheatsText() =
        if (cheatsExpected.isBlank()) NO_CHEATS else "$NO_CHEATS\nLooked for $cheatsFolder/$cheatsExpected"

    private fun refreshOptionsPage() {
        // rebuilt() clears the footer of a non-empty page; the Options page keeps its status line.
        state = state.copy(stack = state.stack.map { p ->
            if (p.kind == PageKind.Options) rebuilt(p, optionsPage()).copy(footer = statusText()) else p
        })
    }

    private fun refreshAchievementsPage() {
        state = state.copy(stack = state.stack.map { p -> if (p.kind == PageKind.Achievements) rebuilt(p, achievementsPage()) else p })
    }

    /** The achievements in the chosen order, locked ones only while [achievementsLockedOnly]. */
    private fun visibleAchievements(): List<RaAchievement> {
        val sorted = achievements.orEmpty().sortedBy(achievementOrder)
        return if (achievementsLockedOnly) sorted.filter { it.state == RaAchievementState.LOCKED } else sorted
    }

    private fun achievementsPage(): MenuPage {
        val rows = visibleAchievements().map { a ->
            val value = when (a.state) {
                RaAchievementState.UNLOCKED -> "Unlocked"
                RaAchievementState.PENDING -> "Pending sync"
                RaAchievementState.LOCKED -> "Locked · ${a.points} pts"
            }
            MenuRow.Option((if (a.id in muted) "[M] " else "") + a.title, "$ACH_KEY_PREFIX${a.id}", value, a.description)
        }
        return MenuPage(PageKind.Achievements, ACHIEVEMENTS_LABEL, rows, footer = if (rows.isEmpty()) NO_LOCKED else "")
    }

    private fun achievementDetailPage(a: RaAchievement) = MenuPage(PageKind.AchievementDetail(a.id), "Achievements", emptyList())   // the name is the page's headline

    private fun achievementFor(key: String): RaAchievement? {
        if (!key.startsWith(ACH_KEY_PREFIX)) return null
        val id = key.removePrefix(ACH_KEY_PREFIX).toIntOrNull() ?: return null
        return achievements?.firstOrNull { it.id == id }
    }

    /** The detail page's achievement, else the selected row's on the Achievements page. */
    private fun selectedAchievementId(): Int? = when (val k = state.page.kind) {
        is PageKind.AchievementDetail -> k.id
        PageKind.Achievements -> (state.page.rows.getOrNull(state.page.selected) as? MenuRow.Option)?.let { achievementFor(it.key)?.id }
        else -> null
    }

    /** Flips the mute locally so the `[M]` marker updates now; the host persists it via the effect. */
    private fun toggleMute(id: Int): MenuEffect {
        muted = if (id in muted) muted - id else muted + id
        refreshAchievementsPage()
        return MenuEffect.ToggleMute(id)
    }

    /** Detail LEFT/RIGHT: the previous/next visible achievement, wrapping; the list beneath follows the selection. */
    private fun stepAchievement(delta: Int) {
        val id = (state.page.kind as? PageKind.AchievementDetail)?.id ?: return
        val visible = visibleAchievements()
        if (visible.isEmpty()) return
        val at = visible.indexOfFirst { it.id == id }
        val next = if (at < 0) visible.first() else visible[((at + delta) % visible.size + visible.size) % visible.size]
        val nextKey = "$ACH_KEY_PREFIX${next.id}"
        val below = state.stack.dropLast(1).map { p ->
            val row = p.rows.indexOfFirst { (it as? MenuRow.Option)?.key == nextKey }
            if (p.kind == PageKind.Achievements && row >= 0) p.copy(selected = row) else p
        }
        state = state.copy(stack = below + achievementDetailPage(next))
    }

    /**
     * Adjust mode UP/DOWN: the previous/next visual row of the page the strip came from (Console Settings, Frontend,
     * Shaders or Shader Parameters), wrapping.
     */
    private fun stepVisual(delta: Int) {
        val key = (state.page.kind as? PageKind.Adjust)?.key ?: return
        val ownerIndex = state.stack.indexOfLast { it.kind !is PageKind.Adjust }
        if (ownerIndex < 0) return
        val owner = state.stack[ownerIndex]
        val visual = owner.rows.indices.filter { (owner.rows[it] as? MenuRow.Option)?.visual == true }
        if (visual.isEmpty()) return
        val at = visual.indexOfFirst { (owner.rows[it] as MenuRow.Option).key == key }.coerceAtLeast(0)
        val target = visual[((at + delta) % visual.size + visual.size) % visual.size]
        val targetKey = (owner.rows[target] as MenuRow.Option).key
        state = state.copy(stack = state.stack.take(ownerIndex) + owner.copy(selected = target) + adjustPage(targetKey))
    }

    /** Rebuilds open Console Settings/Frontend/Shaders/ShaderParams/Adjust pages from the current values. */
    private fun refreshFrontendPages() {
        state = state.copy(stack = state.stack.mapNotNull { p ->
            when (val k = p.kind) {
                PageKind.ConsoleSettings -> rebuilt(p, consoleSettingsPage())
                PageKind.Frontend -> rebuilt(p, frontendPage())
                PageKind.Shaders -> rebuilt(p, shadersPage())
                PageKind.ShaderParams -> rebuilt(p, shaderParamsPage())
                // A parameter the new preset no longer has: close its strip rather than show a stale row.
                is PageKind.Adjust -> if (isVanishedParam(k.key)) null else adjustPage(k.key)
                else -> p
            }
        })
    }

    private fun isVanishedParam(key: String) =
        ShaderOptions.paramId(key) != null && params.none { ShaderOptions.paramKey(it.id) == key }

    private fun move(delta: Int) {
        val p = state.page
        if (p.rows.isEmpty()) return
        replaceTop(p.copy(selected = ((p.selected + delta) % p.rows.size + p.rows.size) % p.rows.size))
    }

    private fun push(page: MenuPage) { state = state.copy(stack = state.stack + page) }
    private fun pop() { if (state.stack.size > 1) state = state.copy(stack = state.stack.dropLast(1)) }
    private fun popTo(kind: PageKind) {
        val i = state.stack.indexOfLast { it.kind == kind }
        if (i >= 0) state = state.copy(stack = state.stack.take(i + 1))
    }
    private fun replaceTop(page: MenuPage) { state = state.copy(stack = state.stack.dropLast(1) + page) }
    /** [new] with [old]'s title, keeping the selected row by identity (option key / submenu target) when it survives. */
    private fun rebuilt(old: MenuPage, new: MenuPage): MenuPage {
        val oldRow = old.rows.getOrNull(old.selected)
        val same = new.rows.indexOfFirst { sameRow(it, oldRow) }
        val selected = if (same >= 0) same else old.selected.coerceIn(0, (new.rows.size - 1).coerceAtLeast(0))
        val footer = if (new.rows.isNotEmpty()) "" else when (new.kind) {
            PageKind.CoreOptions -> NO_LIVE_OPTIONS
            PageKind.ShaderParams -> ShaderOptions.NO_PARAMS
            PageKind.Cheats -> noCheatsText()
            PageKind.Achievements -> NO_LOCKED
            else -> NO_CATEGORY_OPTIONS
        }
        return new.copy(title = old.title, selected = selected, footer = footer)
    }

    private fun sameRow(a: MenuRow, b: MenuRow?): Boolean = when {
        a is MenuRow.Option && b is MenuRow.Option -> a.key == b.key
        a is MenuRow.Submenu && b is MenuRow.Submenu -> a.target == b.target
        else -> false
    }

    private fun rootPage() = if (netplay && netplayEnded) MenuPage(
        PageKind.Root, gameTitle,
        // No save states after netplay, so no resume point to choose about: Quit quits.
        listOf(MenuRow.Action("Continue", MenuAction.CONTINUE), MenuRow.Action("Quit", MenuAction.QUIT_ONLY)),
    ) else if (netplay) MenuPage(
        PageKind.Root, NetplayText.LEAVE_TITLE,
        listOf(MenuRow.Action(NetplayText.CONTINUE, MenuAction.CONTINUE), MenuRow.Action(NetplayText.LEAVE, MenuAction.LEAVE_NETPLAY)),
        footer = NetplayText.LEAVE_TEXT,
    ) else MenuPage(
        PageKind.Root, gameTitle,
        buildList {
            add(MenuRow.Action("Continue", MenuAction.CONTINUE))
            if (discCount > 1) add(MenuRow.Option(DISC_LABEL, DISC_KEY, "${discIndex + 1}/$discCount", DISC_INFO))
            add(MenuRow.Slot("Save", SlotAction.SAVE))
            add(MenuRow.Slot("Load", SlotAction.LOAD))
            if (secondConnected) add(MenuRow.Option(SECOND_SCREEN_LABEL, SECOND_SCREEN_KEY, if (secondOn) "On" else "Off"))
            if (playerSlots.size > 1) add(MenuRow.Submenu(PLAYERS_LABEL, PageKind.Players))
            add(MenuRow.Submenu("Options", PageKind.Options))
            add(MenuRow.Action("Quit", MenuAction.QUIT))
        },
    )

    private fun optionsPage() = MenuPage(
        PageKind.Options, "Options",
        buildList {
            if (consoleRows().isNotEmpty()) add(MenuRow.Submenu(CONSOLE_SETTINGS_LABEL, PageKind.ConsoleSettings))
            add(MenuRow.Submenu("Frontend", PageKind.Frontend))
            add(MenuRow.Submenu("Shaders", PageKind.Shaders))
            add(MenuRow.Submenu("Core Options", PageKind.CoreOptions))
            if (cheatsAvailable) add(MenuRow.Submenu(CHEATS_LABEL, PageKind.Cheats, CHEATS_INFO))
            if (achievementsAvailable) {
                val info = achievementSummary?.let { "${it.first} / ${it.second} unlocked" } ?: NOT_RECOGNISED
                add(MenuRow.Submenu(ACHIEVEMENTS_LABEL, PageKind.Achievements, info))
            }
            add(MenuRow.Submenu("Save Changes", PageKind.SaveChanges))
        },
        footer = statusText(),
    )

    /**
     * The console's own rows ([com.nxredux.mobile.options.ConsoleSettings]: the DS screens, the 3DS and Wii controls, the
     * graphics API, the pad), locked ones hidden; values live with the frontend values.
     */
    private fun consoleSettingsPage() = MenuPage(
        PageKind.ConsoleSettings, CONSOLE_SETTINGS_LABEL,
        consoleRows().map(::frontendRow),
    )

    /**
     * This console's Console Settings rows, locked ones and the ones read only at launch ([FrontendOption.atLaunch]: set in
     * Game Settings / Emulator Settings) left out, and Bottom screen while no second display is [secondConnected]; none
     * left = the page is not offered.
     */
    private fun consoleRows() = ConsoleSettings.tableFor(frontendTag).filter {
        it.key !in frontendLocked && !it.atLaunch && (secondConnected || it.key != BottomScreenOptions.KEY)
    }

    private fun frontendPage() = MenuPage(
        PageKind.Frontend, "Frontend",
        FrontendOptions.table.filter { it.key !in frontendLocked }.map(::frontendRow),
    )

    private fun shadersPage() = MenuPage(
        PageKind.Shaders, "Shaders",
        listOf(
            presetRow(),
            MenuRow.Submenu(ShaderOptions.PARAMS_LABEL, PageKind.ShaderParams, ShaderOptions.PARAMS_INFO),
            MenuRow.Action(ShaderOptions.RESET_LABEL, MenuAction.RESET_SHADER_PARAMS, ShaderOptions.RESET_INFO),
        ),
    )

    private fun shaderParamsPage(): MenuPage {
        val rows = params.map(::paramRow)
        return MenuPage(PageKind.ShaderParams, ShaderOptions.PARAMS_LABEL, rows, footer = if (rows.isEmpty()) ShaderOptions.NO_PARAMS else "")
    }

    private fun presetRow() = MenuRow.Option(
        ShaderOptions.PRESET_LABEL, ShaderOptions.PRESET, shaders[ShaderOptions.PRESET] ?: ShaderOptions.NONE,
        ShaderOptions.PRESET_INFO, visual = true,
    )

    private fun paramRow(p: ShaderParam) = MenuRow.Option(
        p.label, ShaderOptions.paramKey(p.id), shaders[ShaderOptions.paramKey(p.id)] ?: ShaderOptions.format2(p.default),
        p.id, visual = true,
    )

    /** The one row an Adjust page shows for [key]: a frontend setting, the preset, or a parameter. */
    private fun adjustPage(key: String): MenuPage {
        val row = when {
            key == ShaderOptions.PRESET -> presetRow()
            ShaderOptions.paramId(key) != null ->
                paramRow(checkNotNull(params.firstOrNull { ShaderOptions.paramKey(it.id) == key }) { "not an active parameter: $key" })
            else -> frontendRow(checkNotNull(FrontendOptions.option(key)) { "not a frontend key: $key" })
        }
        return MenuPage(PageKind.Adjust(key), row.label, listOf(row))
    }

    private fun frontendRow(o: FrontendOption): MenuRow.Option =
        if (o.key == FrontendOptions.OVERLAY && !overlayAvailable) {
            MenuRow.Option(o.label, o.key, OVERLAY_UNAVAILABLE, OVERLAY_UNAVAILABLE_INFO)
        } else {
            MenuRow.Option(o.label, o.key, o.labelOf(frontend[o.key] ?: o.default), o.info, visual = o.visual)
        }

    private fun saveChangesPage() = MenuPage(
        PageKind.SaveChanges, "Save Changes",
        listOf(
            MenuRow.Action("Save for console", MenuAction.SAVE_FOR_CONSOLE),
            MenuRow.Action("Save for game", MenuAction.SAVE_FOR_GAME),
            MenuRow.Action("Restore defaults", MenuAction.RESTORE_DEFAULTS),
        ),
        footer = statusText(),
    )

    private fun quitPage() = MenuPage(
        PageKind.QuitGame, "Quit Game",
        listOf(
            MenuRow.Action("Save & Quit", MenuAction.SAVE_AND_QUIT),
            MenuRow.Action("Quit", MenuAction.QUIT_ONLY),
        ),
        footer = QUIT_TEXT,
    )

    private fun coreOptionsPage(): MenuPage {
        val live = CoreOptionFilter.live(options, locked)
        val rows = CoreOptionFilter.categories(live).map { MenuRow.Submenu(it.label, PageKind.Category(it.key), it.info) } +
            CoreOptionFilter.uncategorized(live).map { optionRow(it, inCategory = false) }
        return MenuPage(PageKind.CoreOptions, "Core Options", rows)
    }

    private fun categoryPage(key: String): MenuPage {
        val live = CoreOptionFilter.live(options, locked)
        val title = CoreOptionFilter.categories(live).firstOrNull { it.key == key }?.label ?: key
        return MenuPage(PageKind.Category(key), title, CoreOptionFilter.inCategory(live, key).map { optionRow(it, inCategory = true) })
    }

    private fun optionRow(o: CoreOption, inCategory: Boolean) =
        MenuRow.Option(CoreOptionFilter.rowLabel(o, inCategory), o.key, CoreOptionFilter.valueLabel(o), o.info)

    private fun message(title: String, text: String) = MenuPage(PageKind.Message, title, emptyList(), footer = text)

    private fun statusText() = when (status) {
        ConfigSource.NONE -> "Using defaults."
        ConfigSource.CONSOLE -> "Using console config."
        ConfigSource.GAME -> "Using game config."
    }

    companion object {
        const val CONSOLE_SETTINGS_LABEL = "Console Settings"
        const val DISC_KEY = "nx:disc"
        const val DISC_LABEL = "Disc"
        const val SECOND_SCREEN_KEY = "nx:second_screen"   // a menu row, not a frontend (nx_) option
        const val SECOND_SCREEN_LABEL = "Second screen"
        const val DISC_INFO = "Left and right change the disc in the drive,\nas opening the lid and swapping it would."
        const val PLAYERS_LABEL = "Players"
        const val PLAYER_KEY_PREFIX = "nx:player:"
        const val PAD_KEY_PREFIX = "nx:pad:"
        // Presses while the menu is open never claim a slot.
        const val PLAYERS_FOOTER = "Close the menu, then press a button\non a new controller to join."
        private const val EMPTY = "Empty"
        private const val PLAYING = "Playing"
        private const val NOT_PLAYING = "Not playing"
        private const val PAD_INFO = "Not playing: this controller is ignored\nin multiplayer games until it plays again."
        const val CHEATS_LABEL = "Cheats"
        const val CHEATS_INFO = "Toggle cheats for this game"
        const val CHEAT_KEY_PREFIX = "cheat:"
        const val NO_CHEATS = "No cheats for this game."
        const val ACHIEVEMENTS_LABEL = "Achievements"
        const val NOT_RECOGNISED = "Not recognised"
        const val NO_ACHIEVEMENTS = "This game is not recognised, or has\nno achievements yet."
        const val NO_LOCKED = "Everything unlocked."
        const val ACH_KEY_PREFIX = "ach:"
        const val OVERLAY_UNAVAILABLE = "Unavailable"
        private const val QUIT_TEXT = "Save & Quit keeps your place for next time. Quit keeps the last resume point."
        const val OVERLAY_UNAVAILABLE_INFO = "Overlays are off while the DS screens\nare drawn apart."
        private const val NO_LIVE_OPTIONS = "This core has no options that\ncan be changed while running."
        private const val NO_CATEGORY_OPTIONS = "This category has no options."
    }
}
