package com.nxredux.mobile.gametime

/** nx-redux `serializeTime`: "Xh Ym", "Xm Ys" or "Xs". */
object GameTimeFormat {
    fun duration(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) / 1000L)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${sec}s"
            else -> "${sec}s"
        }
    }

    /**
     * The row subtitle as (text, emphasis) parts, NX Redux's Game Tracker style: values emphasised, the words after them
     * and the wide "  ·  " separators not. The UI maps these to styled spans; [rowLine] is the same parts joined.
     */
    fun rowParts(t: GameTotal): List<Pair<String, Boolean>> = listOf(
        duration(t.totalMs) to true, " total" to false, SEP to false,
        duration(t.averageMs) to true, " avg" to false, SEP to false,
        "${t.plays}" to true, (if (t.plays == 1L) " play" else " plays") to false,
    )

    fun rowLine(t: GameTotal): String = rowParts(t).joinToString("") { it.first }

    private const val SEP = "  ·  "
}
