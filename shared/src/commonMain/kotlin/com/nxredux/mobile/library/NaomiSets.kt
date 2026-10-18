package com.nxredux.mobile.library

/**
 * flycast's NAOMI / Atomiswave sets at the core's pin (cores/scripts/flycast-sets.py over core/hw/naomi/naomi_roms.cpp →
 * assets/system/flycast/sets.tsv): per set its parent, title and the BIOS archive it needs (`naomi`, `awbios`, `naomi2` ...:
 * flycast opens `<system>/dc/<bios>.zip` or `.7z`). A row with no title is a BIOS archive itself. Names are lowercase set
 * names without an extension; lookups ignore case.
 */
class NaomiSets private constructor(private val rows: Map<String, Row>) {
    private class Row(val title: String, val parent: String, val bios: String)

    private val biosArchives: Set<String> = rows.values.mapTo(HashSet()) { it.bios }

    val isEmpty: Boolean get() = rows.isEmpty()

    private fun game(set: String): Row? = rows[set.lowercase()]?.takeIf { it.title.isNotEmpty() }

    fun title(set: String): String? = game(set)?.title

    /** The set [set] is a clone of (flycast opens `<its folder>/<parent>.zip` or `.7z` beside it); null for a parent, an unknown set or a BIOS. */
    fun parent(set: String): String? = game(set)?.parent?.takeIf { it.isNotEmpty() }

    /** The BIOS archive (without extension) [set] loads from; null for an unknown set or a BIOS. */
    fun biosFor(set: String): String? = game(set)?.bios

    /** [name] (a file's base name) is a BIOS archive (naomi, awbios ...): needed in Bios/DC/, never a game. */
    fun isBios(name: String): Boolean = name.lowercase() in biosArchives

    /**
     * The archive [fileName] is one of flycast's sets, which it loads as is (it finds the game by the archive's base name,
     * naomi_cart.cpp FindGame); any other zip/7z (a zipped disc) is unpacked like another console's. With no table in the
     * build every archive is loaded as is.
     */
    fun loadsAsIs(fileName: String): Boolean = isEmpty || game(Names.stripExtension(fileName)) != null

    /** The BIOS [fileName]'s set needs when neither `<bios>.zip` nor `.7z` is in [present] (case-sensitive, as flycast); null when met or unknown. */
    fun missingBios(fileName: String, present: Set<String>): String? {
        val bios = biosFor(Names.stripExtension(fileName)) ?: return null
        return bios.takeIf { "$it.zip" !in present && "$it.7z" !in present }
    }

    companion object {
        val EMPTY = NaomiSets(emptyMap())

        fun biosMessage(title: String, bios: String): String = "$title needs $bios.zip: put it in Bios/DC/"

        fun parse(text: String): NaomiSets {
            val rows = HashMap<String, Row>()
            for (line in text.lineSequence()) {
                val f = line.split('\t')
                val name = f[0].trim().lowercase()
                val bios = f.getOrNull(3)?.trim()?.lowercase().orEmpty()
                if (name.isEmpty() || bios.isEmpty()) continue
                rows[name] = Row(f[2].trim(), f[1].trim().lowercase(), bios)
            }
            return NaomiSets(rows)
        }
    }
}
