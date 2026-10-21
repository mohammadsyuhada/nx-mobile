package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.achievements.RaAchievementState
import com.nxredux.mobile.achievements.RaBadgeFolders

/** The second-screen panel's tabs. */
enum class PanelTab(val label: String) { STATES("States"), ACHIEVEMENTS("Achievements") }

/** What a touch on the panel asks the game for. Slot 0 is Auto (the resume state), 1..8 the manual slots. */
sealed interface PanelEvent {
    data object QuickSave : PanelEvent
    data object QuickLoad : PanelEvent
    data class Load(val slot: Int) : PanelEvent
    data class Save(val slot: Int) : PanelEvent
    /** The Game pill: the DS/3DS screen comes back to the second display. */
    data object ShowGame : PanelEvent
}

/**
 * What the panel shows: the [tab], the [slots] (`slots[0]` is Auto), this game's [achievements] with the (unlocked,
 * total) [summary] (null: no RetroAchievements set, and no Achievements tab), the downloaded [badges] (file name → image
 * model) and whether the DS/3DS [showGamePill] is drawn.
 */
data class PanelState(
    val tab: PanelTab,
    val slots: List<SlotUi>,
    val achievements: List<RaAchievement>?,
    val summary: Pair<Int, Int>?,
    val badges: Map<String, String>,
    val showGamePill: Boolean,
)

/** The panel's touch rules. */
object PanelModel {
    /** States always; Achievements only when the game has a set (the in-game menu's Achievements row check). */
    fun tabs(achievementsAvailable: Boolean): List<PanelTab> =
        if (achievementsAvailable) listOf(PanelTab.STATES, PanelTab.ACHIEVEMENTS) else listOf(PanelTab.STATES)

    /** A tap loads an occupied slot; an empty one does nothing. */
    fun tap(slotIndex: Int, slot: SlotUi): PanelEvent? = if (slot.occupied) PanelEvent.Load(slotIndex) else null

    /** A long press saves over a manual slot, with no confirm (as the menu's Save); Auto is load-only. */
    fun longPress(slotIndex: Int): PanelEvent? = if (slotIndex >= 1) PanelEvent.Save(slotIndex) else null
}

/**
 * The second display's panel, touch-only and in the in-game menu's look: the tab words with the underline
 * ([MenuTabRow]), then on States the Quick save / Quick load ring buttons and the slot rows (tap loads, long press
 * saves), on Achievements "N / M unlocked" and the achievement rows. The last tapped row of each tab sits on the
 * accent pill. The Game pill (DS/3DS) sits bottom-right.
 */
@Composable
fun SecondScreenPanel(state: PanelState, onTab: (PanelTab) -> Unit, onEvent: (PanelEvent) -> Unit) {
    val colors = LocalNxColors.current
    val tabs = PanelModel.tabs(state.summary != null && state.achievements != null)
    val tab = state.tab.takeIf { it in tabs } ?: PanelTab.STATES
    // The last tapped row per tab: session state, it only marks where the finger was.
    val picked = remember { mutableStateMapOf<PanelTab, Int>() }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            MenuTabRow(tabs.map { it.label }, tabs.indexOf(tab), onTab = { onTab(tabs[it]) })
            when (tab) {
                PanelTab.STATES -> StatesTab(state.slots, picked[PanelTab.STATES], { picked[PanelTab.STATES] = it }, onEvent)
                PanelTab.ACHIEVEMENTS -> AchievementsTab(
                    state.achievements.orEmpty(), state.summary, state.badges, picked[PanelTab.ACHIEVEMENTS],
                ) { picked[PanelTab.ACHIEVEMENTS] = it }
            }
        }
        if (state.showGamePill) PanelPill("Game", Modifier.align(Alignment.BottomEnd).padding(PANEL_PILL_MARGIN)) { onEvent(PanelEvent.ShowGame) }
    }
}

/** A small hollow pill ([nxButtonRing]) on black: the panel's Game pill, and the Panel pill over the DS/3DS screen. */
@Composable
fun PanelPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalNxColors.current
    Box(
        modifier.clip(RoundedCornerShape(50)).background(colors.background).nxButtonRing(colors).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = colors.listText, fontFamily = LocalNxFont.current, fontSize = 13.sp, maxLines = 1)
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

@Composable
private fun StatesTab(slots: List<SlotUi>, picked: Int?, onPick: (Int) -> Unit, onEvent: (PanelEvent) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = PANEL_GUTTER, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RingButton("Quick save", Modifier.weight(1f)) { onEvent(PanelEvent.QuickSave) }
        RingButton("Quick load", Modifier.weight(1f)) { onEvent(PanelEvent.QuickLoad) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = PANEL_LIST_GUTTER, end = PANEL_LIST_GUTTER, bottom = PANEL_LIST_BOTTOM)) {
        itemsIndexed(slots) { i, slot ->
            SlotRow(
                slot, selected = picked == i,
                onTap = { onPick(i); PanelModel.tap(i, slot)?.let(onEvent) },
                onLongPress = PanelModel.longPress(i)?.let { e -> { onPick(i); onEvent(e) } },
            )
        }
    }
}

