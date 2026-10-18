package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * The virtual pad's touch geometry, kept pure so it can be tested: which d-pad directions a point presses, which button
 * a pointer is on (and keeps holding while it drifts), and how pointers add up to one Down / Up per button.
 */

/** A button's touch box, in whatever unit its caller works in (face-button units for a cluster, px once [scaled]). */
internal data class PadHitBox(val button: NxButton, val rect: Rect)

/** These boxes multiplied by [factor] (face-button units to px). */
internal fun List<PadHitBox>.scaled(factor: Float): List<PadHitBox> = map {
    PadHitBox(it.button, Rect(it.rect.left * factor, it.rect.top * factor, it.rect.right * factor, it.rect.bottom * factor))
}

/**
 * The d-pad directions a pointer at ([dx], [dy]) from the cross's centre presses (screen y grows down): none inside
 * [deadZone], else 8-way by angle, 45° sectors centred on the four arms and the four diagonals (a diagonal holds both
 * of its arms). Distance past the dead zone does not matter, so a thumb run off the cross keeps its direction.
 */
internal fun dpadDirections(dx: Float, dy: Float, deadZone: Float): Set<NxButton> {
    if (dx == 0f && dy == 0f) return emptySet()
    if (dx * dx + dy * dy < deadZone * deadZone) return emptySet()
    val sector = (atan2(dy, dx) / (PI.toFloat() / 4f)).roundToInt().mod(8)
    return when (sector) {
        0 -> setOf(NxButton.RIGHT)
        1 -> setOf(NxButton.RIGHT, NxButton.DOWN)
        2 -> setOf(NxButton.DOWN)
        3 -> setOf(NxButton.LEFT, NxButton.DOWN)
        4 -> setOf(NxButton.LEFT)
        5 -> setOf(NxButton.LEFT, NxButton.UP)
        6 -> setOf(NxButton.UP)
        else -> setOf(NxButton.RIGHT, NxButton.UP)
    }
}

/** How far [p] is from this box: 0 inside it. */
internal fun Rect.distanceTo(p: Offset): Float {
    val dx = maxOf(left - p.x, 0f, p.x - right)
    val dy = maxOf(top - p.y, 0f, p.y - bottom)
    return sqrt(dx * dx + dy * dy)
}

/**
 * The button a pointer at [p] holds, given it held [current] (null: none). Inside a box it is that box's button (the
 * held one where boxes overlap, else the nearest centre), so a thumb sliding onto a neighbour moves the hold there.
 * Off every box it keeps [current] while within [slop] of its box, so a thumb drifting off a held button does not drop
 * it; beyond that, nothing.
 */
internal fun padButtonAt(p: Offset, boxes: List<PadHitBox>, current: NxButton?, slop: Float): NxButton? {
    val held = boxes.firstOrNull { it.button == current }
    if (held != null && held.rect.contains(p)) return held.button
    boxes.filter { it.rect.contains(p) }.minByOrNull { (it.rect.center - p).getDistanceSquared() }?.let { return it.button }
    if (held != null && held.rect.distanceTo(p) <= slop) return held.button
    return null
}

/** The analog knob for a touch [touch] from the well's centre: the touch itself, clamped to [maxRadius]. */
internal fun clampKnob(touch: Offset, maxRadius: Float): Offset {
    val dist = touch.getDistance()
    return if (dist > maxRadius && dist > 0f) touch / dist * maxRadius else touch
}

/** The stick takes a direction once it is this share of its radius from the centre... */
internal const val STICK_ENTER = 0.4f

/** ...and lets go of it only below this share, so a thumb resting on the rim of the dead zone does not flicker. */
internal const val STICK_LEAVE = 0.3f

/** How far past a sector boundary (degrees) a held direction keeps holding, so a thumb on the boundary does not flicker. */
internal const val STICK_SECTOR_MARGIN_DEG = 10.0

/** The menu stick's sectors: the four dominant axes, as the selection only ever moves up, down, left or right. */
internal const val MENU_STICK_SECTORS = 4

