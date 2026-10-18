package com.nxredux.mobile.ui.nx

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PadMetricsTest {
    private fun near(a: Float, b: Float) = assertTrue(abs(a - b) < 0.5f, "expected $b, got $a")

    @Test fun usesRealDpiWhenSane() {
        // 5.8 cm at 420 dpi = 5.8/2.54*420 ≈ 959 px; availablePx large so no cap.
        near(padBandPx(5.8f, 420f, 2.625f, 4000f), 5.8f / 2.54f * 420f)
    }

    @Test fun fallsBackToDensityBucketWhenDpiInsane() {
        // ydpi 0 -> density*160 = 420 dpi bucket.
        near(padBandPx(5.8f, 0f, 2.625f, 4000f), 5.8f / 2.54f * (2.625f * 160f))
    }

    @Test fun cappedAtSixtyPercent() {
        // Tiny screen: 5.8 cm would exceed 60% of 500 px -> capped at 300.
        near(padBandPx(5.8f, 420f, 2.625f, 500f), 300f)
    }

    @Test fun midRowGrowsToTheTargetWithRoomToSpare() {
        near(midRowScale(20f, 20f), 1.25f)
    }

    @Test fun midRowIsClampedByWidth() {
        // A width-bound pad is 9 units wide: (9 - 1.1 - 0.5) / 6 ≈ 1.233.
        near(midRowScale(9f, 7.9f), (9f - 1.6f) / 6f)
    }

    @Test fun n64FaceMakesRoomBesideTheFullSizeStick() {
        // A width-bound 9-unit pad: the stick keeps the diamond's (9 - 1.6) / 6; the N64 cluster takes the width left.
        val w = N64Cluster.WIDTH; val h = N64Cluster.HEIGHT
        val stick = midRowScale(9f, 7.9f)
        assertTrue(abs(stick - 7.4f / 6f) < 0.001f, "the stick is the diamond's scale, got $stick")
        val face = faceClusterScale(9f, 7.9f, stick, w, h)
        val expected = (7.4f - 3f * stick) / w
        assertTrue(abs(face - expected) < 0.001f, "expected $expected, got $face")
    }

    @Test fun n64FaceMakesRoomForTheTallerCluster() {
        // A height-bound pad 7.2 units tall: the stick keeps 3.68 / 3, the N64 cluster (7.2 - 3.12 - 0.4) / N64Cluster.HEIGHT.
        val w = N64Cluster.WIDTH; val h = N64Cluster.HEIGHT
        val stick = midRowScale(20f, 7.2f)
        assertTrue(abs(stick - 3.68f / 3f) < 0.001f, "the 3-unit diamond is unchanged, got $stick")
        val face = faceClusterScale(20f, 7.2f, stick, w, h)
        assertTrue(abs(face - 3.68f / h) < 0.001f, "expected ${3.68f / h}, got $face")
    }

    @Test fun n64StickOnTheFoldMatchesTheDiamond() {
        // The Fold-like pad, 9 x 7.02 units: stick (7.02 - 3.52) / 3 ≈ 1.167 for every console;
        // the N64 face min((9 - 1.6 - 3 * 1.167) / 3.75 ≈ 1.04, 3.5 / 3.45 ≈ 1.014).
        val stick = midRowScale(9f, 7.02f)
        assertTrue(abs(stick - 3.5f / 3f) < 0.001f, "expected ${3.5f / 3f}, got $stick")
        val face = faceClusterScale(9f, 7.02f, stick, N64Cluster.WIDTH, N64Cluster.HEIGHT)
        val expected = minOf((7.4f - 3f * stick) / N64Cluster.WIDTH, 3.5f / N64Cluster.HEIGHT)
        assertTrue(abs(face - expected) < 0.001f, "expected $expected, got $face")
        assertTrue(abs(face - 1.014f) < 0.001f, "expected ≈1.014, got $face")
    }

    @Test fun diamondFaceScaleIsTheStickScale() {
        for (w in listOf(9f, 12f, 20f)) for (h in listOf(6.2f, 7.02f, 7.9f, 9f, 20f)) {
            val stick = midRowScale(w, h)
            assertEquals(stick, faceClusterScale(w, h, stick, 3f, 3f), "w=$w h=$h")
        }
    }

    @Test fun tightPadKeepsTheStickAndShrinksTheN64Cluster() {
        // No room at all: the stick keeps today's size, the taller cluster shrinks to the diamond's 3-unit height.
        val stick = midRowScale(9f, 6.2f)
        assertEquals(1f, stick)
        val face = faceClusterScale(9f, 6.2f, stick, N64Cluster.WIDTH, N64Cluster.HEIGHT)
        assertTrue(abs(face - 3f / N64Cluster.HEIGHT) < 0.001f, "expected ${3f / N64Cluster.HEIGHT}, got $face")
    }

    @Test fun n64RowFitsTheWidth() {
        val fw = N64Cluster.WIDTH; val fh = N64Cluster.HEIGHT
        for (w in listOf(9f, 12f, 20f)) for (h in listOf(6.2f, 7.02f, 9f)) {
            val stick = midRowScale(w, h)
            val face = faceClusterScale(w, h, stick, fw, fh)
            val row = 1.6f + 3f * stick + fw * face
            assertTrue(row <= w + 0.001f, "w=$w h=$h: row $row overflows")
        }
    }

    @Test fun midRowIsClampedByHeightButNeverShrinks() {
        near(midRowScale(9f, 7f), (7f - 3.52f) / 3f)
        near(midRowScale(9f, 6.2f), 1f)   // a height-bound band: no room, keeps today's size
    }
}
