package com.nxredux.mobile.gametime

/** A test calendar at a fixed UTC offset (no DST): midnights are [offsetMs] before each UTC one. */
class FixedOffsetCalendar(private val offsetMs: Long = 0L) : LocalCalendar {
    private val day = 86_400_000L
    private fun epochDay(ms: Long) = (ms + offsetMs).floorDiv(day)
    override fun dayStart(ms: Long, days: Int): Long = (epochDay(ms) + days) * day - offsetMs
    override fun monthStart(ms: Long): Long {
        val d = epochDay(ms)
        val z = d + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val dom = doy - (153 * mp + 2) / 5   // 0-based day of the month
        return (d - dom) * day - offsetMs
    }
}
