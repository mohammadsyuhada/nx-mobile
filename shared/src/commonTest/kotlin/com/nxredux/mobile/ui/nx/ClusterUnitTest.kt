package com.nxredux.mobile.ui.nx

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The landscape clusters keep their height-bound size; only two clusters that would meet shrink, to fit. */
class ClusterUnitTest {
    private fun near(a: Float, b: Float) = assertTrue(abs(a - b) < 0.01f, "expected $b, got $a")

    @Test fun aLandscapePhoneIsHeightBoundAndUnchanged() {
        // 800 × 360: (360 - 32) / 6 ≈ 54.67, as the 0.42-share box gave ((336 - 32) / 4.5 ≈ 67.6 was never the bound).
        val u = landscapeClusterUnit(800f, 360f)
        near(u, 328f / 6f)
        near(u, minOf((800f * 0.42f - 32f) / 4.5f, 328f / 6f))
        near(landscapeClusterWidth(u), 4.5f * 328f / 6f + 32f)
    }

    @Test fun aCutoutOnTheClusterSideLeavesAPhoneHeightBound() {
        near(landscapeClusterUnit(800f, 360f, inset = 30f), 328f / 6f)
    }

    @Test fun theCoverNoLongerShrinksForTheOldShare() {
        // 474 wide, height allowing 53: two 4.5 × 53 + 32 clusters (541) would meet, so each fits half: (237 - 32) / 4.5.
        val u = landscapeClusterUnit(474f, 350f)
        near(u, (237f - 32f) / 4.5f)
        assertTrue(u > (474f * 0.42f - 32f) / 4.5f, "bigger than the old 0.42-share unit")
        near(landscapeClusterWidth(u), 237f)
    }

    @Test fun theTallerCoverWindowFitsHalfTheWidth() {
        near(landscapeClusterUnit(474f, 470f), (237f - 32f) / 4.5f)
    }

    @Test fun twoClustersNeverOverlap() {
        for (w in listOf(320f, 474f, 600f, 800f, 1200f)) for (h in listOf(240f, 360f, 470f, 800f)) {
            val u = landscapeClusterUnit(w, h)
            assertTrue(2f * landscapeClusterWidth(u) <= w + 0.01f, "$w × $h: clusters of ${landscapeClusterWidth(u)} overlap")
            assertTrue(u <= (h - 32f) / 6f + 0.001f, "$w × $h: never taller than the height allows")
        }
    }

    @Test fun theUnitInsideABoxIsTheHeightBoundOneOrTheWidthFit() {
        assertEquals(50f, clusterUnit(400f, 300f))
        assertEquals(20f, clusterUnit(90f, 300f))
    }

    @Test fun overTheGameBothClustersClearTheDeeperCutout() {
        // A camera hole on either edge pushes both clusters in by its depth; none, and both stay at 0.
        near(symmetricCutoutInset(0f, 0f), 0f)
        near(symmetricCutoutInset(32f, 0f), 32f)
        near(symmetricCutoutInset(0f, 28f), 28f)
        near(symmetricCutoutInset(12f, 30f), 30f)
        // The same inset on both sides: the two mirrored clusters get the same unit and width, and fit the half screen.
        val u = landscapeClusterUnit(800f, 360f, symmetricCutoutInset(32f, 0f))
        assertTrue(landscapeClusterWidth(u, symmetricCutoutInset(32f, 0f)) <= 400f + 0.01f)
    }
}
