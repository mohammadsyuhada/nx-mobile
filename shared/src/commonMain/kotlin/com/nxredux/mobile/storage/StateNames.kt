package com.nxredux.mobile.storage

/** A parsed save-state file name: the ROM [stem] and its [slot] (0..N, or [NxPaths.AUTO_SLOT]). */
data class StateName(val stem: String, val slot: Int)

/** Reads the three state naming schemes ([NxPaths]) back into a stem and slot, and finds free slots. */
object StateNames {
    private const val RETROARCH_AUTO_OFFSET = 100

    /**
     * Parses a MinUI (`<rom.ext>.st<n>`), Retroarch-ish (`<stem>.state.<n>`, `.state.auto`) or RetroArch (`<stem>.state`,
     * `<stem>.state<n>`) name, suffixes case-insensitive. RetroArch slots 8 and up come back as `n + 100` so `.state9`
     * never lands on [NxPaths.AUTO_SLOT]. Anything else (thumbnails included) is null.
     */
    fun parse(fileName: String): StateName? {
        val lower = fileName.lowercase()
        if (lower.endsWith(".state.auto")) return named(fileName, ".state.auto".length, NxPaths.AUTO_SLOT)
        lower.lastIndexOf(".state.").let { i ->
            val n = if (i > 0) digits(lower.substring(i + ".state.".length)) else null
            if (n != null) return named(fileName, fileName.length - i, n)
        }
        if (lower.endsWith(".state")) return named(fileName, ".state".length, 0)
        lower.lastIndexOf(".state").let { i ->
            val n = if (i > 0) digits(lower.substring(i + ".state".length)) else null
            if (n != null) return named(fileName, fileName.length - i, if (n >= NxPaths.SLOT_COUNT) n + RETROARCH_AUTO_OFFSET else n)
        }
        lower.lastIndexOf(".st").let { i ->
            val n = if (i > 0) digits(lower.substring(i + ".st".length)) else null
            if (n != null) return StateName(stripExtension(fileName.substring(0, i)), n)
        }
        return null
    }

    /**
     * The slot to store a state wanted in [wanted]: that slot when it is real (0..7 or auto) and not in [taken], else the
     * lowest free of 0..7, else null.
     */
    fun place(wanted: Int, taken: Set<Int>): Int? {
        val real = wanted in 0 until NxPaths.SLOT_COUNT || wanted == NxPaths.AUTO_SLOT
        if (real && wanted !in taken) return wanted
        return (0 until NxPaths.SLOT_COUNT).firstOrNull { it !in taken }
    }

    private fun named(name: String, suffixLength: Int, slot: Int) = StateName(name.dropLast(suffixLength), slot)

    private fun digits(s: String): Int? = if (s.isNotEmpty() && s.length <= 4 && s.all { it in '0'..'9' }) s.toInt() else null
}
