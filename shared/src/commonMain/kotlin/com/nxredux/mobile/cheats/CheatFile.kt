package com.nxredux.mobile.cheats

import com.nxredux.mobile.storage.TextLines

/**
 * One cheat from a RetroArch `.cht` file. [code] is handed to the core verbatim (`retro_cheat_set`); a [mem] cheat
 * (`cheatN_handler = 1`) has no code and is a RAM patch the frontend applies every frame instead.
 */
data class Cheat(val desc: String, val code: String, val enabled: Boolean = false, val mem: MemoryPatch? = null)

/**
 * RetroArch's RAM-patch fields (`cheat_manager_apply_retro_cheats`), with its defaults. [address] is an offset into
 * the core's system RAM; [size] is `memory_search_size` (0-2 = 1/2/4 bits, 3 = 8, 4 = 16, 5 = 32), [type]
 * `cheat_type` (1 set, 2 increase, 3 decrease, 4-7 run the next cheat if equal/not equal/less/greater), [bitMask]
 * `address_bit_position`. [address] and [value] are unsigned 32-bit.
 */
data class MemoryPatch(
    val address: Long = 0, val value: Long = 0, val size: Int = 0, val type: Int = 0, val bitMask: Int = 0,
    val bigEndian: Boolean = false, val repeatCount: Int = 1, val repeatAddToValue: Int = 0, val repeatAddToAddress: Int = 1,
) {
    /** The fields in native `CheatMemPatch` order, for `LibretroDroid.setMemoryCheats`. */
    fun packed(): IntArray = intArrayOf(
        address.toInt(), value.toInt(), size, type, bitMask, if (bigEndian) 1 else 0, repeatCount, repeatAddToValue, repeatAddToAddress,
    )
}

/**
 * RetroArch `.cht` files, read the way nx-redux's minarch does (`ma_cheats.c`): a `cheats = N` line (anywhere: RetroArch's
 * cheat search writes it last), then `cheatN_desc = "…"`, `cheatN_code = "…"`, `cheatN_enable = true|false` and the
 * RAM-patch fields ([MemoryPatch]). Other keys are ignored, and a line that does not parse is skipped, never fatal.
 */
object CheatFile {
    /** Sanity cap on `cheats = N` (the array is allocated up front). */
    const val MAX_CHEATS = 4096

    // The count may be quoted (`cheats = "213"`: every PSP file in libretro's collection, and a few hundred others).
    // RetroArch reads either; minarch's fscanf("%lu") stops at the quote and loses those files.
    private val COUNT = Regex("""^cheats\s*=\s*("?)(\d+)\1$""")
    private val KEY = Regex(
        """^\s*cheat(\d+)_(desc|code|enable|handler|address|value|memory_search_size|cheat_type|address_bit_position|""" +
            """big_endian|repeat_count|repeat_add_to_value|repeat_add_to_address)\s*=\s*""",
    )

    /**
     * Null when the count line is missing, unparsable or zero. Otherwise the cheats that have a code or `handler = 1`,
     * in index order, one without a desc named `Cheat <n>` from 1 as the handheld names it; an empty list means the
     * file parsed but holds nothing usable.
     */
    fun parse(text: String): List<Cheat>? {
        val lines = TextLines.split(text)
        // The first line that is a count line, as minarch finds it.
        val countLine = lines.firstOrNull { it.trimStart().let { l -> l.startsWith("cheats") && (l.getOrNull(6)?.let { c -> c.isWhitespace() || c == '=' } ?: false) } }
            ?: return null
        val count = COUNT.matchEntire(countLine.trim())?.groupValues?.get(2)?.toIntOrNull() ?: return null
        if (count <= 0) return null
        val n = minOf(count, MAX_CHEATS)
        val descs = arrayOfNulls<String>(n)
        val codes = arrayOfNulls<String>(n)
        val enabled = BooleanArray(n)
        val handler = BooleanArray(n)
        val mems = Array(n) { MemoryPatch() }
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
                "big_endian" -> mems[index] = mems[index].copy(bigEndian = flag(value))
                else -> number(value)?.let { v ->
                    val p = mems[index]
                    when (m.groupValues[2]) {
                        "handler" -> handler[index] = v == 1L
                        "address" -> mems[index] = p.copy(address = v)
                        "value" -> mems[index] = p.copy(value = v)
                        "memory_search_size" -> mems[index] = p.copy(size = v.toInt())
                        "cheat_type" -> mems[index] = p.copy(type = v.toInt())
                        "address_bit_position" -> mems[index] = p.copy(bitMask = v.toInt())
                        "repeat_count" -> mems[index] = p.copy(repeatCount = v.toInt())
                        "repeat_add_to_value" -> mems[index] = p.copy(repeatAddToValue = v.toInt())
                        "repeat_add_to_address" -> mems[index] = p.copy(repeatAddToAddress = v.toInt())
                    }
                }
            }
        }
        return (0 until n).mapNotNull { i ->
            val desc = descs[i]?.takeIf { it.isNotEmpty() } ?: "Cheat ${i + 1}"
            if (handler[i]) return@mapNotNull Cheat(desc, "", enabled[i], mems[i])
            val code = codes[i]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Cheat(desc, code, enabled[i])
        }
    }

    /**
     * A number value, quoted or not, read like C's `strtoul(s, 0)` (RetroArch's): `0x` hex, a leading `0` octal, else
     * decimal, up to the first character that isn't a digit, then cut to 32 bits like minarch's `(unsigned)`. Null when
     * it doesn't start with a digit.
     */
    private fun number(value: String): Long? {
        val s = value.removePrefix("\"")
        if (s.isEmpty() || !s[0].isDigit()) return null
        val (radix, digits) = when {
            s.startsWith("0x", ignoreCase = true) -> 16 to s.drop(2)
            s.startsWith("0") -> 8 to s.drop(1)
            else -> 10 to s
        }
        var acc = 0UL
        for (c in digits) {
            val d = c.digitToIntOrNull(radix) ?: break
            acc = if (acc > (ULong.MAX_VALUE - d.toULong()) / radix.toULong()) ULong.MAX_VALUE else acc * radix.toULong() + d.toULong()
        }
        return (acc and 0xFFFFFFFFUL).toLong()
    }

    /** `true` (any case) or `1`, quoted or not, as RetroArch writes and reads flags. */
    private fun flag(value: String): Boolean {
        val s = value.removePrefix("\"")
        return s.startsWith("true", ignoreCase = true) || s.startsWith("1")
    }

    /** The HTML entities the libretro database uses in descriptions (`P1 &amp; P2`), as minarch decodes them. */
    private val ENTITIES = listOf(
        "&quot;" to "\"", "&amp;" to "&", "&apos;" to "'", "&lt;" to "<", "&gt;" to ">", "&nbsp;" to " ", "&eacute;" to "é",
    )

    /** The content of a `"…"` value: `\x` → `x`, [ENTITIES] decoded. Null when not quoted or unterminated. */
    private fun quoted(value: String): String? {
        if (!value.startsWith('"')) return null
        val out = StringBuilder()
        var i = 1
        while (i < value.length) {
            val c = value[i]
            when {
                c == '"' -> return out.toString()
                c == '\\' && i + 1 < value.length -> { out.append(value[i + 1]); i += 2 }
                c == '&' && ENTITIES.any { value.startsWith(it.first, i) } -> {
                    val (entity, text) = ENTITIES.first { value.startsWith(it.first, i) }
                    out.append(text)
                    i += entity.length
                }
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