/**
 * Which of [sectors] equal sectors the stick at ([x], [y]) (a share of its radius each, screen y down) points into, or
 * null in the dead zone. Sector 0 is centred on right and they count clockwise on screen (8: E, SE, S, SW, W, NW, N, NE;
 * 4: right, down, left, up). With hysteresis on the [previous] result: a direction is taken past [STICK_ENTER] and kept
 * down to [STICK_LEAVE], and a held sector is kept until the angle is [STICK_SECTOR_MARGIN_DEG] past its boundary.
 */
internal fun stickSector(x: Float, y: Float, previous: Int?, sectors: Int): Int? {
    val r = sqrt(x * x + y * y)
    if (r < (if (previous == null) STICK_ENTER else STICK_LEAVE)) return null
    val width = 360.0 / sectors
    val deg = (atan2(y.toDouble(), x.toDouble()) * 180.0 / PI + 360.0) % 360.0
    if (previous != null) {
        val off = abs(((deg - previous * width) % 360.0 + 540.0) % 360.0 - 180.0)   // to the held sector's centre
        if (off <= width / 2 + STICK_SECTOR_MARGIN_DEG) return previous
    }
    return ((deg / width).roundToInt()) % sectors
}

/** The menu direction of a [MENU_STICK_SECTORS] sector ([stickSector]), null for the dead zone. */
internal fun menuStickButton(sector: Int?): NxButton? = when (sector) {
    0 -> NxButton.RIGHT
    1 -> NxButton.DOWN
    2 -> NxButton.LEFT
    3 -> NxButton.UP
    else -> null
}

/**
 * Whether the on-screen stick going from sector [old] to [new] ([stickSector]) ticks the pad's haptic: only in the
 * [menu] (in game the stick is analog and stays silent), on entering a direction it did not hold, never while it holds
 * one and never on going back to the centre.
 */
internal fun menuStickTick(menu: Boolean, old: Int?, new: Int?): Boolean = menu && new != null && new != old

/**
 * Counts the pointers holding each button, so a button held by two pointers sends one Down (the first) and one Up
 * (the last to let go).
 */
internal class PadPressCount {
    private val counts = mutableMapOf<NxButton, Int>()

    /** The buttons held by at least one pointer. */
    val pressed: Set<NxButton> get() = counts.keys.toSet()

    /** One more pointer holds [button]; true when it was not held before (send Down). */
    fun press(button: NxButton): Boolean {
        val n = counts[button] ?: 0
        counts[button] = n + 1
        return n == 0
    }

    /** One pointer lets go of [button]; true when it was the last (send Up). A button not held is left alone. */
    fun release(button: NxButton): Boolean {
        val n = counts[button] ?: return false
        if (n <= 1) counts.remove(button) else counts[button] = n - 1
        return n <= 1
    }

    /** One pointer moves from holding [old] to holding [new]: releases first, then presses, [emit]ting what changed. */
    fun move(old: Set<NxButton>, new: Set<NxButton>, emit: (PadEvent) -> Unit) {
        for (b in old) if (b !in new && release(b)) emit(PadEvent.Up(b))
        for (b in new) if (b !in old && press(b)) emit(PadEvent.Down(b))
    }
}

/** The X / Y / B / A diamond's buttons and their squares' top-left corners, in face-button units, in drawing order. */
private val DIAMOND_CELLS = listOf(
    NxButton.X to Offset(1f, 0f),
    NxButton.Y to Offset(0f, 1f),
    NxButton.A to Offset(2f, 1f),
    NxButton.B to Offset(1f, 2f),
)

/**
 * The face cluster's buttons for [profile] and their touch boxes in face-button units, in drawing order: the diamond's
 * four squares, the N64's [N64Cluster.slots] or the Sega's [SegaCluster.visibleSlots]. Each box is the button's square
 * layout bounds, the same box the button is drawn in; with a [cStick] layout the diamond sits at its [CStickLayout.diamond].
 */
