package com.nxredux.mobile.cheats

import com.nxredux.mobile.options.ConfigSource
import com.nxredux.mobile.options.FrontendOptions

/** One row of the Cheats page. [info] is the full text A shows (the desc, prefixed with its source when files were merged). */
data class CheatEntry(val desc: String, val code: String, val info: String, val enabled: Boolean)

/** The cheats resolved for the running game. [expected] is the first file name looked for (shown when nothing matched). */
data class CheatSet(val entries: List<CheatEntry>, val expected: String) {

    fun toggle(index: Int, enabled: Boolean): CheatSet =
        if (index !in entries.indices) this else copy(entries = entries.mapIndexed { i, e -> if (i == index) e.copy(enabled = enabled) else e })

    /**
     * What Save Changes stores: the enabled cheats by [keys] (normalised info: the desc with its source prefix when files
     * were merged), so same-named cheats from different sources stay distinct.
     */
    fun enabledKeys(): List<String> = keys().filterIndexed { i, _ -> entries[i].enabled }

    /** What the core receives, in order: index i of the apply loop is the i-th of these (no gaps, PCSX-ReARMed #116). */
    fun enabledCodes(): List<String> = entries.filter { it.enabled && it.code.isNotEmpty() }.map { it.code }

    /** Every entry enabled iff its normalised info is in [saved]; the file's `cheatN_enable` seeds are ignored. */
    fun withSaved(saved: List<String>): CheatSet {
        val on = saved.map(::normalise).toSet()
        val keys = keys()
        return copy(entries = entries.mapIndexed { i, e -> e.copy(enabled = keys[i] in on) })
    }

    /**
     * Each entry's stored key: its normalised info, and for the nth entry sharing it (n >= 2) `info#n`, so repeats toggle
     * apart. The first keeps the bare info.
     */
    private fun keys(): List<String> {
        val seen = HashMap<String, Int>()
        return entries.map { e ->
            val key = normalise(e.info)
            val n = (seen[key] ?: 0) + 1
            seen[key] = n
            if (n == 1) key else "$key#$n"
        }
    }

    companion object {
        val EMPTY = CheatSet(emptyList(), "")
        private const val SEPARATOR = "|"

        /** The stored form of a cheat's text: trimmed, with the separator character replaced. */
        fun normalise(text: String): String = text.trim().replace('|', '/')

        fun encode(keys: List<String>): String = keys.joinToString(SEPARATOR)

        fun decode(value: String): List<String> = value.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

        /** The saved enabled set, only when the config in use is the game file (a console file never carries it). */
        fun savedFrom(values: Map<String, String>, source: ConfigSource): List<String>? =
            if (source == ConfigSource.GAME) values[FrontendOptions.CHEATS]?.let(::decode) else null
    }
}
