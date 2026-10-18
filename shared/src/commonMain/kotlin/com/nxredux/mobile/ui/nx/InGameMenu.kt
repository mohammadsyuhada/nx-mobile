package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.ui.SplitPanes
import com.nxredux.mobile.ui.UNFOCUSED_ALPHA

/** One save-state slot as shown in the preview. [thumbPath] is a coil-loadable model (e.g. a file:// URI). */
data class SlotUi(
    val label: String,
    val subtitle: String,
    val thumbPath: String?,
    val occupied: Boolean,
    val aspect: Float = 4f / 3f,
)

/**
 * The in-game pause menu: fills the top game area with a semi-transparent wash and renders the top page
 * of [state]. The root page keeps the list + save-slot preview layout; other pages use [NxOptionsRow]s
 * (option rows show `< value >` when selected, submenus and actions a `>`), a dimmed footer line (status or the
 * highlighted row's info) and page-specific hints. Selection is hoisted: touch, the on-screen pad and
 * hardware controllers all drive the host's model. A tap on a message page, or on the hint bar of any
 * non-root page, calls [onDismiss]; a tap on the selected option row's value calls [onChange] with +1
 * (touch equivalent of RIGHT). On an Adjust page only a bottom strip is drawn; the rest stays transparent
 * so the paused frame shows. Its CHANGE hint calls [onChange] with +1 and its NEXT hint sends DOWN to
 * [onPress] (see [adjustHintButton]). An AchievementDetail page draws [achievementDetail] (the model's
 * `achievementDetail()`) with its badge from [badgePath] and the [muted] marker; a tap on it calls [onDismiss].
 *
 * On a wide window ([inGameMenuSplit] on [LocalWindowSize], the Settings rule, reported through [onSplit])
 * the menu draws as two panes ([menuPanes]) under the one title and over the one hint bar: the root beside a dimmed
 * preview of its highlighted row ([preview]; a tap there calls [onPreviewTap]), a deeper page beside its dimmed parent
 * (a tap there calls [onParentTap] with the parent's kind). Adjust strips and message pages stay whole.
 */
@Composable
fun InGameMenu(
    state: MenuState,
    slots: List<SlotUi>,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onChange: (Int) -> Unit = {},
    onPress: (NxButton) -> Unit = {},
    achievementDetail: RaAchievement? = null,
    badgePath: (RaAchievement) -> String? = { null },
    muted: (Int) -> Boolean = { false },
    onInfoTruncated: (String, Boolean) -> Unit = { _, _ -> },
    preview: (MenuRow) -> MenuPage? = { null },
    onSplit: (Boolean) -> Unit = {},
    onParentTap: (PageKind, Int) -> Unit = { _, _ -> },
    onPreviewTap: (Int) -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The lists' row rule: the root rows' text is the Tools lists' label size.
        val rowHeight = maxHeight / nxRowCountFor(maxHeight)
        // The window (the host's [LocalWindowSize]), as Settings decides; the menu's own area without one.
        val split = inGameMenuSplit(LocalWindowSize.current.takeIf { it != DpSize.Zero } ?: DpSize(maxWidth, maxHeight))
        // After every composition, so the model's input agrees with the panes drawn from the first frame.
        SideEffect { onSplit(split) }
        // A dialog page (Quit's) draws over the page under it, which takes no input meanwhile (the scrim covers it).
        val (under, dialog) = menuDialog(state)
        if (under.page.kind is PageKind.Adjust) {
            AdjustContent(under.page, rowHeight, onDismiss, onChange, onPress)
        } else {
            val detail = AchievementDetailUi(achievementDetail, badgePath, muted)
            val panes = if (split) menuPanes(under, preview) else null
            val splitUi = panes?.let { SplitUi(it, onParentTap, onPreviewTap) }
            InGameMenuContent(under, slots, rowHeight, onSelect, onActivate, onDismiss, onChange, onPress, detail, onInfoTruncated, splitUi)
        }
        if (dialog != null) {
            // Only a tap opens it (a pad's A / X quit on the row itself), so no key hints.
            NxChoiceDialog(dialog.title, dialog.footer, dialog.rows.map { it.label }, dialog.selected, onChoose = onActivate, onCancel = onDismiss)
        }
    }
}

