package com.nxredux.mobile.gametime

/**
 * The device's local calendar, for Home's stats month. Day and month arithmetic goes by calendar dates in the local
 * zone, so it is DST-safe: a day across a change is 23 or 25 h, and a day whose midnight is skipped starts
 * at its first local time.
 */
interface LocalCalendar {
    /** The start (local 00:00) of the calendar day [days] after the day holding [ms] (negative: before it). */
    fun dayStart(ms: Long, days: Int = 0): Long

    /** The start (local 00:00) of the 1st of the calendar month holding [ms]: Home's stats window starts here. */
    fun monthStart(ms: Long): Long
}

/** The device's calendar, in its current time zone. */
expect fun systemLocalCalendar(): LocalCalendar
