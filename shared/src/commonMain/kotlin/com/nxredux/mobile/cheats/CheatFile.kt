package com.nxredux.mobile.cheats

import com.nxredux.mobile.storage.TextLines

/** One cheat from a RetroArch `.cht` file. [code] is handed to the core verbatim (`retro_cheat_set`). */
data class Cheat(val desc: String, val code: String, val enabled: Boolean = false)

/**
 * RetroArch `.cht` files, read the way nx-redux's minarch does (`ma_cheats.c`): `cheats = N` first, then
 * `cheatN_desc = "…"`, `cheatN_code = "…"` and `cheatN_enable = true|false`. Every other key (`_address`,
 * `_handler`, …) is ignored, and a line that does not parse is skipped, never fatal.
 */
object CheatFile {
    /** Sanity cap on `cheats = N` (the array is allocated up front). */
    const val MAX_CHEATS = 4096

    // The count may be quoted (`cheats = "213"`: every PSP file in libretro's collection, and a few hundred others).
    // RetroArch reads either; minarch's fscanf("%lu") stops at the quote and loses those files.
    private val COUNT = Regex("""^cheats\s*=\s*("?)(\d+)\1$""")
    private val KEY = Regex("""^\s*cheat(\d+)_(desc|code|enable)\s*=\s*""")

    /**
     * Null when the count line is missing, unparsable or zero. Otherwise the cheats that have both a desc and
     * a code, in index order; an empty list means the file parsed but holds nothing usable.
     */
    fun parse(text: String): List<Cheat>? {
        val lines = TextLines.split(text)
        val first = lines.firstOrNull { it.isNotBlank() } ?: return null
        val count = COUNT.matchEntire(first.trim())?.groupValues?.get(2)?.toIntOrNull() ?: return null
        if (count <= 0) return null
        val n = minOf(count, MAX_CHEATS)
        val descs = arrayOfNulls<String>(n)
        val codes = arrayOfNulls<String>(n)
        val enabled = BooleanArray(n)
        for (line in lines) {
            val m = KEY.find(line) ?: continue
            val index = m.groupValues[1].toIntOrNull() ?: continue
            if (index !in 0 until n) continue
            val value = line.substring(m.range.last + 1)
            when (m.groupValues[2]) {
                // Duplicate keys overwrite the earlier value, like minarch.
                "desc" -> quoted(value)?.let { descs[index] = it }
                "code" -> quoted(value)?.let { codes[index] = it }
                "enable" -> bool(value)?.let { enabled[index] = it }
            }
        }
        return (0 until n).mapNotNull { i ->
            val desc = descs[i]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val code = codes[i]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Cheat(desc, code, enabled[i])
        }
    }

    /** The content of a `"…"` value: `\x` → `x`, `&quot;` → `"`. Null when not quoted or unterminated. */
    private fun quoted(value: String): String? {
        if (!value.startsWith('"')) return null
        val out = StringBuilder()
        var i = 1
        while (i < value.length) {
            val c = value[i]
            when {
                c == '"' -> return out.toString()
                c == '\\' && i + 1 < value.length -> { out.append(value[i + 1]); i += 2 }
                c == '&' && value.startsWith("&quot;", i) -> { out.append('"'); i += 6 }
                else -> { out.append(c); i++ }
            }
        }
        return null
    }

    /** Bare `true`/`false`, case-insensitive prefix match (minarch `strncasecmp`); null otherwise. */
    private fun bool(value: String): Boolean? = when {
        value.startsWith("true", ignoreCase = true) -> true
        value.startsWith("false", ignoreCase = true) -> false
        else -> null
    }
}
