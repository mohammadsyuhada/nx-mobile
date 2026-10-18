package com.nxredux.mobile.gametime

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** The device calendar across DST changes: every day start a local midnight, days of 23 and 25 h, the month's start local. */
class ZoneCalendarTest {
    private val hour = 3_600_000L
    private val london = ZoneId.of("Europe/London")
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    // Clocks go forward on Sunday 29 March 2026: that day is 23 h.
    @Test fun springForwardDayIs23Hours() {
        val cal = ZoneCalendar { london }
        val noon = at("2026-03-29T11:00:00Z")
        assertEquals(at("2026-03-29T00:00:00Z"), cal.dayStart(noon))          // 00:00 GMT
        assertEquals(at("2026-03-29T23:00:00Z"), cal.dayStart(noon, 1))       // Monday 00:00 BST
        assertEquals(23 * hour, cal.dayStart(noon, 1) - cal.dayStart(noon))
    }

    // Clocks go back on Sunday 25 October 2026: that day is 25 h.
    @Test fun fallBackDayIs25Hours() {
        val cal = ZoneCalendar { london }
        val noon = at("2026-10-25T12:00:00Z")
        assertEquals(25 * hour, cal.dayStart(noon, 1) - cal.dayStart(noon))
        assertEquals(at("2026-10-24T23:00:00Z"), cal.dayStart(noon))          // 00:00 BST
    }

    // The month starts at local 00:00 on the 1st, in summer time too.
    @Test fun theMonthStartsAtLocalMidnight() {
        val cal = ZoneCalendar { london }
        assertEquals(at("2026-03-31T23:00:00Z"), cal.monthStart(at("2026-04-15T12:00:00Z")))   // 1 April 00:00 BST
        assertEquals(at("2026-03-01T00:00:00Z"), cal.monthStart(at("2026-03-30T12:00:00Z")))   // across the change: 00:00 GMT
    }

    // A zone whose DST began at midnight (Sao Paulo, 4 November 2018): that day starts at 01:00.
    @Test fun aDayWithoutAMidnightStartsAtItsFirstHour() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val cal = ZoneCalendar { zone }
        assertEquals(at("2018-11-04T03:00:00Z"), cal.dayStart(at("2018-11-04T15:00:00Z")))   // 01:00 -02:00
        assertEquals(23 * hour, cal.dayStart(at("2018-11-04T15:00:00Z"), 1) - cal.dayStart(at("2018-11-04T15:00:00Z")))
    }
}
