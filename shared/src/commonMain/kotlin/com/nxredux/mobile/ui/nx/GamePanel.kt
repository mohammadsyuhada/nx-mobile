package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.ui.raAchievementSubtitle
import com.nxredux.mobile.ui.raBadgeModel
import com.nxredux.mobile.ui.toNxTone
import org.jetbrains.compose.resources.painterResource

/**
 * The game panel, in the in-game menu's look: the header (art or console logo, title, play time, achievement count,
 * clock and battery), the tab words with the underline ([MenuTabRow]), then the current tab: Shortcuts as hollow pill
 * buttons in a grid (4 columns, 2 when narrow; disabled ones dimmed and inert), Achievements as badge rows, Players as
 * the Players page's rows. The controller's [GamePanelState.focus] sits on the accent pill (the tab row's capsule on
 * Tabs). The Game pill (DS/3DS panel-up) sits bottom-right. The dual-screen panel and the in-game menu share it.
 */
@Composable
fun GamePanel(state: GamePanelState, onEvent: (GamePanelEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalNxColors.current
    val tab = GamePanelModel.resolveTab(state.tab, state.tabs)
    val tabs = state.tabs.ifEmpty { listOf(GamePanelTab.SHORTCUTS) }
    val focused = (state.focus as? PanelFocus.Item)?.index
    // The lists' bottom room: clear of the Game pill when it is drawn.
    val bottom = if (state.showGamePill) GP_LIST_BOTTOM else GP_LIST_END
    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            PanelHeader(state.header)
            MenuTabRow(
                tabs.map { it.label }, tabs.indexOf(tab).coerceAtLeast(0), onTab = { onEvent(GamePanelEvent.SelectTab(tabs[it])) },
                focused = state.focus == PanelFocus.Tabs,
            )
            when (tab) {
                GamePanelTab.SHORTCUTS -> ShortcutsTab(state.shortcuts, focused, bottom, onEvent)
                GamePanelTab.ACHIEVEMENTS -> AchievementsTab(state.achievements, state.badges, state.touchOnly, focused, bottom, onEvent)
                GamePanelTab.PLAYERS -> PlayersTab(state.players, state.playerPads, focused, bottom, onEvent)
            }
        }
        if (state.showGamePill) PanelPill("Game", Modifier.align(Alignment.BottomEnd).padding(GP_PILL_MARGIN)) { onEvent(GamePanelEvent.ShowGame) }
    }
}

