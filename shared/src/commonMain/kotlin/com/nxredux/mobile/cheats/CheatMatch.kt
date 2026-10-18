package com.nxredux.mobile.cheats

/**
 * Which `.cht` files belong to a ROM, as nx-redux decides it (`ma_cheats.c` `Cheat_getPaths` and
 * `ma_cheat_match.c`): exact candidate names first, then a stem + region ranking over every file in the
 * folder that merges same-game variants from different cheat sources. Pure string work; the caller lists
 * the folder and reads the files.
 */
object CheatMatch {
    const val MAX_CANDIDATES = 16
    private const val MAX_TOKENS = 8

    /** Parenthesised tokens naming where the cheats came from; every other token is a region/version qualifier. */
    private val SOURCES = setOf(
        "code breaker", "codebreaker", "gameshark", "game shark",
        "action replay", "pro action replay", "game genie", "gamegenie",
        "rumbles", "xploder", "game buster", "gamebuster", "raw", "wip",
    )

    /**
     * nx-redux `getDisplayName`: the file name without its 2–4 character extensions (repeated, so `.p8.png` goes)
     * and without trailing `(…)` / `[…]` groups. Never empty: a name that would vanish keeps its extension-less form.
     */
    fun displayName(fileName: String): String {
        var out = fileName.substringAfterLast('/')
        while (true) {
            val dot = out.lastIndexOf('.')
            if (dot < 0 || out.length - dot !in 3..5) break   // ".gb" .. ".doom": dot plus 2–4 letters
            out = out.substring(0, dot)
        }
        val beforeGroups = out
        while (true) {
            val i = out.lastIndexOf('(').takeIf { it >= 0 } ?: out.lastIndexOf('[')
            if (i <= 0) break
            out = out.substring(0, i)
        }
        // Fallbacks after trimming, so a name that is only groups or blanks never comes back empty.
        out = out.trimEnd()
        if (out.isEmpty()) out = beforeGroups.trimEnd()
        if (out.isEmpty()) return fileName
        return out
    }

    /**
     * Exact file names to try, most likely first: `<rom.ext>.cht`, `<rom>.<coreExt>.cht` per core extension
     * (a zipped ROM's cheat file is named after the inner file), `<rom>.cht`, `<displayName>.cht`, `<title>.cht`.
     */
    fun candidates(romFileName: String, title: String, coreExtensions: Set<String>): List<String> {
        val out = LinkedHashSet<String>()
        out += "$romFileName.cht"
        val dot = romFileName.lastIndexOf('.')
        if (dot >= 0 && romFileName.length - dot in 3..5) {
            val stem = romFileName.substring(0, dot)
            coreExtensions.map { it.lowercase() }.filter { it != "zip" }.sorted().forEach { out += "$stem.$it.cht" }
            out += "$stem.cht"   // the whole stem (GoodTools `Tetris [!].cht`); the cleaned name follows
        }
        out += "${displayName(romFileName)}.cht"
        if (title.isNotBlank()) out += "$title.cht"
        return out.take(MAX_CANDIDATES)
    }

    /** `Contra (USA) (GameShark).cht` → `GameShark`; empty when the name carries no source tag. */
    fun sourceLabel(baseName: String): String = parse(baseName).source

    /**
     * The files to merge for a ROM shown as [displayName] (its region-stripped name) whose full name is
     * [romFileName]: only `.cht` files whose stem equals [displayName] (case-insensitive) qualify; the group
     * sharing the most region tokens with the ROM wins, then the one with the fewest extra tokens, then the
     * lexically smallest key. Files without a source tag come first, then the rest in input order. Empty when
     * nothing qualifies.
     */
    fun select(displayName: String, romFileName: String, files: List<String>): List<String> {
        val stem = displayName.trim()
        val rom = parse(romFileName)
        val parsed = files.filter { it.endsWith(".cht", ignoreCase = true) }.map { it to parse(it) }
            .filter { (_, p) -> p.stem.equals(stem, ignoreCase = true) }
        if (parsed.isEmpty()) return emptyList()
        data class Rank(val overlap: Int, val extra: Int, val key: String)
        fun rank(p: Parsed): Rank {
            val overlap = p.quals.count { it in rom.quals }
            return Rank(overlap, p.quals.size - overlap, p.groupKey())
        }
        val best = parsed.map { (_, p) -> rank(p) }
            .reduce { a, b -> if (b.overlap != a.overlap) (if (b.overlap > a.overlap) b else a) else if (b.extra != a.extra) (if (b.extra < a.extra) b else a) else if (b.key < a.key) b else a }
        return noSourceFirst(parsed.filter { (_, p) -> p.groupKey() == best.key })
    }

    /**
     * Every `.cht` file in [files] (case-insensitive extension) in the same game+region group as [name]:
     * same stem and the same qualifiers, whatever their cheat source. [name] itself is included when it is in
     * [files]. Files without a source tag come first, then the rest in input order (as [select] orders them).
     */
    fun variants(name: String, files: List<String>): List<String> {
        val key = parse(name).groupKey()
        return noSourceFirst(
            files.filter { it.endsWith(".cht", ignoreCase = true) }.map { it to parse(it) }.filter { (_, p) -> p.groupKey() == key },
        )
    }

    /** The names of [group], those without a source tag first, each part keeping input order. */
    private fun noSourceFirst(group: List<Pair<String, Parsed>>): List<String> {
        val (plain, sourced) = group.partition { (_, p) -> p.source.isEmpty() }
        return (plain + sourced).map { it.first }
    }

    private class Parsed(val stem: String, val quals: List<String>, val source: String) {
        /** Stem plus sorted qualifiers: files with the same key are one game+region group. */
        // Each qualifier followed by a comma, as the C code writes it, so `rev` vs `rev 1` ties-break the same way.
        fun groupKey() = stem + "|" + quals.sorted().joinToString("") { "$it," }
    }

    /** `Name (USA, Europe) (GameShark).cht` → stem `Name`, quals `[usa, europe]`, source `GameShark`. */
    private fun parse(baseName: String): Parsed {
        val work = if (baseName.endsWith(".cht", ignoreCase = true)) baseName.dropLast(4) else baseName
        val paren = work.indexOf(" (")
        val stem = (if (paren >= 0) work.substring(0, paren) else work).trim()
        val quals = ArrayList<String>()
        var source = ""
        var i = work.indexOf('(')
        while (i >= 0) {
            val end = work.indexOf(')', i)
            if (end < 0) break
            work.substring(i + 1, end).split(", ").forEach { raw ->
                val tok = raw.trim()
                if (tok.isEmpty()) return@forEach
                val lc = tok.lowercase()
                if (lc in SOURCES) { if (source.isEmpty()) source = tok }
                else if (quals.size < MAX_TOKENS) quals += lc
            }
            i = work.indexOf('(', end + 1)
        }
        return Parsed(stem, quals, source)
    }
}
