package com.nxredux.mobile.ui.nx

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwipeTest {
    @Test fun swipeDragReleaseHandsTheDragOverOnce() {
        val live = mutableFloatStateOf(Float.NaN)
        val d = SwipeDrag(live)
        val released = mutableListOf<Triple<Float, Float, Float>>()
        d.release(0f) { a, b, c -> released += Triple(a, b, c) }
        assertTrue(released.isEmpty())      // no drag: nothing to release
        d.begin(resting = 3f)
        assertEquals(3f, live.floatValue)
        d.move(-50f, toValue = { it * 0.01f }, lo = 0f, hi = 9f, give = 0.5f)
        assertEquals(2.5f, live.floatValue, 0.0001f)
        d.release(-800f) { a, b, c -> released += Triple(a, b, c) }
        assertEquals(listOf(Triple(3f, -50f, -800f)), released)
        d.release(0f) { a, b, c -> released += Triple(a, b, c) }
        assertEquals(1, released.size)      // only once
        assertEquals(null, d.cancel())      // and a later stop has nothing to cancel
        // A drag begun while the last release still settles starts from what is shown, as a new gesture.
        val g0 = d.gesture
        d.begin(resting = 9f)
        assertEquals(2.5f, d.start)
        assertEquals(g0 + 1, d.gesture)
    }

    @Test fun swipeDragCancelClearsWithoutSelecting() {
        // A tab switch or rotation mid-drag stops the detector: the drag clears and nothing is selected.
        val live = mutableFloatStateOf(Float.NaN)
        val d = SwipeDrag(live)
        var selected = 0
        d.begin(resting = 3f)
        d.move(-50f, toValue = { it * 0.01f }, lo = 0f, hi = 9f, give = 0.5f)
        assertEquals(2.5f, d.cancel()!!, 0.0001f)   // what it showed, for easing back
        assertTrue(live.floatValue.isNaN())
        d.release(0f) { _, _, _ -> selected++ }      // a lift after the cancel is not a release
        assertEquals(0, selected)
        assertEquals(null, d.cancel())
    }

    // 340 wide, 0.62 scale, 16 gap: one step is 170 + 16 + 105.4 = 291.4 dp.
    private val r = RowGeometry(itemW = 340f, itemH = 240f, scale = 0.62f, gap = 16f, top = 20f)

    @Test fun rowStepIsRowItemsFirstStep() {
        assertEquals(291.4f, rowSwipeStep(r), 0.01f)
        assertEquals(rowItem(RowStyle.CAROUSEL, 1f, r).dx, rowSwipeStep(r), 0.01f)
    }

    @Test fun rowWithoutMovementRoundsThePosition() {
        assertEquals(4, rowSwipeTarget(4f, 0f, 0f, 10, r))
        assertEquals(4, rowSwipeTarget(4.3f, 0f, 0f, 10, r))   // mid-slide
    }

    @Test fun rowShortDrag() {
        assertEquals(4, rowSwipeTarget(4f, -100f, 0f, 10, r))  // 0.34 of a step
        assertEquals(5, rowSwipeTarget(4f, -160f, 0f, 10, r))  // 0.55: the next item
        assertEquals(3, rowSwipeTarget(4f, 160f, 0f, 10, r))   // right: the previous item
    }

    @Test fun rowLongDrag() {
        // Past the first step the items are 226.8 apart (340 × 0.62 + 16): 600 dp is 1 + 308.6 / 226.8 = 2.36 items.
        assertEquals(6, rowSwipeTarget(4f, -600f, 0f, 10, r))
        assertEquals(0, rowSwipeTarget(4f, 900f, 0f, 10, r))   // 1 + 608.6 / 226.8 = 3.68 items
    }

    // A drag moves the row by the items' real positions (rowItem): the item that was centred stays under the finger,
    // the first step 291.4 dp, each later one 226.8.
    @Test fun rowDragFollowsTheItemsPositions() {
        assertEquals(0f, rowSwipeDelta(0f, r), 1e-6f)
        assertEquals(1f, rowSwipeDelta(-291.4f, r), 1e-4f)
        assertEquals(2f, rowSwipeDelta(-291.4f - 226.8f, r), 1e-4f)
        assertEquals(-3f, rowSwipeDelta(291.4f + 2 * 226.8f, r), 1e-4f)
        for (g in listOf(r, RowGeometry(itemW = 330f, itemH = 130f, scale = 0.45f, gap = 28f, top = 92f))) {   // and the Consoles row
            for (drag in listOf(-50f, -291.4f, -400f, -700f, 120f, 500f, 1000f)) {
                val moved = rowSwipeDelta(drag, g)
                assertEquals(drag, rowItem(RowStyle.BACKDROP, -moved, g).dx, 0.01f)   // the started-centre item, under the finger
            }
        }
    }

    // A tapered row (a Carousel's): the drag still keeps the started-centre item under the finger, steps closer
    // out, and the settle's rate is the step's own.
    @Test fun aTaperedRowDragFollowsTheItemsPositions() {
        val t = r.copy(scale = 0.75f, taper = 0.8f)
        for (drag in listOf(-50f, -300f, -500f, -700f, -900f, -1500f, 120f, 640f, 2000f)) {
            val moved = rowSwipeDelta(drag, t)
            assertEquals(drag, rowItem(RowStyle.CAROUSEL, -moved, t).dx, 0.01f)
        }
        assertEquals(2f, rowSwipeDelta(-t.restDx(2f), t), 1e-4f)
        // The rate is the row's own there: a dp more of drag, that much more position.
        val at = -(t.restDx(2f) + 1f)
        assertEquals(rowSwipeDelta(at - 0.5f, t) - rowSwipeDelta(at + 0.5f, t), -rowSwipeSlope(at, t), 1e-4f)
    }

    // The Consoles row (330, 0.45, gap 28): one first step 165 + 28 + 74.25 = 267.25, then 176.5 per console; a slow
    // drag of 443.75 dp lands two consoles on.
    @Test fun aSlowDragOnConsolesReachesTheSecondConsoleAtItsPosition() {
        val consoles = RowGeometry(itemW = 330f, itemH = 130f, scale = 0.45f, gap = 28f, top = 92f)
        assertEquals(2f, rowSwipeDelta(-443.75f, consoles), 1e-3f)
        assertEquals(6, rowSwipeTarget(4f, -443.75f, 0f, 10, consoles))
    }

    @Test fun swipeDragMovesByItsMapping() {
        val live = mutableFloatStateOf(Float.NaN)
        val d = SwipeDrag(live)
        d.begin(resting = 4f)
        d.move(-291.4f - 226.8f, toValue = { rowSwipeDelta(it, r) }, lo = 0f, hi = 9f, give = 0.5f)
        assertEquals(6f, live.floatValue, 1e-3f)
    }

    @Test fun rowFlingProjectsFurther() {
        // (100 + 2000 × 0.25) / 291.4 = 2.06 steps.
        assertEquals(6, rowSwipeTarget(4f, -100f, -2000f, 10, r))
        assertEquals(2, rowSwipeTarget(4f, 100f, 2000f, 10, r))
    }

    @Test fun rowClampsAtBothEnds() {
        assertEquals(0, rowSwipeTarget(1f, 2000f, 0f, 10, r))
        assertEquals(0, rowSwipeTarget(0f, 50f, 3000f, 10, r))
        assertEquals(9, rowSwipeTarget(8f, -5000f, 0f, 10, r))
        assertEquals(9, rowSwipeTarget(9f, -50f, -3000f, 10, r))
    }

    @Test fun rowOneItem() {
        assertEquals(0, rowSwipeTarget(0f, 0f, 0f, 1, r))
        assertEquals(0, rowSwipeTarget(0f, -800f, -3000f, 1, r))
        assertEquals(0, rowSwipeTarget(0f, 800f, 3000f, 1, r))
    }

    @Test fun rubberHoldsTheRangeAndResistsPastIt() {
        assertEquals(5f, swipeRubber(5f, 0f, 10f, 2f))
        val past = swipeRubber(14f, 0f, 10f, 2f)
        assertTrue(past > 10f && past < 12f)
        assertTrue(swipeRubber(100f, 0f, 10f, 2f) < 12f)
        assertTrue(swipeRubber(20f, 0f, 10f, 2f) > past)      // keeps moving, less and less
        val before = swipeRubber(-4f, 0f, 10f, 2f)
        assertTrue(before < 0f && before > -2f)
        assertEquals(10f, swipeRubber(14f, 0f, 10f, 0f))      // no give: a hard clamp
    }

    @Test fun settleKeepsTheFingersVelocity() {
        // A release 2 items short of its rest at 3 items/s: the spring starts at the finger's speed, not a fresh ease's.
        assertEquals(3f, swipeSettleVelocity(0f, 2f, 3f, 12f))
        assertEquals(-3f, swipeSettleVelocity(2f, 0f, -3f, 12f))
        assertEquals(-1f, swipeSettleVelocity(0f, 2f, -1f, 12f))   // a rest behind the finger: it still leaves at its speed
    }

    @Test fun settleCapsTheVelocitySoTheSpringCannotOvershoot() {
        assertEquals(2f, swipeSettleVelocity(0f, 0.1f, 50f, 20f), 1e-4f)
        assertEquals(-2f, swipeSettleVelocity(0.1f, 0f, -50f, 20f), 1e-4f)
        assertEquals(-2f, swipeSettleVelocity(0f, 0.1f, -50f, 20f), 1e-4f)
        assertEquals(0f, swipeSettleVelocity(1f, 1f, 20f, 20f))    // already at rest (an end): nothing to carry
    }

    @Test fun settleFrequencyLetsTheSpringStartWithoutSurging() {
        assertEquals(16f, swipeSettleOmega(0f, 0.5f, 4f, 800f))           // 2 × 4 / 0.5
        assertEquals(16f, swipeSettleOmega(0.5f, 0f, -4f, 800f))
        assertEquals(SWIPE_SETTLE_OMEGA_MIN, swipeSettleOmega(0f, 2f, 3f, 600f))    // far ahead of a moving finger: the softest
        assertEquals(SWIPE_SETTLE_OMEGA_MAX, swipeSettleOmega(0f, 0.1f, 9f, 1700f)) // close to a quick one: the stiffest
        assertEquals(SWIPE_SETTLE_OMEGA_MIN, swipeSettleOmega(0f, 1f, -5f, 900f))   // moving away: the floor
        assertEquals(SWIPE_SETTLE_OMEGA_MIN, swipeSettleOmega(1f, 1f, 5f, 900f))    // already there: the floor
    }

    @Test fun settleFloorStiffensAsTheFingerSlows() {
        assertEquals(SWIPE_SETTLE_OMEGA_MAX, swipeSettleOmega(0f, 0.4f, 0f, 0f))    // a still finger: a quick snap
        assertEquals((SWIPE_SETTLE_OMEGA_MIN + SWIPE_SETTLE_OMEGA_MAX) / 2, swipeSettleOmega(0f, 2f, 0.1f, SWIPE_SETTLE_SLOW_DP / 2), 1e-4f)
        assertEquals(SWIPE_SETTLE_OMEGA_MIN, swipeSettleOmega(0f, 2f, 0.1f, SWIPE_SETTLE_SLOW_DP))
        assertEquals(SWIPE_SETTLE_OMEGA_MIN, swipeSettleOmega(0f, 2f, 0.1f, -3 * SWIPE_SETTLE_SLOW_DP))
    }

    // v in value units / s; the finger's dp/s taken as v × 190 (a row item's step), enough to pick the floor.
    private fun settle(from: Float, to: Float, v: Float, speedDp: Float = v * 190f): Pair<TargetBasedAnimation<Float, AnimationVector1D>, Float> {
        val omega = swipeSettleOmega(from, to, v, speedDp)
        val start = swipeSettleVelocity(from, to, v, omega)
        return TargetBasedAnimation(swipeSettleSpec(omega, 0.01f), Float.VectorConverter, from, to, start) to start
    }

    @Test fun settleSpringStartsAtTheVelocityAndNeverPassesTheRest() {
        for ((from, to, v) in listOf(Triple(0.3f, 2f, 4f), Triple(0f, 1f, 40f), Triple(-120f, -600f, -3000f), Triple(1.9f, 2f, 9f), Triple(0.95f, 2f, 3.4f))) {
            val (a, start) = settle(from, to, v, if (abs(v) > 100f) v else v * 190f)
            assertEquals(start, a.getVelocityVectorFromNanos(0L).value, abs(start) * 1e-3f + 1e-3f)
            var t = 0L
            while (!a.isFinishedFromNanos(t)) {
                val x = a.getValueFromNanos(t)
                assertTrue(if (to >= from) x <= to + 1e-3f else x >= to - 1e-3f, "passed $to at $x ($from, $v)")
                t += 4_000_000L
            }
            // Visibly there within 0.7 s (the rest is the sub-threshold tail), and finished.
            assertTrue(abs(a.getValueFromNanos(700_000_000L) - to) <= abs(to - from) * 0.02f, "not settled by 0.7 s ($from, $v)")
            assertTrue(t < 2_000_000_000L, "finishes, took ${t / 1e6} ms")
            assertEquals(to, a.getValueFromNanos(t), 1e-3f)
        }
    }

    @Test fun settleNeverOutrunsAFingerItCanFollow() {
        // Where 2 × speed / distance is within the frequencies, the row only slows from the finger's speed.
        for ((from, to, v) in listOf(Triple(0f, 0.5f, 4f), Triple(0.95f, 1.7f, 3.4f), Triple(-120f, -600f, -3000f))) {
            val (a, start) = settle(from, to, v, if (abs(v) > 100f) v else v * 190f)
            var t = 0L
            while (!a.isFinishedFromNanos(t)) {
                assertTrue(abs(a.getVelocityVectorFromNanos(t).value) <= abs(start) * 1.001f, "surged at ${t / 1e6} ms ($from, $to, $v)")
                t += 2_000_000L
            }
        }
    }

    @Test fun settleFromRestTakesAboutTheSlidesTime() {
        // A still finger: the stiffest spring, near the d-pad's 0.26 / 0.3 s ease rather than a slow drift.
        val (a, _) = settle(0f, 1f, 0f)
        assertTrue(a.getValueFromNanos(250_000_000L) > 0.95f)
        assertTrue(a.durationNanos in 300_000_000L..900_000_000L, "${a.durationNanos / 1e6} ms")
    }

    @Test fun rubberSlopeIsOneInsideAndFallsOffPastAnEnd() {
        assertEquals(1f, swipeRubberSlope(5f, 0f, 10f, 2f))
        assertEquals(1f, swipeRubberSlope(10f, 0f, 10f, 2f))
        assertEquals(0.25f, swipeRubberSlope(12f, 0f, 10f, 2f), 1e-6f)   // give² / (over + give)²
        assertEquals(0.25f, swipeRubberSlope(-2f, 0f, 10f, 2f), 1e-6f)
        assertEquals(0f, swipeRubberSlope(14f, 0f, 10f, 0f))              // a hard clamp holds still
        // It is swipeRubber's derivative.
        val h = 1e-2f
        assertEquals((swipeRubber(13f + h, 0f, 10f, 2f) - swipeRubber(13f - h, 0f, 10f, 2f)) / (2 * h), swipeRubberSlope(13f, 0f, 10f, 2f), 1e-3f)
    }
}