/** [under]: the menu drawn as pages; [dialog]: the top page when it draws as a dialog over them ([NxChoiceDialog]). */
internal data class MenuDialog(val under: MenuState, val dialog: MenuPage?)

/** Whether [state]'s top page is a dialog (Quit Game): then the pages under it draw, the dialog over them. */
internal fun menuDialog(state: MenuState): MenuDialog =
    if (state.page.kind == PageKind.QuitGame && state.stack.size > 1) {
        MenuDialog(state.copy(stack = state.stack.dropLast(1)), state.page)
    } else {
        MenuDialog(state, null)
    }

/** The two panes to draw ([menuPanes]) and their taps: a row of the dimmed parent, a row of the root's preview. */
private class SplitUi(val panes: MenuPanes, val onParentTap: (PageKind, Int) -> Unit, val onPreviewTap: (Int) -> Unit)

@Composable
private fun InGameMenuContent(
    state: MenuState,
    slots: List<SlotUi>,
    rowHeight: Dp,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
    onPress: (NxButton) -> Unit,
    detail: AchievementDetailUi,
    onInfoTruncated: (String, Boolean) -> Unit,
    split: SplitUi?,
) {
    val page = state.page
    // The save-slot card's text: the list label size at the menu's row height.
    val slotTextSize = NxTextSizes.listLabel(rowHeight) * nxTextScale()   // Appearance → Text size
    val selectedRow = page.rows.getOrNull(page.selected)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .pointerInput(Unit) { detectTapGestures { /* swallow */ } }
            // The wash covers the camera cutout; the content stays clear of it on whichever side it is.
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        // The standard page title (the game list's and every Tools/Settings page's): the game on the root, the page's name below.
        NxPageTitle(page.title)
        if (split != null) {
            SplitBody(state, split, slots, rowHeight, slotTextSize, onSelect, onActivate, onDismiss, onChange, detail, onInfoTruncated, Modifier.weight(1f))
        } else when (page.kind) {
            PageKind.Root -> RootBody(page, state.slotIndex, slots, rowHeight, slotTextSize, onSelect, onActivate, onChange, Modifier.weight(1f))
            PageKind.Message -> MessageBody(page, onDismiss, Modifier.weight(1f))
            is PageKind.AchievementDetail -> AchievementDetailBody(detail, onDismiss, Modifier.weight(1f))
            // Keyed by page so each page gets its own scroll position.
            else -> key(page.kind) { RowsBody(page, rowHeight, onSelect, onActivate, onChange, onInfoTruncated, Modifier.weight(1f)) }
        }
        // In landscape the menu covers the pad, so each hint is a tap target that sends its button (see
        // [menuHintButton]); off the root page a tap on the bar's empty space is Back.
        val dismiss by rememberUpdatedState(onDismiss)
        val hintModifier = if (page.kind != PageKind.Root) {
            Modifier.pointerInput(Unit) { detectTapGestures { dismiss() } }
        } else {
            Modifier
        }
        NxHintBar(hintsFor(page, selectedRow, state.selectedInfoTruncated), hintModifier, onHint = { onPress(menuHintButton(it)) })
    }
}

