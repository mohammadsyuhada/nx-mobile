package com.nxredux.mobile.library

/**
 * FBNeo's arcade sets at the core's pin (cores/scripts/fbneo-sets.py → assets/system/fbneo/sets.tsv): per set its DAT
 * title, parent (`cloneof`), the set its ROMs come from (`romof`: the parent, or the BIOS such as neogeo) and whether it
 * is a BIOS. Names are lowercase set names without an extension; lookups ignore case.
 */
class ArcadeSets private constructor(private val rows: Map<String, Row>) {
    private class Row(val title: String, val cloneof: String, val romof: String, val bios: Boolean)

    val isEmpty: Boolean get() = rows.isEmpty()

    fun title(name: String): String? = rows[name.lowercase()]?.title?.takeIf { it.isNotEmpty() }

    fun isBios(name: String): Boolean = rows[name.lowercase()]?.bios == true

    /** The sets [name] loads ROMs from, nearest first (clone → parent → BIOS); a repeat ends the walk. */
    fun chain(name: String): List<String> {
        val out = ArrayList<String>()
        val seen = hashSetOf(name.lowercase())
        var row = rows[name.lowercase()]
        while (row != null) {
            val next = row.romof.ifEmpty { row.cloneof }
            if (next.isEmpty() || !seen.add(next)) break
            out += next
            row = rows[next]
        }
        return out
    }

    companion object {
        val EMPTY = ArcadeSets(emptyMap())

        fun parse(text: String): ArcadeSets {
            val rows = HashMap<String, Row>()
            for (line in text.lineSequence()) {
                val f = line.split('\t')
                val name = f.getOrNull(0)?.trim()?.lowercase().orEmpty()
                if (f.size < 2 || name.isEmpty()) continue
                rows[name] = Row(
                    f[1].trim(),
                    f.getOrNull(2)?.trim()?.lowercase().orEmpty(),
                    f.getOrNull(3)?.trim()?.lowercase().orEmpty(),
                    f.getOrNull(4)?.trim() == "1",
                )
            }
            return ArcadeSets(rows)
        }
    }
}