@Composable
private fun AchievementsTab(list: List<RaAchievement>, summary: Pair<Int, Int>?, badges: Map<String, String>, picked: Int?, onPick: (Int) -> Unit) {
    val colors = LocalNxColors.current
    if (summary != null) Text(
        "${summary.first} / ${summary.second} unlocked", color = colors.hint, fontFamily = LocalNxFont.current, fontSize = PANEL_TIME_SIZE,
        modifier = Modifier.padding(start = PANEL_GUTTER + 2.dp, top = 10.dp, bottom = 6.dp),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = PANEL_LIST_GUTTER, end = PANEL_LIST_GUTTER, bottom = PANEL_LIST_BOTTOM)) {
        itemsIndexed(list, key = { _, a -> a.id }) { i, a ->
            val locked = a.state == RaAchievementState.LOCKED
            AchievementRow(a, RaBadgeFolders.fileName(a.badge, locked)?.let(badges::get), selected = picked == i) { onPick(i) }
        }
    }
}

/** A hollow pill button ([nxButtonRing]) that fills its share of the row. */
@Composable
private fun RingButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalNxColors.current
    Box(
        modifier.clip(RoundedCornerShape(50)).nxButtonRing(colors).clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.listText, fontFamily = LocalNxFont.current, fontSize = PANEL_LABEL_SIZE, maxLines = 1)
    }
}

/** One slot: its picture (a faint tile while empty), its label, and when it was saved ("Empty") on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotRow(slot: SlotUi, selected: Boolean, onTap: () -> Unit, onLongPress: (() -> Unit)?) {
    val colors = LocalNxColors.current
    PanelRow(selected, Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
        Box(Modifier.size(PANEL_THUMB_W, PANEL_THUMB_H).clip(RoundedCornerShape(3.dp)).background(colors.main.copy(alpha = 0.13f))) {
            if (slot.thumbPath != null) {
                AsyncImage(model = slot.thumbPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        RowLabel(slot.label, selected, Modifier.weight(1f))
        Text(
            if (slot.occupied) slot.subtitle else "Empty", maxLines = 1, fontFamily = LocalNxFont.current, fontSize = PANEL_TIME_SIZE,
            color = if (selected) colors.onAccentMuted else colors.hint,
        )
    }
}

/** One achievement: its badge, title and "Unlocked" in green, "Pending sync" in amber or "Locked" in grey, as the in-game list shows them. */
@Composable
private fun AchievementRow(a: RaAchievement, badge: String?, selected: Boolean, onTap: () -> Unit) {
    val colors = LocalNxColors.current
    PanelRow(selected, Modifier.clickable(onClick = onTap)) {
        Box(Modifier.size(PANEL_THUMB_H).clip(RoundedCornerShape(3.dp)).background(colors.main.copy(alpha = 0.13f))) {
            if (badge != null) AsyncImage(model = badge, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        RowLabel(a.title, selected, Modifier.weight(1f))
        // As the in-game Achievements page words them; pending in the warning colour, as NxRichList's WARNING tone.
        val (label, color) = when (a.state) {
            RaAchievementState.UNLOCKED -> "Unlocked" to if (selected) colors.successOnAccent else colors.success
            RaAchievementState.PENDING -> "Pending sync" to if (selected) colors.warningOnAccent else colors.warning
            RaAchievementState.LOCKED -> "Locked" to if (selected) colors.onAccentMuted else colors.hint
        }
        Text(label, maxLines = 1, fontFamily = LocalNxFont.current, fontSize = PANEL_TIME_SIZE, color = color)
    }
}

/** A row's frame: [PANEL_ROW_HEIGHT] high, on the accent pill while [selected]. */
@Composable
private fun PanelRow(selected: Boolean, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val colors = LocalNxColors.current
    Row(
        Modifier.fillMaxWidth().height(PANEL_ROW_HEIGHT).clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(colors.accent) else Modifier).then(modifier).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun RowLabel(text: String, selected: Boolean, modifier: Modifier) {
    val colors = LocalNxColors.current
    Text(
        text, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalNxFont.current, fontSize = PANEL_LABEL_SIZE,
        color = if (selected) colors.onAccent else colors.listText,
    )
}

private val PANEL_GUTTER = 16.dp
private val PANEL_LIST_GUTTER = 8.dp
private val PANEL_LIST_BOTTOM = 52.dp   // the last row clears the Game pill
private val PANEL_PILL_MARGIN = 10.dp
private val PANEL_ROW_HEIGHT = 44.dp
private val PANEL_THUMB_W = 44.dp
private val PANEL_THUMB_H = 32.dp
private val PANEL_LABEL_SIZE = 15.sp
private val PANEL_TIME_SIZE = 13.sp
