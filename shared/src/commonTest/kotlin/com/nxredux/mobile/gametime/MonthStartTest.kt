package com.nxredux.mobile.gametime

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthStartTest {
    private val day = 86_400_000L
    // 2026-10-03 00:00 UTC is epoch day 20729; 2026-10-01 is 20727; 2026-03-01 is 20513.
    @Test fun midMonthGoesToTheFirst() = assertEquals(20727 * day, FixedOffsetCalendar().monthStart(20729 * day + 5_000L))
    @Test fun theFirstIsItsOwnStart() = assertEquals(20727 * day, FixedOffsetCalendar().monthStart(20727 * day))
    @Test fun marchAfterFebruary() = assertEquals(20513 * day, FixedOffsetCalendar().monthStart(20513 * day + 3 * day))
    @Test fun offsetZoneUsesLocalDate() {
        val plus8 = FixedOffsetCalendar(8 * 3_600_000L)
        // 2026-09-30 17:00 UTC is 2026-10-01 01:00 at +8: October already.
        assertEquals(20727 * day - 8 * 3_600_000L, plus8.monthStart(20726 * day + 17 * 3_600_000L))
    }
}
