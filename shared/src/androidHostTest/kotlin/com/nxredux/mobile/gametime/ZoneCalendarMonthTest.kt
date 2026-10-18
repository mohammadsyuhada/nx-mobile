package com.nxredux.mobile.gametime

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneCalendarMonthTest {
    @Test fun dstMonthStartsAtLocalMidnight() {
        val z = ZoneId.of("Europe/Berlin")
        val cal = ZoneCalendar { z }
        val inMarch = ZonedDateTime.of(2026, 3, 30, 12, 0, 0, 0, z).toInstant().toEpochMilli()   // after the 29 March change
        assertEquals(ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, z).toInstant().toEpochMilli(), cal.monthStart(inMarch))
    }
}