@Composable
private fun RootBody(
    page: MenuPage,
    slotIndex: Int,
    slots: List<SlotUi>,
    rowHeight: Dp,
    slotTextSize: TextUnit,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    onChange: (Int) -> Unit,
    modifier: Modifier,
) {
    val showSlots = page.rows.getOrNull(page.selected) is MenuRow.Slot
    // The labels start [MenuPillPadding] into their pills, where the page title's first letter sits ([nxListInset]).
    Row(modifier.fillMaxWidth().padding(start = nxListInset() - MenuPillPadding, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
        Box(Modifier.weight(0.42f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            RootList(page, rowHeight, onSelect, onActivate, onChange)
        }
        // Always reserve the preview column so the list keeps a fixed width and doesn't shift
        // when Save/Load (which shows the preview) is selected.
        if (showSlots) {
            SlotPreview(
                slot = slots.getOrNull(slotIndex),
                slotIndex = slotIndex,
                slotCount = slots.size,
                textSize = slotTextSize,
                onTap = { onActivate(page.selected) },
                onChange = onChange,
                modifier = Modifier.weight(0.58f).fillMaxHeight().padding(start = 12.dp),
            )
        } else {
            Spacer(Modifier.weight(0.58f).fillMaxHeight())
        }
    }
}

/**
 * The root page's list: its rows at [rowHeight], an Option row (Disc N/M) with its value, the change arrows while selected.
 * Save, Load and the plain Quit take two taps ([inGameRowTakesTwoTaps]); while Save or Load is highlighted a sideways
 * swipe on the list steps the slot, as on the slot card ([slotSwipe]).
 */
@Composable
private fun RootList(page: MenuPage, pageRowHeight: Dp, onSelect: (Int) -> Unit, onActivate: (Int) -> Unit, onChange: (Int) -> Unit) {
    val onSlot = page.rows.getOrNull(page.selected) is MenuRow.Slot
    val rowHeight = nxOptionsRowHeight(pageRowHeight, LocalNxOptionText.current)   // Appearance → Text size, as the option pages
    CompositionLocalProvider(LocalNxTapOpens provides { i -> !inGameRowTakesTwoTaps(page.rows.getOrNull(i)) }) { NxList(
        rows = page.rows.mapIndexed { i, row ->
            val value = (row as? MenuRow.Option)?.valueLabel
            NxRow(row.label, secondary = value?.let { if (i == page.selected) "<  $it  >" else it })
        },
        selectedIndex = page.selected,
        onSelect = onSelect,
        onOpen = onActivate,
        modifier = Modifier.fillMaxWidth().height(rowHeight * page.rows.size.coerceAtLeast(1))
            .then(if (onSlot) Modifier.slotSwipe(onChange) else Modifier),
        rowHeightOverride = rowHeight,
        pillPaddingOverride = MenuPillPadding,
    ) }
}

/**
 * The two panes between the title and the hint bar ([SplitPanes], the Settings frame): the focused one takes the
 * input callbacks; the dimmed one ([UNFOCUSED_ALPHA]) sends its taps to [SplitUi]. A dimmed preview page highlights no row.
 */
@Composable
private fun SplitBody(
    state: MenuState,
    split: SplitUi,
    slots: List<SlotUi>,
    rowHeight: Dp,
    slotTextSize: TextUnit,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
    detail: AchievementDetailUi,
    onInfoTruncated: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    val panes = split.panes
    val noInfo: (String, Boolean) -> Unit = { _, _ -> }
    Box(modifier.fillMaxWidth()) {
        SplitPanes(
            left = {
                if (panes.focusLeft) {
                    PaneBody(panes.left, rowHeight, onSelect, onActivate, onChange, onInfoTruncated, detail, onDismiss, Modifier)
                } else {
                    val tap: (Int) -> Unit = { i -> split.onParentTap(panes.left.kind, i) }
                    PaneBody(panes.left, rowHeight, tap, tap, {}, noInfo, detail, {}, Modifier.alpha(UNFOCUSED_ALPHA))
                }
            },
            right = {
                when (val right = panes.right) {
                    MenuPaneContent.Empty -> Unit
                    // Full brightness, unlike a page preview: the card is swiped and tapped itself.
                    MenuPaneContent.Slots -> SlotPreview(
                        slot = slots.getOrNull(state.slotIndex), slotIndex = state.slotIndex, slotCount = slots.size,
                        textSize = slotTextSize, onTap = { onActivate(state.page.selected) }, onChange = onChange,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    is MenuPaneContent.Page -> if (panes.focusLeft) {
                        PaneBody(right.page.copy(selected = -1), rowHeight, split.onPreviewTap, split.onPreviewTap, {}, noInfo, detail, {}, Modifier.alpha(UNFOCUSED_ALPHA))
                    } else {
                        PaneBody(right.page, rowHeight, onSelect, onActivate, onChange, onInfoTruncated, detail, onDismiss, Modifier)
                    }
                }
            },
        )
    }
}

/** One pane's page: the root list, an achievement's detail, or a page of options rows. */
@Composable
private fun PaneBody(
    page: MenuPage,
    rowHeight: Dp,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    onChange: (Int) -> Unit,
    onInfoTruncated: (String, Boolean) -> Unit,
    detail: AchievementDetailUi,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    when (page.kind) {
        PageKind.Root -> Box(
            modifier.fillMaxSize().padding(start = nxListInset() - MenuPillPadding, end = 12.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) { RootList(page, rowHeight, onSelect, onActivate, onChange) }
        is PageKind.AchievementDetail -> AchievementDetailBody(detail, onDismiss, modifier.fillMaxSize())
        else -> key(page.kind) { RowsBody(page, rowHeight, onSelect, onActivate, onChange, onInfoTruncated, modifier.fillMaxSize()) }
    }
}

/** Horizontal padding inside a label pill on the root list and the adjust strip (the rows pages use [NxOptionsRow]). */
private val MenuPillPadding = 10.dp

/** Height of the band above/below a scrolling list that holds its scroll chevron. */
private val ArrowBand = 10.dp

/** The scroll position of a rows page; [key] is the (selection, rows per page, row count) it was last fitted to. */
private class RowsWindow {
    var scroll by mutableIntStateOf(0)
    var key: Any? = null
}

/**
 * Full-width rows in a window of whole rows, centred vertically when they all fit, with a reserved
 * description area below (the selected row's info, else the page footer) so the list never jumps.
 * When the list scrolls, chevrons above/below show there are more rows; a vertical drag moves the window
 * a row at a time, while selection changes scroll it the minimum to keep the selected row visible.
 */
@Composable
private fun RowsBody(
    page: MenuPage,
    rowHeight: Dp,
    onSelect: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    onChange: (Int) -> Unit,
    onInfoTruncated: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    // Options rows as on the Settings pages: a list row's pitch, the label one step under the list's, the value smaller.
    val itemHeight = nxOptionsRowHeight(rowHeight, LocalNxOptionText.current)   // Appearance → Text size
    val listText = NxTextSizes.listLabel(itemHeight)
    val labelSize = nxOptionsLabelSize(listText)
    val valueSize = nxOptionsValueSize(listText)
    val innerPad = NxOptionsInnerPad
    val descLine = NxTextSizes.menuInfoLine
    val descHeight = with(density) { (descLine * InfoLines).toDp() } + 12.dp
    val rowInfo = (page.rows.getOrNull(page.selected) as? MenuRow.Option)?.info?.takeIf { it.isNotBlank() }
    val info = page.rows.getOrNull(page.selected)?.info?.takeIf { it.isNotBlank() } ?: page.footer
    val window = remember { RowsWindow() }
    val tapOpens = LocalNxTapOpens.current
    // The row margin leaves the labels [innerPad] further in, where the page title's first letter sits ([nxListInset]).
    val sidePad = NxOptionsSidePad
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(start = sidePad, end = sidePad, top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            val count = page.rows.size
            val scrolling = count > (maxHeight / itemHeight).toInt()
            val perPage = if (scrolling) maxOf(1, ((maxHeight - ArrowBand * 2) / itemHeight).toInt()) else count
            val maxStart = maxOf(0, count - perPage)
            val fitKey = Triple(page.selected, perPage, count)
            // A new fit (selection, page size or row count) moves the window the minimum; otherwise it stays where a drag
            // left it. Read here, recorded once the composition applies, so composing never writes the window.
            val fitted = if (window.key != fitKey) windowStart(page.selected, window.scroll, perPage, count) else window.scroll
            val start = fitted.coerceIn(0, maxStart)
            SideEffect {
                window.key = fitKey
                window.scroll = start
            }
            val end = minOf(count, start + perPage)
            val rowPx = with(density) { itemHeight.toPx() }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (scrolling) {
                    Box(Modifier.height(ArrowBand), contentAlignment = Alignment.Center) {
                        if (start > 0) NxScrollArrow(up = true, width = rowHeight * 0.8f)
                    }
                }
                Column(
                    Modifier.fillMaxWidth().height(itemHeight * (end - start))
                        .pointerInput(maxStart, rowPx) {
                            // Touch scroll: each row-height of drag moves the window one row.
                            var acc = 0f
                            detectVerticalDragGestures(onDragStart = { acc = 0f }) { change, dy ->
                                change.consume()
                                acc += dy
                                while (acc <= -rowPx && window.scroll < maxStart) { window.scroll++; acc += rowPx }
                                while (acc >= rowPx && window.scroll > 0) { window.scroll--; acc -= rowPx }
                                acc = acc.coerceIn(-rowPx, rowPx)
                            }
                        },
                ) {
                    for (i in start until end) {
                        key(i) {
                            val selected = i == page.selected
                            val row = menuOptionRow(page.rows[i])
                            NxOptionsRow(
                                row, selected, itemHeight, labelSize, valueSize, innerPad,
                                Modifier.clickable { nxTap(i, selected, tapOpens(i), onSelect, onActivate) },
                                // Tapping the selected option's value cycles it forward (touch equivalent of RIGHT).
                                onValueClick = if (row.cycleable) { { onChange(1) } } else null,
                            )
                        }
                    }
                }
                if (scrolling) {
                    Box(Modifier.height(ArrowBand), contentAlignment = Alignment.Center) {
                        if (end < count) NxScrollArrow(up = false, width = rowHeight * 0.8f)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(descHeight), contentAlignment = Alignment.Center) {
            // An option's description reports whether it was cut off: X then shows it whole.
            if (info.isNotBlank()) FooterText(info, descLine, onTruncated = if (info == rowInfo) { t -> onInfoTruncated(info, t) } else null)
        }
    }
}

/**
 * A rows-page row as an options row: an option shows its value (`< value >` while selected); a submenu and an action a
 * `>` (NX Redux ma_menu.c), so they take the two-layer bar too.
 */
internal fun menuOptionRow(row: MenuRow): NxRow = when (row) {
    is MenuRow.Option -> NxRow(row.label, secondary = row.valueLabel, cycleable = true)
    is MenuRow.Submenu, is MenuRow.Action -> NxRow(row.label, secondary = ">")
    is MenuRow.Slot -> NxRow(row.label)
}

/**
 * Whether root [row] takes two taps, a tap to highlight and a tap on the highlighted one to act ([LocalNxTapOpens]):
 * Save and Load, whose slot is chosen first, and the plain Quit after netplay, which quits with nothing to confirm.
 * Quit itself opens its dialog on one tap (the dialog is the second step).
 */
internal fun inGameRowTakesTwoTaps(row: MenuRow?): Boolean =
    row is MenuRow.Slot || (row as? MenuRow.Action)?.action == MenuAction.QUIT_ONLY

/** The slot step of a sideways swipe [drag] px long: one slot past [threshold] (finger left: the next), however far. */
internal fun slotSwipeStep(drag: Float, threshold: Float): Int = when {
    drag <= -threshold -> 1
    drag >= threshold -> -1
    else -> 0
}

/** How far a swipe must go to step the slot. */
private val SlotSwipeDistance = 40.dp

/** A sideways swipe steps the save slot ([slotSwipeStep]) through [onChange], +1 the next slot, as RIGHT. */
@Composable
private fun Modifier.slotSwipe(onChange: (Int) -> Unit): Modifier {
    val change by rememberUpdatedState(onChange)
    return pointerInput(Unit) {
        var drag = 0f
        detectHorizontalDragGestures(
            onDragStart = { drag = 0f },
            onDragEnd = { slotSwipeStep(drag, SlotSwipeDistance.toPx()).takeIf { it != 0 }?.let { change(it) } },
        ) { c, dx -> c.consume(); drag += dx }
    }
}

@Composable
private fun MessageBody(page: MenuPage, onDismiss: () -> Unit, modifier: Modifier) {
    val colors = LocalNxColors.current
    val dismiss by rememberUpdatedState(onDismiss)
    val scroll = rememberScrollState()
    val stepPx = with(LocalDensity.current) { MessageScrollStep.toPx() }
    // Controller UP/DOWN (the model counts them in scrollSeq): one step per press, clamped by the scroll state.
    LaunchedEffect(page.scrollSeq) { if (page.scrollSeq > 0) scroll.animateScrollBy(page.scrollDir * stepPx) }
    // Message pages have no rows, so a tap anywhere on the message dismisses it (touch equivalent of B).
    Box(
        modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { dismiss() } }.padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = page.footer,
            color = colors.listText,
            fontFamily = LocalNxFont.current,
            fontWeight = FontWeight.SemiBold,
            fontSize = NxTextSizes.menuMessage,
            lineHeight = NxTextSizes.menuMessageLine,
            textAlign = TextAlign.Center,
            // No line cap: an option's full description can outgrow the page, so it scrolls (centred when it fits).
            modifier = Modifier.verticalScroll(scroll).padding(vertical = 8.dp),
        )
    }
}

/** What the achievement detail page draws: the top page's achievement and the host's badge/mute lookups. */
private class AchievementDetailUi(
    val achievement: RaAchievement?,
    val badgePath: (RaAchievement) -> String?,
    val muted: (Int) -> Boolean,
)

@Composable
private fun AchievementDetailBody(detail: AchievementDetailUi, onDismiss: () -> Unit, modifier: Modifier) {
    val dismiss by rememberUpdatedState(onDismiss)
    // Like a message page, the detail has no rows: a tap anywhere on it goes back (touch equivalent of B).
    Box(modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { dismiss() } }, contentAlignment = Alignment.Center) {
        val a = detail.achievement ?: return@Box
        AchievementDetail(a, detail.badgePath(a), detail.muted(a.id))
    }
}

/**
 * Adjust mode: one strip at the bottom of the menu area (label left on the accent, `<  value  >` right on the Secondary
 * accent's bar, the options rows' [nxOptionsSelection]) over the hint line. Everything above is transparent, so the
 * paused frame, redrawn after each change, is fully visible; a tap on it (or on the B hint) leaves adjust mode, and
 * `<` / `>` change the value.
 */
@Composable
private fun AdjustContent(
    page: MenuPage,
    rowHeight: Dp,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
    onPress: (NxButton) -> Unit,
) {
    val row = page.rows.firstOrNull() as? MenuRow.Option ?: return
    val selection = nxOptionsSelection(LocalNxColors.current)
    val font = LocalNxFont.current
    val fontSize = NxTextSizes.listLabel(rowHeight)
    val valueSize = NxTextSizes.secondary(fontSize)
    val pillHeight = rowHeight * 0.84f
    val dismiss by rememberUpdatedState(onDismiss)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) { detectTapGestures { dismiss() } })
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xD9000000))
                .pointerInput(Unit) { detectTapGestures { /* swallow */ } }
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        ) {
            Box(
                Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.fillMaxWidth().height(pillHeight).background(selection.bar, RoundedCornerShape(50)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        Box(
                            Modifier.height(pillHeight).background(selection.fill, RoundedCornerShape(50))
                                .padding(horizontal = MenuPillPadding),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = row.label,
                                color = selection.text,
                                fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = fontSize, lineHeight = fontSize,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    AdjustArrow("<", valueSize, selection.barText) { onChange(-1) }
                    Text(
                        text = row.valueLabel,
                        color = selection.barText,
                        fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = valueSize, lineHeight = valueSize,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                    AdjustArrow(">", valueSize, selection.barText) { onChange(1) }
                }
            }
            NxHintBar(hintsFor(page, row), onHint = { hint ->
                when (val b = adjustHintButton(hint)) {
                    null -> Unit
                    NxButton.B -> dismiss()
                    NxButton.RIGHT -> onChange(1)
                    else -> onPress(b)
                }
            })
        }
    }
}

