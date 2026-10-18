package com.nxredux.mobile.ui

import com.nxredux.mobile.gametime.GameTimeFormat

/**
 * Home's stats strip under the tab row: the calendar month's [played] time and [achievements] unlocked (null signed
 * out: left off), then Most played's cleaned title and time ([most]; null with nothing played: the strip is one line).
 * Before the first computation every value is "…" and both lines stay, so nothing moves when the values arrive.
 */
data class HomeStrip(val played: String, val playedDim: Boolean, val achievements: String?, val most: HomeStripMost?)

/** Most played on the strip: the cleaned title and its time this month (null while pending). */
data class HomeStripMost(val title: String, val time: String?)

/** The strip for [stats]. Nothing played this month: a grey "No play yet", 0 achievements, no Most played line. */
internal fun homeStrip(stats: HomeStats): HomeStrip {
    if (!stats.computed) return HomeStrip(STAT_PENDING, false, STAT_PENDING.takeIf { stats.signedIn }, HomeStripMost(STAT_PENDING, null))
    val ach = if (stats.signedIn) (stats.unlocks ?: 0).toString() else null
    val played = stats.playedMs ?: 0L
    val top = stats.top
    if (played <= 0L || top == null) return HomeStrip(NO_PLAY_YET, true, ach, null)
    return HomeStrip(GameTimeFormat.duration(played), false, ach, HomeStripMost(homeStripTitle(top.name), GameTimeFormat.duration(top.totalMs)))
}

/** A ROM title as the strip shows it: region, version and dump tags in (…) or […] dropped, spaces squeezed (the raw title, trimmed, when nothing else is left). */
internal fun homeStripTitle(raw: String): String =
    raw.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").replace(Regex("""\s+"""), " ").trim().ifBlank { raw.trim() }

/**
 * The strip's lines: none without one, one with no Most played, else two (on every frame: a one-line strip would crowd
 * the Recent and Search icons at the top right).
 */
internal fun homeStripLines(strip: HomeStrip?): Int = when {
    strip == null -> 0
    strip.most == null -> 1
    else -> 2
}
