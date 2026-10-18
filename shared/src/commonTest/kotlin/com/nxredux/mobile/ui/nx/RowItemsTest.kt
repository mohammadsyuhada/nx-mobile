package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RowItemsTest {
    @Test fun composeWindowReachesOneHiddenItemPastTheShownOnes() {
        assertEquals(0..4, rowComposeWindow(0f, 10))
        assertEquals(1..9, rowComposeWindow(5f, 10))
        assertEquals(1..9, rowComposeWindow(5.5f, 10))     // mid-slide: 0.5 and 10.5 are out
        assertEquals(IntRange.EMPTY, rowComposeWindow(0f, 0))
        // Every item rowItem can show is composed, at any position.
        for (p in listOf(0f, 0.3f, 2.5f, 4f, 7.9f)) {
            assertTrue(rowComposeWindow(p, 10).toList().containsAll(rowWindow(p, 10).toList()), "pos $p")
        }
    }

    @Test fun rowWindowKeepsItsFourStepReachByDefault() {
        assertEquals(rowWindow(5f, 10), rowWindow(5f, 10, 4f))
        assertEquals(3..7, rowWindow(5f, 10, 3f))
    }

    @Test fun firstWindowTakesFreshSlots() {
        assertEquals(mapOf(2 to 0, 3 to 1, 4 to 2), itemSlots(emptyMap(), 2..4))
    }

    @Test fun aStepHandsTheLeavingItemsSlotToTheOneComingIn() {
        val before = itemSlots(emptyMap(), 0..4)
        val after = itemSlots(before, 1..5)
        assertEquals(before[0], after[5])                               // 0 left, 5 came in on its slot
        for (i in 1..4) assertEquals(before[i], after[i], "item $i")    // the rest keep theirs
        assertEquals(5, after.values.toSet().size)                      // no two items share a slot
    }

    @Test fun aWiderWindowAddsSlotsAndANarrowerOneKeepsTheRest() {
        val a = itemSlots(emptyMap(), 3..5)
        val wide = itemSlots(a, 1..7)
        for (i in 3..5) assertEquals(a[i], wide[i])
        assertEquals(7, wide.values.toSet().size)
        val narrow = itemSlots(wide, 4..5)
        assertEquals(mapOf(4 to wide[4], 5 to wide[5]), narrow)
    }

    @Test fun aJumpReusesTheLowestFreedSlotsFirst() {
        val a = itemSlots(emptyMap(), 0..2)   // 0, 1, 2
        val jumped = itemSlots(a, 10..12)
        assertEquals(listOf(0, 1, 2), (10..12).map { jumped.getValue(it) })
    }

    @Test fun padsPrefetchNearestTheSelectionFirstWithoutRepeats() {
        val pads = listOf("a", null, "c", "d", "a", "f")
        assertEquals(listOf("c", "d", "a", "f"), padPrefetchOrder(pads, 2))   // 2, 3, 1 (none), 4 (a), 0 (a again), 5
        assertEquals(listOf("a", "c", "d", "f"), padPrefetchOrder(pads, 0))
        assertEquals(emptyList(), padPrefetchOrder(listOf<String?>(null, null), 0))
    }
}
