package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class N3dsPadTest {
    @Test fun the_3ds_tag_picks_the_3ds_pad_with_or_without_the_new_controls() {
        assertSame(PadProfile.N3DS, PadProfile.forTag("3DS"))
        assertSame(PadProfile.N3DS, PadProfile.forTag(" 3ds ", n3dsNew = false))
        assertSame(PadProfile.N3DS_NEW, PadProfile.forTag("3DS", n3dsNew = true))
        assertSame(PadProfile.STANDARD, PadProfile.forTag("NDS", n3dsNew = true))   // only the 3DS has them
        assertEquals(FaceCluster.DIAMOND, PadProfile.N3DS.cluster)
    }

    @Test fun without_the_new_controls_zl_zr_and_the_c_stick_are_left_off() {
        val p = PadProfile.N3DS
        assertEquals(setOf(NxButton.L2, NxButton.R2), NxButton.entries.filter { p.isHidden(it) }.toSet())
        assertFalse(p.cStick)
    }

    @Test fun with_them_zl_zr_are_drawn_and_named_and_the_c_stick_shows() {
        val p = PadProfile.N3DS_NEW
        assertTrue(NxButton.entries.none { p.isHidden(it) })
        assertEquals("ZL", p.label(NxButton.L2))
        assertEquals("ZR", p.label(NxButton.R2))
        assertTrue(p.cStick)
        assertFalse(PadProfile.STANDARD.cStick)
    }

    @Test fun letters_and_menus_go_by_position_like_the_3ds() {
        for (p in listOf(PadProfile.N3DS, PadProfile.N3DS_NEW)) {
            assertEquals("A", p.label(NxButton.A)); assertEquals("B", p.label(NxButton.B))
            assertEquals("X", p.label(NxButton.X)); assertEquals("Y", p.label(NxButton.Y))
            assertEquals("L", p.label(NxButton.L1)); assertEquals("R", p.label(NxButton.R1))
            for (b in NxButton.entries) assertEquals(b, p.menuButton(b), b.name)
        }
    }

    private fun assertClear(l: CStickLayout, what: String) {
        assertTrue(cStickGap(l) >= C_STICK_GAP, "$what: gap ${cStickGap(l)}")
        assertEquals(C_STICK_DIAMETER, l.stick.width, what); assertEquals(l.stick.width, l.stick.height, what)
        // The well stays inside the cluster's box, and so does the diamond.
        assertTrue(l.stick.left >= 0f && l.stick.top >= 0f && l.stick.right <= l.width && l.stick.bottom <= l.height, what)
        assertTrue(l.diamond.x >= 0f && l.diamond.y >= 0f && l.diamond.x + 3f <= l.width + 1e-4f && l.diamond.y + 3f <= l.height + 1e-4f, what)
        // Its square touch box stays out of every face button's, so it never takes their touches.
        for (hit in faceClusterHitBoxes(PadProfile.N3DS_NEW, l)) assertFalse(hit.rect.overlaps(l.stick), "$what: ${hit.button}")
    }

    @Test fun every_drop_keeps_the_gap_with_the_stick_and_diamond_inside_the_box() {
        for (i in 0..10) assertClear(cStickLayout(i * 0.05f), "drop ${i * 0.05f}")
    }

    @Test fun landscape_drops_the_diamond_half_a_unit_and_fits_the_cluster() {
        val l = cStickLayout(C_STICK_LANDSCAPE_DROP)
        assertClear(l, "landscape")
        assertEquals(0.5f, l.diamond.y)
        // The 4.5 x 6 cluster: the box plus its 0.2 edge nudge, and shoulders 1 + gap 0.35 + box + gap 0.35 + system 0.72.
        assertTrue(l.width + 0.2f <= 4.5f, "width ${l.width}")
        assertTrue(1f + 0.35f + l.height + 0.35f + 0.72f <= 6f, "height ${l.height}")
    }

    /** The portrait stick's circle in [box] keeps [PORTRAIT_C_STICK_GAP] off both clusters' squares and the pills, NX mark. */
    private fun assertPortraitClear(w: Float, h: Float, box: Rect, what: String) {
        val s = midRowScale(w, h)
        val g = PORTRAIT_C_STICK_GAP - 1e-3f
        val r = box.width / 2f
        assertEquals(box.width, box.height, 1e-4f, what)
        assertEquals(w / 2f, box.center.x, 1e-4f, "$what: centred")
        assertTrue(box.top >= 0.86f + g, "$what: under SELECT / START")
        assertTrue(box.bottom <= h - 2.12f - g, "$what: above the NX mark")
        val rowTop = 1f + (h - 3.12f - 3f * s) / 2f
        val dpad = Rect(0.55f, rowTop, 0.55f + 3f * s, rowTop + 3f * s)
        val face = Rect(w - 0.55f - 3f * s, rowTop, w - 0.55f, rowTop + 3f * s)
        for (c in listOf(dpad, face)) assertTrue(c.distanceTo(box.center) >= r + g, "$what: clear of $c")
    }

    @Test fun portrait_c_stick_on_the_fold_inner_screen_is_full_size_under_select_start() {
        val w = 13.05f; val h = 6.2f
        val box = portraitCStickBox(w, h, midRowScale(w, h))
        assertEquals(C_STICK_DIAMETER * midRowScale(w, h), box.width, 1e-4f)
        assertPortraitClear(w, h, box, "inner")
    }

    @Test fun portrait_c_stick_on_the_fold_inner_screen_stays_between_the_clusters() {
        val w = 13.05f; val h = 6.2f
        val c = portraitCStick(w, h, midRowScale(w, h))
        assertEquals(0f, c.clusterDrop)
        assertEquals(portraitCStickBox(w, h, midRowScale(w, h)), c.box)
    }

    @Test fun portrait_c_stick_on_the_fold_cover_sits_above_the_dropped_clusters() {
        // The Fold's cover in portrait: 443 x 340 dp inside the padding, 9 x 6.91 units; the clusters 1.12 units apart.
        val w = 9f; val h = 6.91f
        val s = midRowScale(w, h)
        assertEquals(1.13f, s, 1e-3f)                                       // the clusters keep their size
        assertTrue(portraitCStickBox(w, h, s).width < C_STICK_DIAMETER * s)  // too narrow between them
        val c = portraitCStick(w, h, s)
        val g = PORTRAIT_C_STICK_GAP - 1e-3f
        assertTrue(c.clusterDrop > 0f)
        val clusterTop = 1f + c.clusterDrop
        val clusterBottom = clusterTop + 3f * s
        assertEquals(h, clusterBottom + c.clusterBottomPad + 0.72f, 1e-3f)     // the column has no free height left
        assertTrue(clusterBottom + g <= h - 0.72f, "clear of MENU / toggle")
        // Dropped ~19 dp (unit 443 / 9 ≈ 49.2 dp) from 1.2 (the free 0.4 split in two), ~50 dp above the lowest place.
        assertEquals(19.7f, (clusterTop - 1.2f) * 443f / 9f, 1f)
        // The NX mark (1.1 tall + 0.3 over MENU, 1.15 wide: wider than the clusters' 1.12-unit gap) cannot fit under them.
        assertTrue(clusterBottom + g + 1.1f + 0.3f > h - 0.72f)
        val box = c.box
        assertEquals(w / 2f, box.center.x, 1e-4f)
        assertEquals(box.width, box.height, 1e-4f)
        assertTrue(box.top >= 0.86f + g, "under SELECT / START")
        assertTrue(box.width > portraitCStickBox(w, h, s).width, "bigger than between the undropped clusters")
        assertTrue(box.width <= C_STICK_DIAMETER * s + 1e-4f)
        assertEquals(C_STICK_DIAMETER * s, box.width, 1e-4f)                     // full size
        // Its well keeps 0.1 off every actual button: the d-pad's arms, the left stick's well, X and Y.
        val r = box.width / 2f
        val (arms, circles) = portraitCStickNeighbours(w, s, clusterTop)
        for (arm in arms) assertTrue(arm.distanceTo(box.center) >= r + PORTRAIT_C_STICK_BUTTON_GAP - 1e-3f, "arm $arm")
        for ((o, rr) in circles) assertTrue((o - box.center).getDistance() >= r + rr + PORTRAIT_C_STICK_BUTTON_GAP - 1e-3f, "button $o")
        // It does cover the clusters' empty corners (their square boxes), which is why its touch is the round well.
        val dpad = Rect(0.55f, clusterTop, 0.55f + 3f * s, clusterBottom)
        val face = Rect(w - 0.55f - 3f * s, clusterTop, w - 0.55f, clusterBottom)
        assertTrue(dpad.overlaps(box) && face.overlaps(box))
    }

    @Test fun a_touch_starts_on_the_c_stick_only_inside_its_round_well() {
        val w = 9f; val h = 6.91f
        val s = midRowScale(w, h)
        val c = portraitCStick(w, h, s)
        val box = c.box
        val r = box.width / 2f
        assertTrue(cStickWellHit(box, box.center))
        assertTrue(cStickWellHit(box, box.center + Offset(r * 0.99f, 0f)))
        // The well's square box's bottom corners sit over the clusters' empty corners: those touches are the clusters'.
        val dpad = Rect(0.55f, 1f + c.clusterDrop, 0.55f + 3f * s, 1f + c.clusterDrop + 3f * s)
        val corner = Offset(box.left + 0.02f, box.bottom - 0.02f)
        assertTrue(dpad.contains(corner))
        assertFalse(cStickWellHit(box, corner))
        assertFalse(cStickWellHit(box, Offset(box.right - 0.02f, box.bottom - 0.02f)))
    }

    @Test fun portrait_c_stick_clears_everything_on_other_pads() {
        for ((w, h) in listOf(9f to 9f, 10f to 6.2f, 9f to 6.2f, 11f to 7f)) {
            assertPortraitClear(w, h, portraitCStickBox(w, h, midRowScale(w, h)), "$w x $h")
        }
    }

    @Test fun without_the_c_stick_the_diamond_keeps_its_place() {
        val cells = mapOf(NxButton.X to (1f to 0f), NxButton.Y to (0f to 1f), NxButton.A to (2f to 1f), NxButton.B to (1f to 2f))
        for (p in listOf(PadProfile.N3DS, PadProfile.N3DS_NEW, PadProfile.STANDARD)) {
            val boxes = faceClusterHitBoxes(p)
            assertEquals(cells.keys, boxes.map { it.button }.toSet())
            for (b in boxes) assertEquals(cells.getValue(b.button), b.rect.left to b.rect.top, "${p.kind} ${b.button}")
        }
        assertEquals(3f, faceWidthUnits(PadProfile.N3DS_NEW)); assertEquals(3f, faceHeightUnits(PadProfile.N3DS_NEW))
    }

    @Test fun with_it_the_touch_boxes_follow_the_dropped_diamond() {
        val l = cStickLayout(C_STICK_LANDSCAPE_DROP)
        val plain = faceClusterHitBoxes(PadProfile.N3DS_NEW).associate { it.button to it.rect }
        for (b in faceClusterHitBoxes(PadProfile.N3DS_NEW, l)) assertEquals(plain.getValue(b.button).translate(l.diamond), b.rect)
    }

}