/** Art (or the console logo) on the left, title and the time / achievement lines beside it, clock and battery top-right. */
@Composable
private fun PanelHeader(h: GamePanelHeader) {
    val colors = LocalNxColors.current
    val font = LocalNxFont.current
    Row(
        Modifier.fillMaxWidth().padding(start = GP_GUTTER, end = GP_GUTTER, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(GP_ART).clip(RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            val logo = consoleLogo(h.consoleTag)
            when {
                h.artPath != null -> AsyncImage(model = h.artPath, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                logo != null -> Image(painterResource(logo), contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp), colorFilter = ColorFilter.tint(colors.listText))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(h.title, color = colors.listText, fontFamily = font, fontSize = GP_TITLE_SIZE, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(GamePanelModel.timeLine(h), color = colors.hint, fontFamily = font, fontSize = GP_SMALL_SIZE, maxLines = 2, overflow = TextOverflow.Ellipsis)
            h.ra?.let { (unlocked, total) ->
                Text("Achievements $unlocked / $total", color = colors.hint, fontFamily = font, fontSize = GP_SMALL_SIZE, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(h.clockText, color = colors.listText, fontFamily = font, fontSize = GP_SMALL_SIZE, maxLines = 1)
            h.batteryPercent?.let { Text("$it%", color = colors.hint, fontFamily = font, fontSize = GP_SMALL_SIZE, maxLines = 1) }
        }
    }
}

/** The shortcut pills, 4 to a row (2 under [GP_NARROW]); a short last row keeps the cell width. */
@Composable
private fun ShortcutsTab(shortcuts: List<ShortcutState>, focused: Int?, bottom: Dp, onEvent: (GamePanelEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = gamePanelColumns(maxWidth)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = GP_GUTTER, end = GP_GUTTER, top = 10.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            shortcuts.chunked(columns).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { c, s ->
                        val i = r * columns + c
                        ShortcutPill(s, selected = focused == i, Modifier.weight(1f)) { GamePanelModel.eventFor(s)?.let(onEvent) }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The shortcut grid's columns in a panel [width] wide: 4, or 2 under [GP_NARROW] (the in-game menu steps by them). */
internal fun gamePanelColumns(width: Dp): Int = if (width < GP_NARROW) 2 else 4

/** A hollow pill ([nxButtonRing]): the label, its value under it; at 0.35 alpha and not clickable while disabled. */
@Composable
private fun ShortcutPill(s: ShortcutState, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalNxColors.current
    val font = LocalNxFont.current
    // The controller's pill scrolls into view in the grid's scroll.
    val into = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) into.bringIntoView() }
    Column(
        modifier.bringIntoViewRequester(into).alpha(if (s.enabled) 1f else GP_DISABLED_ALPHA).heightIn(min = GP_PILL_HEIGHT).clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(colors.accent) else Modifier.nxButtonRing(colors))
            .then(if (s.enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            s.label, color = if (selected) colors.onAccent else colors.listText, fontFamily = font, fontSize = GP_LABEL_SIZE,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        s.value?.let {
            Text(it, color = if (selected) colors.onAccentMuted else colors.hint, fontFamily = font, fontSize = GP_SMALL_SIZE, maxLines = 1)
        }
    }
}

/**
 * Badge, title and its state line ("Unlocked · 10 pts" green, "Pending sync" amber, "Locked" dim). [touchOnly] (the
 * dual-screen panel): the description sits under the title and a tap does nothing (no detail page there).
 */
@Composable
private fun AchievementsTab(
    list: List<RaAchievement>, badges: Map<String, String>, touchOnly: Boolean, focused: Int?, bottom: Dp, onEvent: (GamePanelEvent) -> Unit,
) {
    val colors = LocalNxColors.current
    val font = LocalNxFont.current
    LazyColumn(
        Modifier.fillMaxSize(), state = rememberFocusedListState(focused),
        contentPadding = PaddingValues(start = GP_LIST_GUTTER, end = GP_LIST_GUTTER, top = 6.dp, bottom = bottom),
    ) {
        itemsIndexed(list, key = { _, a -> a.id }) { i, a ->
            val selected = focused == i
            val sub = raAchievementSubtitle(a)
            PanelListRow(selected, if (touchOnly) Modifier else Modifier.clickable { onEvent(GamePanelEvent.OpenAchievement(a.id)) }) {
                Box(Modifier.size(GP_BADGE).clip(RoundedCornerShape(3.dp)).background(colors.main.copy(alpha = 0.13f))) {
                    raBadgeModel(badges, a)?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                }
                Column(Modifier.weight(1f)) {
                    RowText(a.title, selected)
                    if (touchOnly && a.description.isNotEmpty()) Text(
                        a.description, color = if (selected) colors.onAccentMuted else colors.hint, fontFamily = font, fontSize = GP_SMALL_SIZE,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(sub.text, maxLines = 1, fontFamily = font, fontSize = GP_SMALL_SIZE, color = colors.toneColor(sub.tone.toNxTone(), selected))
            }
        }
    }
}

/**
 * As the Players page: "Player N" with its pad (a tap steps it to the next playing pad, [GamePanelModel.nextPlayer]),
 * then each connected pad with Playing / Not playing (a tap flips it, [GamePanelModel.togglePlaying]).
 */
@Composable
private fun PlayersTab(players: List<PlayerSlotRow>, pads: List<PlayerPadRow>, focused: Int?, bottom: Dp, onEvent: (GamePanelEvent) -> Unit) {
    // Item indices are the focus indices: the slots, then the pads (no header item).
    LazyColumn(
        Modifier.fillMaxSize(), state = rememberFocusedListState(focused),
        contentPadding = PaddingValues(start = GP_LIST_GUTTER, end = GP_LIST_GUTTER, top = 6.dp, bottom = bottom),
    ) {
        itemsIndexed(players) { i, s ->
            PlayerRow("Player ${i + 1}", GamePanelModel.playerValue(s), focused == i, Modifier.clickable { onEvent(GamePanelModel.nextPlayer(i, players, pads)) })
        }
        itemsIndexed(pads, key = { _, p -> "pad:" + p.key }) { j, p ->
            PlayerRow(p.name, if (p.playing) "Playing" else "Not playing", focused == players.size + j, Modifier.clickable { onEvent(GamePanelModel.togglePlaying(p)) })
        }
    }
}

/**
 * A list's state that follows the controller's [focused] item (its LazyColumn index): the least scroll that shows it
 * whole, or a jump to it when it is off screen.
 */
@Composable
private fun rememberFocusedListState(focused: Int?): LazyListState {
    val state = rememberLazyListState()
    LaunchedEffect(focused) {
        val i = focused ?: return@LaunchedEffect
        // Not laid out yet (the tab's first frame): wait for it, so the focus is measured against real rows.
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) withFrameNanos { }
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == i }
        val top = info.viewportStartOffset + info.beforeContentPadding
        val bottom = info.viewportEndOffset - info.afterContentPadding
        when {
            item == null -> state.animateScrollToItem(i)
            item.offset < top -> state.animateScrollBy((item.offset - top).toFloat())
            item.offset + item.size > bottom -> state.animateScrollBy((item.offset + item.size - bottom).toFloat())
        }
    }
    return state
}

@Composable
private fun PlayerRow(label: String, value: String, selected: Boolean, modifier: Modifier) {
    val colors = LocalNxColors.current
    PanelListRow(selected, modifier) {
        RowText(label, selected, Modifier.weight(1f))
        Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalNxFont.current, fontSize = GP_SMALL_SIZE, color = if (selected) colors.onAccentMuted else colors.hint)
    }
}

/** A list row's frame: at least [GP_ROW_HEIGHT] high, on the accent pill while [selected]. */
@Composable
private fun PanelListRow(selected: Boolean, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val colors = LocalNxColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = GP_ROW_HEIGHT).clip(RoundedCornerShape(GP_ROW_HEIGHT / 2))
            .then(if (selected) Modifier.background(colors.accent) else Modifier).then(modifier).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun RowText(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalNxColors.current
    Text(
        text, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalNxFont.current, fontSize = GP_LABEL_SIZE,
        color = if (selected) colors.onAccent else colors.listText,
    )
}

/** A small hollow pill ([nxButtonRing]) on black: the panel's Game pill, and the Panel pill over the DS/3DS screen. */
@Composable
fun PanelPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalNxColors.current
    Box(
        modifier.clip(RoundedCornerShape(50)).background(colors.background).nxButtonRing(colors).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = colors.listText, fontFamily = LocalNxFont.current, fontSize = GP_SMALL_SIZE, maxLines = 1)
    }
}

/** Onboarding's Bottom screen step on the second display: one big "This is the bottom screen" button in the middle. */
@Composable
fun BottomScreenButton(onClick: () -> Unit) {
    val colors = LocalNxColors.current
    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).nxButtonRing(colors).clickable(onClick = onClick)
                .padding(horizontal = 32.dp, vertical = 18.dp),
        ) {
            Text("This is the bottom screen", color = colors.listText, fontFamily = LocalNxFont.current, fontSize = 22.sp, maxLines = 1)
        }
    }
}

private val GP_GUTTER = 16.dp
private val GP_LIST_GUTTER = 8.dp
private val GP_LIST_BOTTOM = 52.dp   // the last row clears the Game pill
private val GP_LIST_END = 8.dp       // without the pill
private val GP_PILL_MARGIN = 10.dp
private val GP_ART = 64.dp
private val GP_BADGE = 32.dp
private val GP_ROW_HEIGHT = 44.dp
private val GP_PILL_HEIGHT = 52.dp
private val GP_NARROW = 360.dp       // narrower: the shortcut grid goes to 2 columns
private const val GP_DISABLED_ALPHA = 0.35f
private val GP_TITLE_SIZE = 17.sp
private val GP_LABEL_SIZE = 15.sp
private val GP_SMALL_SIZE = 13.sp
