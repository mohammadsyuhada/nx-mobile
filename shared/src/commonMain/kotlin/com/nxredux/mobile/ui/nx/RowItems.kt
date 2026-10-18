package com.nxredux.mobile.ui.nx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

/*
 * The Carousel / Backdrop rows' and stacks' selection plumbing, kept light for scrolling at 120 Hz. The selection and the
 * caption reach these layouts as lambdas, read only in small scopes: [FollowSelection] (the slide), the item loop (which
 * item is lit, which are composed) and the caption. A selection step then recomposes those and the two items whose lit
 * state changed, never the whole row, its geometry or its untouched items; the slide itself is read in layers only.
 */

/** [selectedIndex] now, kept in `0 until count`, read without subscribing the caller (a row's starting position). */
internal fun selectionNow(selectedIndex: () -> Int, count: Int): Int =
    Snapshot.withoutReadObservation { selectedIndex() }.coerceIn(0, maxOf(0, count - 1))

/**
 * Eases [pos] to the selection ([ms], [NxMenuEasing]) whenever it changes; a swipe's release already settling there
 * keeps its spring ([easeUnlessSettling]). Its own scope: only this recomposes on a selection step.
 */
@Composable
internal fun FollowSelection(pos: Animatable<Float, AnimationVector1D>, selectedIndex: () -> Int, count: Int, ms: Int) {
    val sel = selectedIndex().coerceIn(0, maxOf(0, count - 1))
    LaunchedEffect(pos, sel) { pos.easeUnlessSettling(sel.toFloat(), tween(ms, easing = NxMenuEasing)) }
}

/** The position a row or stack shows: the finger's while it drags ([dragPos] not NaN), else the animated [pos]. */
@Composable
internal fun rememberShownPosition(pos: Animatable<Float, AnimationVector1D>, dragPos: MutableFloatState): () -> Float =
    remember(pos, dragPos) { { dragPos.floatValue.takeUnless { it.isNaN() } ?: pos.value } }

/**
 * A row's or stack's item taps, reading the selection and the callbacks when the tap lands (one object for the whole
 * row, so an item never recomposes for a new callback): [tap] the one tap path ([nxTap]), [swipeSelect] a swipe's
 * release (selects only another item), [longPress].
 */
@Stable
internal class ItemTaps(
    private val selected: State<() -> Int>, private val count: State<Int>,
    private val onSelect: State<(Int) -> Unit>, private val onOpen: State<(Int) -> Unit>,
    private val opens: State<(Int) -> Boolean>, private val onLongPress: State<((Int) -> Unit)?>,
) {
    /** The selection now, in the list. */
    fun current(): Int = selected.value().coerceIn(0, maxOf(0, count.value - 1))
    fun tap(i: Int) = nxTap(i, i == current(), opens.value(i), onSelect.value, onOpen.value)
    fun swipeSelect(i: Int) { if (i != current()) onSelect.value(i) }
    fun longPress(i: Int) { onLongPress.value?.invoke(i) }
}

@Composable
internal fun rememberItemTaps(
    selectedIndex: () -> Int, count: Int, onSelect: (Int) -> Unit, onOpen: (Int) -> Unit, onLongPress: ((Int) -> Unit)?,
): ItemTaps {
    val selected = rememberUpdatedState(selectedIndex)
    val n = rememberUpdatedState(count)
    val select = rememberUpdatedState(onSelect)
    val open = rememberUpdatedState(onOpen)
    val opens = rememberUpdatedState(LocalNxTapOpens.current)
    val longPress = rememberUpdatedState(onLongPress)
    return remember { ItemTaps(selected, n, select, open, opens, longPress) }
}

/**
 * A row caption's drawn height as it glides (the selected item's top follows it): the caption composable owns the
 * animation ([anim], set when it first composes) and the items read [value] while placing, never in composition.
 */
@Stable
internal class CaptionGlide {
    var anim: Animatable<Float, AnimationVector1D>? by mutableStateOf(null)
    fun value(): Float = anim?.value ?: 0f
}

/**
 * The caption's glide in [glide]: starts at [drawn] (a new [glide], with a new list or screen, snaps) and animates to
 * each new [drawn] over [ms] ([NxMenuEasing]), with the row's slide.
 */
@Composable
internal fun GlideCaptionHeight(glide: CaptionGlide, drawn: Float, ms: Int) {
    val capH = remember(glide) { Animatable(drawn).also { glide.anim = it } }
    LaunchedEffect(capH, drawn) { capH.animateTo(drawn, tween(ms, easing = NxMenuEasing)) }
}

/**
 * The composition slot of each index in [window], recycling the slots of indices that left: an index kept keeps its
 * slot, an index that comes in takes a slot one that left gave up (the lowest first), else a new one. A row keys its
 * items by slot, so a step moves a slot from the item leaving at one end to the item coming in at the other: that item
 * recomposes with its new data instead of a whole item being disposed and another built (for scrolling at 120 Hz).
 */
internal fun itemSlots(previous: Map<Int, Int>, window: IntRange): Map<Int, Int> {
    val out = HashMap<Int, Int>()
    for ((i, slot) in previous) if (i in window) out[i] = slot
    val free = previous.values.filter { it !in out.values }.sorted().toMutableList()
    var next = (previous.values.maxOrNull() ?: -1) + 1
    for (i in window) if (i !in out) out[i] = if (free.isNotEmpty()) free.removeAt(0) else next++
    return out
}

/** A row's [itemSlots], kept across compositions. */
internal class ItemSlots {
    private var slots: Map<Int, Int> = emptyMap()
    fun of(window: IntRange): Map<Int, Int> = itemSlots(slots, window).also { slots = it }
}
