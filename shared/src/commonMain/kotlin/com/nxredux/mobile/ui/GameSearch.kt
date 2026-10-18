package com.nxredux.mobile.ui

import com.nxredux.mobile.library.SystemFolder

/**
 * START's search over the library. NX Redux matches "label (TAG)" and the filename as one substring; here the query and
 * each game are normalised ([normalize]) and every query word may sit inside any word of the name, the tag or the
 * filename, in any order. No typo tolerance: a big library makes short near-misses noise.
 */
object GameSearch {
    /** Lowercase, accents folded, every run of anything but letters and digits one space, standalone II–XV as digits. */
    fun normalize(text: String): String = words(text).joinToString(" ") { ROMAN[it] ?: it }

    /** [normalize]'s words before the numerals: a query word may be meant as letters ("vi" in "Virtua"). */
    private fun words(text: String): List<String> {
        val out = StringBuilder(text.length)
        var gap = true   // collapses a run of separators into one space, and drops leading ones
        for (c in text.lowercase()) {
            for (f in FOLD[c] ?: c.toString()) {
                // NFD names (a Mac-copied card) carry accents as combining marks: drop them, don't split the word.
                if (f.category in MARKS) continue
                if (f.isLetterOrDigit()) { out.append(f); gap = false }
                else if (!gap) { out.append(' '); gap = true }
            }
        }
        return out.split(' ').filter { it.isNotEmpty() }
    }

    /**
     * Every game of [systems] matching [query], ranked: names starting with the query (spaces aside: "pacman" starts
     * "Pac-Man"), then names where every query word starts a word, then the rest (a word's middle, the tag, the
     * filename); A–Z by shown name, then tag, within each. A blank query finds nothing.
     */
    fun search(systems: List<SystemFolder>, query: String): List<Listed> =
        ranked(systems.asSequence().flatMap { s -> s.games.asSequence().map { Listed(s, it) } }, query)

    /** A game list's own search: the [entries] matching [query], ranked as [search] ranks the library's. */
    fun filter(entries: List<Listed>, query: String): List<Listed> = ranked(entries.asSequence(), query)

    private fun ranked(entries: Sequence<Listed>, query: String): List<Listed> {
        val raw = words(query)
        if (raw.isEmpty()) return emptyList()
        // Each word as a numeral and as typed: "vi" finds "Final Fantasy VI" and "Virtua Fighter" alike.
        val words = raw.map { w -> listOfNotNull(ROMAN[w], w) }
        val queries = listOf(words.joinToString(" ") { it.first() }, raw.joinToString(" ")).distinct()
        return entries
            .mapNotNull { e -> rank(e, queries, words)?.let { e to it } }
            .sortedWith(compareBy({ it.second }, { it.first.game.shownName.lowercase() }, { it.first.system.tag }))
            .map { it.first }
            .toList()
    }

    /**
     * [e]'s group (0 best), or null when a query word (in none of its forms, [words]) is in none of its name, tag or
     * filename. [queries]: the whole query converted and as typed.
     */
    private fun rank(e: Listed, queries: List<String>, words: List<List<String>>): Int? {
        val name = normalize(e.game.shownName)
        val file = normalize(e.game.fileName.substringBeforeLast('.'))
        val nameWords = name.split(' ')
        val hay = nameWords + normalize(e.system.tag).split(' ') + file.split(' ')
        // Glued forms too ("pacman" in "ms pac man"): a word may span the name's or the filename's spaces.
        val gluedName = name.replace(" ", "")
        val glued = listOf(gluedName, file.replace(" ", ""))
        if (!words.all { forms -> forms.any { w -> hay.any { it.contains(w) } || glued.any { it.contains(w) } } }) return null
        return when {
            // "pacman" starts "Pac-Man" as much as "pac man" does: not mid-word noise.
            queries.any { name.startsWith(it) || gluedName.startsWith(it.replace(" ", "")) } -> 0
            words.all { forms -> forms.any { w -> nameWords.any { it.startsWith(w) } } } -> 1
            else -> 2
        }
    }

    private val MARKS = setOf(CharCategory.NON_SPACING_MARK, CharCategory.ENCLOSING_MARK, CharCategory.COMBINING_SPACING_MARK)

    /** II–XV as digits; I, V and X alone stay letters ("Mega Man X", "Final Fantasy V", a lone "I"). */
    private val ROMAN = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9",
        "xi" to "11", "xii" to "12", "xiii" to "13", "xiv" to "14", "xv" to "15",
    )

    /** Latin-1 and Latin Extended-A letters to plain ASCII (lowercase input): ROM names' accents. */
    private val FOLD: Map<Char, String> = buildMap {
        fun fold(chars: String, to: String) = chars.forEach { put(it, to) }
        fold("àáâãäåāăą", "a"); fold("çćĉċč", "c"); fold("ďđð", "d"); fold("èéêëēĕėęě", "e")
        fold("ĝğġģ", "g"); fold("ĥħ", "h"); fold("ìíîïĩīĭįı", "i"); fold("ĵ", "j"); fold("ķ", "k")
        fold("ĺļľŀł", "l"); fold("ñńņňŉ", "n"); fold("òóôõöøōŏő", "o"); fold("ŕŗř", "r")
        fold("śŝşš", "s"); fold("ţťŧ", "t"); fold("ùúûüũūŭůűų", "u"); fold("ŵ", "w"); fold("ýÿŷ", "y"); fold("źżž", "z")
        fold("ß", "ss"); fold("æ", "ae"); fold("œ", "oe"); fold("þ", "th")
    }
}