/**
 * The button a tapped adjust-strip hint stands for: CHANGE (◄►) steps the value forward like RIGHT,
 * NEXT (▲▼) moves to the next visual row like DOWN, DONE is B. Null for a hint that does nothing.
 */
internal fun adjustHintButton(hint: NxButton): NxButton? = when (hint) {
    NxButton.LEFT -> NxButton.RIGHT
    NxButton.UP -> NxButton.DOWN
    NxButton.B -> NxButton.B
    else -> null
}

@Composable
private fun AdjustArrow(text: String, size: TextUnit, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = LocalNxFont.current, fontWeight = FontWeight.SemiBold, fontSize = size, lineHeight = size,
        )
    }
}

/** Lines kept below the options list for the selected row's info (nx-redux keeps two; the phone fits three). */
private const val InfoLines = 3

@Composable
private fun FooterText(text: String, lineHeight: TextUnit, onTruncated: ((Boolean) -> Unit)? = null) {
    Text(
        text = text,
        color = LocalNxColors.current.hint,
        fontFamily = LocalNxFont.current,
        fontWeight = FontWeight.SemiBold,
        fontSize = NxTextSizes.menuInfo,
        lineHeight = lineHeight,
        maxLines = InfoLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        onTextLayout = { r -> onTruncated?.invoke(r.hasVisualOverflow || (r.lineCount > 0 && r.isLineEllipsized(r.lineCount - 1))) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    )
}

internal fun hintsFor(page: MenuPage, row: MenuRow?, infoTruncated: Boolean = false): List<Hint> = when {
    page.kind is PageKind.Adjust -> listOf(Hint(NxButton.LEFT, "Change"), Hint(NxButton.UP, "Next"), Hint(NxButton.B, "Done"))
    page.kind == PageKind.Message -> listOf(Hint(NxButton.B, "Back"))
    page.kind is PageKind.AchievementDetail -> listOf(Hint(NxButton.LEFT, "Next"), Hint(NxButton.X, "Mute"), Hint(NxButton.B, "Back"))
    page.kind == PageKind.Achievements && row != null ->
        listOf(Hint(NxButton.A, "Details"), Hint(NxButton.Y, "Locked only"), Hint(NxButton.X, "Mute"), Hint(NxButton.B, "Back"))
    // Filtered to nothing ("Everything unlocked."): Y still brings the full list back.
    page.kind == PageKind.Achievements -> listOf(Hint(NxButton.Y, "Locked only"), Hint(NxButton.B, "Back"))
    page.rows.isEmpty() -> listOf(Hint(NxButton.B, "Back"))   // e.g. an empty Cheats page: A does nothing
    page.kind == PageKind.Root && row is MenuRow.Slot ->
        listOf(Hint(NxButton.LEFT, "Slot"), Hint(NxButton.A, row.label), Hint(NxButton.B, "Resume"))
    // The root has no info area (Disc N/M): A opens the info there.
    page.kind == PageKind.Root && row is MenuRow.Option -> buildList {
        add(Hint(NxButton.LEFT, "Change"))
        if (row.info.isNotBlank()) add(Hint(NxButton.A, "Info"))
        add(Hint(NxButton.B, "Resume"))
    }
    row is MenuRow.Option -> optionHints(row, infoTruncated)
    // A pad picks how to quit on the Quit row itself (a tap opens the Quit dialog).
    page.kind == PageKind.Root && (row as? MenuRow.Action)?.action == MenuAction.QUIT ->
        listOf(Hint(NxButton.A, "Save & Quit"), Hint(NxButton.X, "Quit"), Hint(NxButton.B, "Resume"))
    page.kind == PageKind.Root && (row as? MenuRow.Action)?.action == MenuAction.QUIT_ONLY ->
        listOf(Hint(NxButton.A, "Quit"), Hint(NxButton.B, "Resume"))
    page.kind == PageKind.Root -> listOf(Hint(NxButton.A, "Select"), Hint(NxButton.B, "Resume"))
    row is MenuRow.Submenu -> listOf(Hint(NxButton.A, "Open"), Hint(NxButton.B, "Back"))
    else -> listOf(Hint(NxButton.A, "Select"), Hint(NxButton.B, "Back"))
}

/** An option row: Change, Adjust on a visual row, X Info when its description is cut off in the info area. */
private fun optionHints(row: MenuRow.Option, infoTruncated: Boolean): List<Hint> = buildList {
    add(Hint(NxButton.LEFT, "Change"))
    if (row.visual) add(Hint(NxButton.A, "Adjust"))
    if (infoTruncated && row.info.isNotBlank()) add(Hint(NxButton.X, "Info"))
    add(Hint(NxButton.B, "Back"))
}

/**
 * The button a tapped menu hint sends: CHANGE (◄►) steps the value forward like RIGHT (and SLOT/NEXT step forward
 * too); every other hint sends its own button.
 */
internal fun menuHintButton(hint: NxButton): NxButton = if (hint == NxButton.LEFT) NxButton.RIGHT else hint

/** How far one controller UP/DOWN scrolls a message page. */
private val MessageScrollStep = 66.dp

/**
 * Screenshot preview of the chosen slot + a dot pager + the slot's label/timestamp. A sideways swipe steps the slot
 * ([slotSwipe], [onChange]); a tap saves or loads it ([onTap]), as a second tap on the highlighted Save / Load row.
 */
@Composable
private fun SlotPreview(
    slot: SlotUi?,
    slotIndex: Int,
    slotCount: Int,
    textSize: TextUnit,
    onTap: () -> Unit,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap by rememberUpdatedState(onTap)
    val thumb = slot?.thumbPath
    val aspect = slot?.aspect ?: (4f / 3f)
    val small = NxTextSizes.secondary(textSize)
    // A white "polaroid" card that hugs the screenshot (photo box uses the shot's own aspect, so no
    // black padding). The top strip shows the save's date/time; the bottom strip holds the dot pager.
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().slotSwipe(onChange).pointerInput(Unit) { detectTapGestures { tap() } }
                .clip(RoundedCornerShape(12.dp)).background(LocalNxColors.current.main)
                .padding(start = 8.dp, end = 8.dp, top = 3.dp, bottom = 5.dp),
        ) {
            if (slot?.occupied == true) {
                Text(
                    text = slot.subtitle,
                    color = Color(0xFF3A3A3A),
                    fontFamily = LocalNxFont.current,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = small,
                    lineHeight = small,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            } else {
                // No datetime for an empty slot: keep the top border thin, like the sides.
                Spacer(Modifier.height(5.dp))
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(3.dp)).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "Empty Slot",
                        color = LocalNxColors.current.main,
                        fontFamily = LocalNxFont.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = textSize,
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    repeat(slotCount) { i ->
                        if (i > 0) Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.size(if (i == slotIndex) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (i == slotIndex) Color(0xFF111111) else Color(0xFF8C8C8C)),
                        )
                    }
                }
            }
        }
    }
}
