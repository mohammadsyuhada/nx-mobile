package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals

class RaSortTest {
    private fun a(id: Int, title: String, points: Int, rarity: Float, state: RaAchievementState, type: RaType? = null) =
        RaAchievement(id, title, "d", points, "b$id", state, if (state == RaAchievementState.LOCKED) null else 1000L + id, null, 0f, rarity, type)
    private val list = listOf(
        a(3, "Charlie", 5, 80f, RaAchievementState.LOCKED, RaType.WIN),
        a(1, "alpha", 10, 20f, RaAchievementState.UNLOCKED),
        a(2, "Bravo", 25, 50f, RaAchievementState.PENDING, RaType.MISSABLE),
    )

    @Test fun unlocked_first_keeps_display_order_within_groups() =
        assertEquals(listOf(1, 2, 3), list.sortedBy(RaSortOrder.UNLOCKED_FIRST).map { it.id })
    @Test fun display_order() {
        assertEquals(listOf(3, 1, 2), list.sortedBy(RaSortOrder.DISPLAY_ORDER_FIRST).map { it.id })
        assertEquals(listOf(2, 1, 3), list.sortedBy(RaSortOrder.DISPLAY_ORDER_LAST).map { it.id })
    }
    @Test fun won_by() {
        assertEquals(listOf(3, 2, 1), list.sortedBy(RaSortOrder.WON_BY_MOST).map { it.id })
        assertEquals(listOf(1, 2, 3), list.sortedBy(RaSortOrder.WON_BY_LEAST).map { it.id })
    }
    @Test fun points() {
        assertEquals(listOf(2, 1, 3), list.sortedBy(RaSortOrder.POINTS_MOST).map { it.id })
        assertEquals(listOf(3, 1, 2), list.sortedBy(RaSortOrder.POINTS_LEAST).map { it.id })
    }
    @Test fun title_ignores_case() {
        assertEquals(listOf(1, 2, 3), list.sortedBy(RaSortOrder.TITLE_AZ).map { it.id })
        assertEquals(listOf(3, 2, 1), list.sortedBy(RaSortOrder.TITLE_ZA).map { it.id })
    }
    @Test fun type_orders_standard_missable_progression_win() {
        assertEquals(listOf(1, 2, 3), list.sortedBy(RaSortOrder.TYPE_ASC).map { it.id })
        assertEquals(listOf(3, 2, 1), list.sortedBy(RaSortOrder.TYPE_DESC).map { it.id })
    }
    @Test fun sort_order_labels_and_default() {
        assertEquals(11, RaSortOrder.entries.size)
        assertEquals("Unlocked first", RaSortOrder.UNLOCKED_FIRST.label)
        assertEquals(RaSortOrder.UNLOCKED_FIRST, RaSortOrder.DEFAULT)
    }
}
