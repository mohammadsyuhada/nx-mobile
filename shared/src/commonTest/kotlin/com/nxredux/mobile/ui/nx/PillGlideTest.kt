package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PillGlideTest {
    @Test fun aNearbyMoveStartsWhereThePillIs() {
        assertEquals(3f, nxPillGlideStart(3f, from = 3, to = 4, count = 20, visibleRows = 8))
        assertEquals(3.4f, nxPillGlideStart(3.4f, from = 4, to = 2, count = 20, visibleRows = 8))   // retarget mid-glide
    }

    @Test fun aWrapEntersFromOneRowBeyondTheNearEdge() {
        assertEquals(-1f, nxPillGlideStart(19f, from = 19, to = 0, count = 20, visibleRows = 8))    // drops in from above
        assertEquals(20f, nxPillGlideStart(0f, from = 0, to = 19, count = 20, visibleRows = 8))     // rises from below
        assertEquals(0f, nxPillGlideStart(0f, from = 0, to = 0, count = 1, visibleRows = 8))        // one row: no wrap
    }

    @Test fun aFarJumpEntersFromOneRowOffTheTargetInItsDirection() {
        assertEquals(11f, nxPillGlideStart(2f, from = 2, to = 12, count = 40, visibleRows = 8))
        assertEquals(13f, nxPillGlideStart(30f, from = 30, to = 12, count = 40, visibleRows = 8))
    }

    @Test fun rowTopFollowsTheScroll() {
        assertEquals(60f, nxPillRowTop(3f, firstIndex = 0, firstOffset = 0, pitch = 20))
        assertEquals(15f, nxPillRowTop(3f, firstIndex = 2, firstOffset = 5, pitch = 20))
        assertEquals(-20f, nxPillRowTop(-1f, firstIndex = 0, firstOffset = 0, pitch = 20))
        assertEquals(50f, nxPillRowTop(2.5f, firstIndex = 0, firstOffset = 0, pitch = 20))
    }

    @Test fun smoothstepEasesInAndOut() {
        assertEquals(0f, NxSmoothstep.transform(0f))
        assertEquals(0.5f, NxSmoothstep.transform(0.5f))
        assertEquals(1f, NxSmoothstep.transform(1f))
        assertEquals(0.104f, NxSmoothstep.transform(0.2f), 0.0001f)
    }

    // The in-game menu's Disc row changes its value in place: still the same list, so the pill does not snap.
    @Test fun aRowValueChangeKeepsTheListAndALabelChangeDoesNot() {
        val disc1 = listOf(NxRow("Resume"), NxRow("Disc", secondary = "1/3", cycleable = true))
        val disc2 = listOf(NxRow("Resume"), NxRow("Disc", secondary = "2/3", cycleable = true))
        assertEquals(nxPillListIdentity(disc1), nxPillListIdentity(disc2))
        assertNotEquals(nxPillListIdentity(disc1), nxPillListIdentity(listOf(NxRow("Resume"), NxRow("Reset"))))
        assertEquals(
            nxPillListIdentity(listOf(NxRichRow("Mario", subtitle = "1h"))),
            nxPillListIdentity(listOf(NxRichRow("Mario", subtitle = "2h"))),
        )
    }

    @Test fun aShorterListDropsTheSizesPastItsEnd() {
        val sizes = mapOf(0 to 1, 1 to 1, 4 to 1, 7 to 1)
        assertEquals(listOf(4, 7), nxStaleSizeKeys(sizes, count = 4).sorted())
        assertEquals(emptyList(), nxStaleSizeKeys(sizes, count = 8))
    }
}
