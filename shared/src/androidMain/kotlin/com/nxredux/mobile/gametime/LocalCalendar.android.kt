package com.nxredux.mobile.gametime

import java.time.Instant
import java.time.ZoneId

/** [LocalCalendar] on java.time in [zone] (read at each call, so a zone change applies at once). */
class ZoneCalendar(private val zone: () -> ZoneId) : LocalCalendar {
    override fun dayStart(ms: Long, days: Int): Long {
        val z = zone()
        return Instant.ofEpochMilli(ms).atZone(z).toLocalDate().plusDays(days.toLong()).atStartOfDay(z).toInstant().toEpochMilli()
    }

    override fun monthStart(ms: Long): Long {
        val z = zone()
        return Instant.ofEpochMilli(ms).atZone(z).toLocalDate().withDayOfMonth(1).atStartOfDay(z).toInstant().toEpochMilli()
    }
}

actual fun systemLocalCalendar(): LocalCalendar = ZoneCalendar { ZoneId.systemDefault() }
