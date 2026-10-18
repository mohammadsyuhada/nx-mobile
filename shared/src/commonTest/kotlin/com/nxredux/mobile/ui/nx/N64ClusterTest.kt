package com.nxredux.mobile.ui.nx

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class N64ClusterTest {
    @Test fun six_buttons_a_b_and_the_c_diamond() {
        assertEquals(
            setOf(NxButton.A, NxButton.B, NxButton.C_UP, NxButton.C_DOWN, NxButton.C_LEFT, NxButton.C_RIGHT),
            N64Cluster.slots.map { it.button }.toSet(),
        )
        assertEquals(6, N64Cluster.slots.size)
    }

    // "Never overlap" means the drawn circles (centre distance vs radii); the square touch boxes may touch at corners.
    @Test fun n64_slots_fit_and_never_overlap() {
        val s = N64Cluster.slots
        for (a in s) {
            assertTrue(a.x >= 0f && a.y >= 0f && a.x + a.diameter <= N64Cluster.WIDTH && a.y + a.diameter <= N64Cluster.HEIGHT, "${a.button} inside the cluster box")
        }
        for (i in s.indices) for (j in i + 1 until s.size) {
            val p = s[i]; val q = s[j]
            val d = hypot((p.x + p.diameter / 2) - (q.x + q.diameter / 2), (p.y + p.diameter / 2) - (q.y + q.diameter / 2))
            assertTrue(d >= (p.diameter + q.diameter) / 2 + 0.05f, "${p.button} and ${q.button} keep a gap")
        }
    }

    @Test fun layout_is_like_a_real_n64_pad() {
        fun centre(b: NxButton) = N64Cluster.slots.first { it.button == b }.let { (it.x + it.diameter / 2) to (it.y + it.diameter / 2) }
        val a = centre(NxButton.A); val b = centre(NxButton.B)
        assertTrue(b.first < a.first && b.second < a.second, "B up and left of A")
        val cUp = centre(NxButton.C_UP); val cDown = centre(NxButton.C_DOWN)
        val cLeft = centre(NxButton.C_LEFT); val cRight = centre(NxButton.C_RIGHT)
        assertTrue(cUp.second < cDown.second && cLeft.first < cRight.first, "C diamond")
        assertTrue(cLeft.first > b.first && cUp.second < a.second, "C diamond upper right")
        assertTrue(abs(b.second - cDown.second) < 0.001f && b.first < cLeft.first, "B level with C_DOWN, left of the diamond")
        assertTrue(a.first > b.first && a.second > b.second, "A below and right of B")
        assertTrue(a.first < cDown.first && a.second > cDown.second, "A below and left of C_DOWN")
        val aSlot = N64Cluster.slots.first { it.button == NxButton.A }; val bSlot = N64Cluster.slots.first { it.button == NxButton.B }
        val abGap = hypot(a.first - b.first, a.second - b.second) - (aSlot.diameter + bSlot.diameter) / 2
        assertTrue(abGap < 0.5f, "A and B close together, a pair (gap $abGap)")
        assertTrue(N64Cluster.slots.first { it.button == NxButton.A }.diameter > N64Cluster.slots.first { it.button == NxButton.C_UP }.diameter)
        val aSize = N64Cluster.slots.first { it.button == NxButton.A }.diameter
        assertTrue(aSize >= 1.1f, "A and B are bigger than a diamond face button (A is $aSize)")
        for (c in listOf(NxButton.C_UP, NxButton.C_DOWN, NxButton.C_LEFT, NxButton.C_RIGHT)) {
            assertTrue(N64Cluster.slots.first { it.button == c }.diameter >= 0.75f * aSize, "$c at least 0.75 of A")
        }
    }
}
