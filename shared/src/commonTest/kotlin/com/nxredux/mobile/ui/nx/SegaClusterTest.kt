package com.nxredux.mobile.ui.nx

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegaClusterTest {
    private fun centre(s: SegaCluster.Slot) = (s.x + s.diameter / 2) to (s.y + s.diameter / 2)

    @Test fun six_slots_one_per_sega_button() {
        assertEquals(listOf(NxButton.Y, NxButton.X, NxButton.A, NxButton.B, NxButton.L1, NxButton.R1), SegaCluster.slots.map { it.button })
    }

    @Test fun bottom_row_abc_rises_right_top_row_xyz_above_and_right() {
        val c = SegaCluster.slots.map(::centre)
        val (a, b, cc) = Triple(c[0], c[1], c[2])
        val (x, y, z) = Triple(c[3], c[4], c[5])
        assertTrue(a.first < b.first && b.first < cc.first); assertTrue(a.second > b.second && b.second > cc.second)
        assertTrue(x.first < y.first && y.first < z.first); assertTrue(x.second > y.second && y.second > z.second)
        for ((top, bottom) in listOf(x to a, y to b, z to cc)) { assertTrue(top.second < bottom.second); assertTrue(top.first > bottom.first) }
    }

    @Test fun slots_do_not_overlap_and_fit_the_box() {
        val s = SegaCluster.slots
        for (i in s.indices) for (j in i + 1 until s.size) {
            val (ax, ay) = centre(s[i]); val (bx, by) = centre(s[j])
            assertTrue(hypot(ax - bx, ay - by) >= (s[i].diameter + s[j].diameter) / 2, "${s[i].button} overlaps ${s[j].button}")
        }
        for (slot in s) {
            assertTrue(slot.x >= 0f && slot.y >= 0f, "${slot.button}")
            assertTrue(slot.x + slot.diameter <= SegaCluster.WIDTH && slot.y + slot.diameter <= SegaCluster.HEIGHT, "${slot.button}")
        }
    }

    @Test fun box_is_tight_around_the_slots() {
        val s = SegaCluster.slots
        val margin = 0.05f + 1e-4f
        assertTrue(s.minOf { it.x } <= margin && s.minOf { it.y } <= margin, "left/top inset")
        assertTrue(SegaCluster.WIDTH - s.maxOf { it.x + it.diameter } <= margin, "right inset")
        assertTrue(SegaCluster.HEIGHT - s.maxOf { it.y + it.diameter } <= margin, "bottom inset")
    }
    private val xyz = setOf(NxButton.B, NxButton.L1, NxButton.R1)
    private val threeButton = SegaCluster.visibleSlots { it in xyz }

    @Test fun three_button_slots_are_a_b_c() {
        assertEquals(listOf(NxButton.Y, NxButton.X, NxButton.A), threeButton.map { it.button })
    }

    @Test fun three_button_row_is_centred_in_the_box() {
        val s = threeButton
        val cx = (s.minOf { it.x } + s.maxOf { it.x + it.diameter }) / 2
        val cy = (s.minOf { it.y } + s.maxOf { it.y + it.diameter }) / 2
        assertEquals(SegaCluster.WIDTH / 2, cx, 1e-4f)
        assertEquals(SegaCluster.HEIGHT / 2, cy, 1e-4f)
    }

    @Test fun three_button_row_keeps_the_six_button_spacing_and_size() {
        val six = SegaCluster.slots.take(3)
        val three = threeButton
        val dx = three[0].x - six[0].x
        val dy = three[0].y - six[0].y
        for ((a, b) in six.zip(three)) {
            assertEquals(a.x + dx, b.x, 1e-4f); assertEquals(a.y + dy, b.y, 1e-4f); assertEquals(a.diameter, b.diameter)
        }
    }

    @Test fun three_button_row_fits_the_box() {
        for (slot in threeButton) {
            assertTrue(slot.x >= 0f && slot.y >= 0f, "${slot.button}")
            assertTrue(slot.x + slot.diameter <= SegaCluster.WIDTH && slot.y + slot.diameter <= SegaCluster.HEIGHT, "${slot.button}")
        }
    }

    @Test fun six_button_slots_are_unchanged() {
        assertEquals(SegaCluster.slots, SegaCluster.visibleSlots { false })
    }
}
