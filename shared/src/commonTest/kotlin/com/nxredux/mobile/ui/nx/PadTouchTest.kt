package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PadTouchTest {
    private val up = NxButton.UP
    private val down = NxButton.DOWN
    private val left = NxButton.LEFT
    private val right = NxButton.RIGHT

    /** The direction set at [degrees] (0 = right, 90 = down: screen y grows down) and [r] from the centre. */
    private fun at(degrees: Double, r: Float = 100f, dead: Float = 20f): Set<NxButton> {
        val rad = degrees * kotlin.math.PI / 180.0
        return dpadDirections((cos(rad) * r).toFloat(), (sin(rad) * r).toFloat(), dead)
    }

    // ---- d-pad: 8-way by angle ----

    @Test fun dpadCardinals() {
        assertEquals(setOf(right), at(0.0))
        assertEquals(setOf(down), at(90.0))
        assertEquals(setOf(left), at(180.0))
        assertEquals(setOf(up), at(270.0))
    }

    @Test fun dpadDiagonalsPressTwoArms() {
        assertEquals(setOf(right, down), at(45.0))
        assertEquals(setOf(left, down), at(135.0))
        assertEquals(setOf(left, up), at(225.0))
        assertEquals(setOf(right, up), at(315.0))
    }

    @Test fun dpadSectorsAre45DegreesWide() {
        assertEquals(setOf(right), at(20.0))
        assertEquals(setOf(right, down), at(25.0))
        assertEquals(setOf(right, down), at(65.0))
        assertEquals(setOf(down), at(70.0))
        assertEquals(setOf(up), at(-70.0))
        assertEquals(setOf(right, up), at(-25.0))
    }

    @Test fun dpadDeadZoneAtTheCentre() {
        assertEquals(emptySet(), dpadDirections(0f, 0f, 20f))
        assertEquals(emptySet(), at(45.0, r = 19f))
        assertEquals(setOf(right, down), at(45.0, r = 21f))
        assertEquals(emptySet(), dpadDirections(0f, 0f, 0f))   // no direction even without a dead zone
    }

    @Test fun dpadRollingUpToRightPassesThroughTheDiagonal() {
        val path = (270..360 step 5).map { at(it.toDouble()) }
        assertEquals(setOf(up), path.first())
        assertTrue(setOf(right, up) in path)
        assertEquals(setOf(right), path.last())
    }

    @Test fun dpadFarOutsideTheCrossStillHasADirection() {
        assertEquals(setOf(left), at(180.0, r = 10_000f))
    }

    // ---- buttons: the one under the pointer, held within a slop ----

    // The X / Y / B / A diamond in a 3 x 3 square of 100 px buttons.
    private val diamond = listOf(
        PadHitBox(NxButton.X, Rect(100f, 0f, 200f, 100f)),
        PadHitBox(NxButton.Y, Rect(0f, 100f, 100f, 200f)),
        PadHitBox(NxButton.A, Rect(200f, 100f, 300f, 200f)),
        PadHitBox(NxButton.B, Rect(100f, 200f, 200f, 300f)),
    )

    @Test fun buttonUnderThePointer() {
        assertEquals(NxButton.B, padButtonAt(Offset(150f, 250f), diamond, null, 50f))
        assertEquals(NxButton.A, padButtonAt(Offset(250f, 150f), diamond, null, 50f))
        assertNull(padButtonAt(Offset(150f, 150f), diamond, null, 50f))   // the diamond's empty middle, nothing held
    }

    @Test fun aHeldButtonStaysHeldWhileTheThumbDriftsWithinTheSlop() {
        // B held; the thumb drifts below the cluster, 40 px past B's edge.
        assertEquals(NxButton.B, padButtonAt(Offset(150f, 340f), diamond, NxButton.B, 50f))
        // Into the empty middle of the diamond: still within B's slop.
        assertEquals(NxButton.B, padButtonAt(Offset(150f, 170f), diamond, NxButton.B, 50f))
    }

    @Test fun aHeldButtonReleasesBeyondTheSlop() {
        assertNull(padButtonAt(Offset(150f, 360f), diamond, NxButton.B, 50f))
    }

    @Test fun slidingOntoANeighbourMovesTheHold() {
        // B held, the thumb rolls up-right onto A.
        assertEquals(NxButton.B, padButtonAt(Offset(190f, 210f), diamond, NxButton.B, 50f))
        assertEquals(NxButton.A, padButtonAt(Offset(210f, 190f), diamond, NxButton.B, 50f))
    }

    @Test fun theHeldButtonWinsWhereBoxesOverlap() {
        val overlap = listOf(
            PadHitBox(NxButton.A, Rect(0f, 0f, 100f, 100f)),
            PadHitBox(NxButton.B, Rect(80f, 0f, 180f, 100f)),
        )
        assertEquals(NxButton.A, padButtonAt(Offset(95f, 50f), overlap, NxButton.A, 10f))
        assertEquals(NxButton.B, padButtonAt(Offset(85f, 50f), overlap, NxButton.B, 10f))
        // Nothing held: the nearer centre.
        assertEquals(NxButton.B, padButtonAt(Offset(95f, 50f), overlap, null, 10f))
    }

    @Test fun distanceToABox() {
        val r = Rect(0f, 0f, 10f, 10f)
        assertEquals(0f, r.distanceTo(Offset(5f, 5f)))
        assertEquals(3f, r.distanceTo(Offset(13f, 5f)))
        assertEquals(5f, r.distanceTo(Offset(-3f, -4f)))
    }

    // ---- the press counter: Down / Up once per button whatever the pointers ----

    @Test fun twoPointersOnOneButtonEmitOneDownAndOneUp() {
        val c = PadPressCount()
        assertTrue(c.press(NxButton.A))
        assertFalse(c.press(NxButton.A))
        assertEquals(setOf(NxButton.A), c.pressed)
        assertFalse(c.release(NxButton.A))
        assertTrue(c.release(NxButton.A))
        assertEquals(emptySet(), c.pressed)
    }

    @Test fun twoThumbsTwoButtons() {
        val c = PadPressCount()
        assertTrue(c.press(NxButton.A))
        assertTrue(c.press(NxButton.B))
        assertEquals(setOf(NxButton.A, NxButton.B), c.pressed)
        assertTrue(c.release(NxButton.A))
        assertEquals(setOf(NxButton.B), c.pressed)
    }

    @Test fun releasingAButtonNotHeldIsANoOp() {
        val c = PadPressCount()
        assertFalse(c.release(NxButton.A))
        assertEquals(emptySet(), c.pressed)
    }

    @Test fun moveSwapsOnlyWhatChanged() {
        val c = PadPressCount()
        val log = mutableListOf<PadEvent>()
        c.move(emptySet(), setOf(up), log::add)
        c.move(setOf(up), setOf(up, right), log::add)
        c.move(setOf(up, right), setOf(right), log::add)
        c.move(setOf(right), emptySet(), log::add)
        assertEquals(
            listOf(PadEvent.Down(up), PadEvent.Down(right), PadEvent.Up(up), PadEvent.Up(right)),
            log,
        )
    }

    // ---- face clusters' touch boxes ----

    @Test fun diamondBoxesAreTheFourSquaresOfTheCross() {
        val boxes = faceClusterHitBoxes(PadProfile.STANDARD).associate { it.button to it.rect }
        assertEquals(Rect(1f, 0f, 2f, 1f), boxes[NxButton.X])
        assertEquals(Rect(0f, 1f, 1f, 2f), boxes[NxButton.Y])
        assertEquals(Rect(2f, 1f, 3f, 2f), boxes[NxButton.A])
        assertEquals(Rect(1f, 2f, 2f, 3f), boxes[NxButton.B])
    }

    @Test fun n64BoxesAreItsSlots() {
        val boxes = faceClusterHitBoxes(PadProfile.N64)
        assertEquals(N64Cluster.slots.map { it.button }, boxes.map { it.button })
        for ((slot, box) in N64Cluster.slots.zip(boxes)) {
            assertEquals(Rect(slot.x, slot.y, slot.x + slot.diameter, slot.y + slot.diameter), box.rect)
        }
    }

    @Test fun boxesScaleToPixels() {
        val px = faceClusterHitBoxes(PadProfile.STANDARD).scaled(100f).first { it.button == NxButton.A }
        assertEquals(Rect(200f, 100f, 300f, 200f), px.rect)
    }

    // ---- analog stick ----

    @Test fun knobFollowsTheTouchInsideTheRadius() {
        assertEquals(Offset(30f, -40f), clampKnob(Offset(30f, -40f), 100f))
    }

    @Test fun knobClampsToTheRadius() {
        val k = clampKnob(Offset(300f, 400f), 100f)
        assertTrue(abs(k.x - 60f) < 1e-3f && abs(k.y - 80f) < 1e-3f, "got $k")
    }

    // ---- the stick's menu sectors: direction by angle, with hysteresis round each boundary and the dead zone ----

    /** The sector of [sectors] at [degrees] (0 = right, 90 = down) and [r] (share of the radius), [previous] held. */
    private fun sector(degrees: Double, r: Float, previous: Int?, sectors: Int = 8): Int? {
        val rad = degrees * kotlin.math.PI / 180.0
        return stickSector((cos(rad) * r).toFloat(), (sin(rad) * r).toFloat(), previous, sectors)
    }

    @Test fun stickSectorsAreEightByAngleClockwiseFromRight() {
        // 0 E, 1 SE, 2 S, 3 SW, 4 W, 5 NW, 6 N, 7 NE (screen y grows down).
        for (i in 0 until 8) assertEquals(i, sector(i * 45.0, 1f, null), "at ${i * 45}°")
        assertEquals(0, sector(-20.0, 1f, null))   // just above right is still right
        assertEquals(7, sector(-25.0, 1f, null))   // past the 22.5° boundary: up-right
    }

    @Test fun stickSectorsAreFourDominantAxesInTheMenu() {
        assertEquals(right, menuStickButton(sector(40.0, 1f, null, MENU_STICK_SECTORS)))
        assertEquals(down, menuStickButton(sector(50.0, 1f, null, MENU_STICK_SECTORS)))
        assertEquals(left, menuStickButton(sector(180.0, 1f, null, MENU_STICK_SECTORS)))
        assertEquals(up, menuStickButton(sector(270.0, 1f, null, MENU_STICK_SECTORS)))
        assertNull(menuStickButton(null))
    }

    @Test fun stickEntersADirectionOnlyPastTheEnterThreshold() {
        assertNull(sector(0.0, STICK_ENTER - 0.01f, null))
        assertEquals(0, sector(0.0, STICK_ENTER + 0.01f, null))
    }

    @Test fun stickLeavesToNeutralOnlyBelowTheLeaveThreshold() {
        // Between the two thresholds a held direction stays held (no chatter at the dead zone's rim).
        val mid = (STICK_ENTER + STICK_LEAVE) / 2
        assertEquals(0, sector(0.0, mid, 0))
        assertEquals(0, sector(0.0, STICK_LEAVE + 0.01f, 0))
        assertNull(sector(0.0, STICK_LEAVE - 0.01f, 0))
        assertNull(sector(0.0, mid, null))   // and neutral stays neutral there
    }

    @Test fun stickHoldsItsSectorWithinTheAngularMargin() {
        // E/SE boundary at 22.5°: held E stays E until the margin is passed, and likewise back.
        assertEquals(0, sector(22.5 + STICK_SECTOR_MARGIN_DEG - 1, 1f, 0))
        assertEquals(1, sector(22.5 + STICK_SECTOR_MARGIN_DEG + 1, 1f, 0))
        assertEquals(1, sector(22.5 - STICK_SECTOR_MARGIN_DEG + 1, 1f, 1))
        assertEquals(0, sector(22.5 - STICK_SECTOR_MARGIN_DEG - 1, 1f, 1))
        // Across the wrap at 0°/360°: held NE (315°) round to E.
        assertEquals(7, sector(337.5 + STICK_SECTOR_MARGIN_DEG - 1, 1f, 7))
        assertEquals(0, sector(337.5 + STICK_SECTOR_MARGIN_DEG + 1, 1f, 7))
        assertEquals(0, sector(-(22.5 + STICK_SECTOR_MARGIN_DEG - 1), 1f, 0))
    }

    @Test fun stickHoveringOnABoundaryDoesNotChatter() {
        var held: Int? = null
        var ticks = 0
        // A thumb wobbling ±5° round the E/SE boundary and ±0.04 round the enter threshold.
        val path = listOf(0.0 to 0.2f, 0.0 to 0.9f) + (0 until 20).map { i -> (22.5 + if (i % 2 == 0) 5.0 else -5.0) to 0.9f } +
            (0 until 20).map { i -> 22.5 to (STICK_ENTER + if (i % 2 == 0) 0.04f else -0.04f) }
        for ((deg, r) in path) {
            val next = sector(deg, r, held)
            if (menuStickTick(menu = true, old = held, new = next)) ticks++
            held = next
        }
        assertEquals(1, ticks)   // the one entry into E
    }

    @Test fun stickTicksOnEachNewDirectionNotOnNeutralNorWhileHeld() {
        assertTrue(menuStickTick(menu = true, old = null, new = 0))   // out of neutral
        assertTrue(menuStickTick(menu = true, old = 0, new = 1))      // round to the next
        assertFalse(menuStickTick(menu = true, old = 1, new = 1))     // held
        assertFalse(menuStickTick(menu = true, old = 1, new = null))  // back to the centre
        assertFalse(menuStickTick(menu = true, old = null, new = null))
    }

    @Test fun stickNeverTicksInGame() {
        assertFalse(menuStickTick(menu = false, old = null, new = 0))
        assertFalse(menuStickTick(menu = false, old = 0, new = 1))
    }

    @Test fun stickArcTicksOncePerSectorPassed() {
        // Most of a circle at the rim from right (stopping short of right again): one tick per sector entered.
        for ((n, expected) in listOf(8 to 8, 4 to 4)) {
            var held: Int? = null
            var ticks = 0
            for (deg in 0..320 step 3) {
                val next = sector(deg.toDouble(), 1f, held, n)
                if (menuStickTick(menu = true, old = held, new = next)) ticks++
                held = next
            }
            assertEquals(expected, ticks, "$n sectors")
        }
    }

    // ---- haptics: a tick on a pointer's press edge and on each new d-pad direction, none on letting go ----

    @Test fun hapticTicksOnAPress() {
        assertTrue(padHapticTick(emptySet(), setOf(NxButton.A)))
        assertTrue(padHapticTick(emptySet(), setOf(up)))
    }

    @Test fun hapticStaysQuietWhileHeldAndOnRelease() {
        assertFalse(padHapticTick(setOf(NxButton.A), setOf(NxButton.A)))
        assertFalse(padHapticTick(setOf(NxButton.A), emptySet()))
        assertFalse(padHapticTick(emptySet(), emptySet()))
    }

    @Test fun hapticTicksOnEveryNewDpadDirectionIncludingDiagonals() {
        assertTrue(padHapticTick(setOf(up), setOf(up, right)))      // into the diagonal
        assertTrue(padHapticTick(setOf(up, right), setOf(right)))   // out of it onto an arm: a new direction
        assertTrue(padHapticTick(setOf(up), setOf(down)))
        assertFalse(padHapticTick(setOf(up, right), setOf(right, up)))   // the same diagonal
        assertFalse(padHapticTick(setOf(up, right), emptySet()))          // back to neutral
    }

    @Test fun hapticTicksWhenAThumbSlidesOntoAnotherButton() {
        assertTrue(padHapticTick(setOf(NxButton.B), setOf(NxButton.A)))
    }

    @Test fun hapticTicksForASecondFingerOnAnotherButtonOnly() {
        // Pointers are judged one at a time: the first finger holding A stays quiet, the second landing on B ticks.
        val first = setOf(NxButton.A)
        assertFalse(padHapticTick(first, first))
        assertTrue(padHapticTick(emptySet(), setOf(NxButton.B)))
        // Two pointers moving in one event tick once between them.
        assertTrue(padHapticTick(listOf(first to first, emptySet<NxButton>() to setOf(NxButton.B))))
        assertFalse(padHapticTick(listOf(first to first, setOf(NxButton.B) to emptySet())))
    }
}