internal fun faceClusterHitBoxes(profile: PadProfile, cStick: CStickLayout? = null): List<PadHitBox> = when (profile.cluster) {
    FaceCluster.DIAMOND -> {
        val at = cStick?.diamond ?: Offset.Zero     // the C-stick layout drops the diamond clear of the stick
        DIAMOND_CELLS.filterNot { profile.isHiddenInCluster(it.first) }
            .map { (b, o) -> PadHitBox(b, Rect(at.x + o.x, at.y + o.y, at.x + o.x + 1f, at.y + o.y + 1f)) }
    }
    FaceCluster.N64 -> N64Cluster.slots.map { PadHitBox(it.button, Rect(it.x, it.y, it.x + it.diameter, it.y + it.diameter)) }
    FaceCluster.SEGA -> SegaCluster.visibleSlots(profile::isHiddenInCluster)
        .map { PadHitBox(it.button, Rect(it.x, it.y, it.x + it.diameter, it.y + it.diameter)) }
}

/** The C-stick's well diameter, in face-button units ([cStickLayout]). */
internal const val C_STICK_DIAMETER = 1.2f

/** The clear space kept between the C-stick's well and X's and Y's faces (radius 0.5), in face-button units. */
internal const val C_STICK_GAP = 0.35f

/** How far the diamond drops in the landscape clusters' C-stick layout, face-button units ([cStickLayout]). */
internal const val C_STICK_LANDSCAPE_DROP = 0.5f

/**
 * A face cluster with a C-stick ([PadProfile.cStick]) in face-button units: a [width] x [height] box, the X/Y/B/A diamond's
 * 3 x 3 square at [diamond] (its top-left corner) and the C-stick's square [stick] box.
 */
internal data class CStickLayout(val width: Float, val height: Float, val diamond: Offset, val stick: Rect)

/**
 * A C-stick face cluster with the diamond dropped [drop] units: the C-stick's well sits in the box's top-left corner
 * (wholly inside it, so it never reaches the shoulders above or past the box's side), and the diamond is pushed down by
 * [drop] and right by as little as keeps [C_STICK_GAP] between the well and both X's and Y's faces (and the stick's
 * square touch box out of theirs). The box grows by both, so the stick takes room the cluster owns rather than poking
 * out of it.
 */
internal fun cStickLayout(drop: Float): CStickLayout {
    val r = C_STICK_DIAMETER / 2f
    val reach = r + 0.5f + C_STICK_GAP + 0.002f            // centre to centre, a hair over so rounding keeps the gap
    fun across(down: Float) = sqrt(maxOf(0f, reach * reach - down * down))
    // X's centre is (e + 1.5, drop + 0.5), Y's (e + 0.5, drop + 1.5), the stick's (r, r): both right of it by `across`.
    var shift = maxOf(0f, across(drop + 0.5f - r) - (1.5f - r), across(drop + 1.5f - r) - (0.5f - r))
    // Keep the stick's square touch box out of Y's (and so X's) too, so it never takes their touches.
    if (drop + 1f < 2 * r) shift = maxOf(shift, 2 * r)
    return CStickLayout(3f + shift, 3f + drop, Offset(shift, drop), Rect(0f, 0f, 2 * r, 2 * r))
}

/** The clear space between the C-stick's well and the nearer of X's and Y's faces in [layout], face-button units. */
internal fun cStickGap(layout: CStickLayout): Float {
    val c = layout.stick.center
    val r = layout.stick.width / 2f
    val d = layout.diamond
    return listOf(Offset(d.x + 1.5f, d.y + 0.5f), Offset(d.x + 0.5f, d.y + 1.5f))
        .minOf { (it - c).getDistance() } - r - 0.5f
}

/**
 * Whether one pointer going from holding [old] to holding [new] ticks the pad's haptic: on its press and on every new
 * thing it holds (a thumb sliding onto another button, the d-pad rolling to another of its 8 directions, a diagonal
 * included), never while it holds the same, and never on letting go (back to neutral, off every button). Judged per
 * pointer, so a second finger landing on a button ticks even while the first holds.
 */
internal fun padHapticTick(old: Set<NxButton>, new: Set<NxButton>): Boolean = new.isNotEmpty() && new != old

/** [padHapticTick] for the pointers of one event, as (old, new) pairs: one tick at most however many pressed. */
internal fun padHapticTick(moves: List<Pair<Set<NxButton>, Set<NxButton>>>): Boolean = moves.any { (o, n) -> padHapticTick(o, n) }
