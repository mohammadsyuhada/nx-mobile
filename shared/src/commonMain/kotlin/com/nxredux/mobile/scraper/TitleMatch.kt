package com.nxredux.mobile.scraper

/**
 * The looser title tier tried after a source's exact match misses: SteamGridDB and TheGamesDB through [pick],
 * libretro's name list through [Libretro.bestListed]. Two steps, each only when the one before found nothing:
 * 1. [loose] keys equal: punctuation ignored, `&` = `and`, a leading "The" or a No-Intro trailing article dropped,
 *    trailing version words dropped (`v1.004`, `Rev A`, `Version`), Roman numerals II-IV, VI-IX as digits, a trailing
 *    "I" as 1 and a trailing 1 after two or more words dropped ("Sonic The Hedgehog I" = "Sonic the Hedgehog").
 * 2. A listed title's [head] (its words before a subtitle separator `:`, ` - ` or libretro's `_ `) equals the user's
 *    [loose] key, when the head has at least two words and every such hit is one title ("Marvel vs Capcom 2" =
 *    "Marvel vs. Capcom 2: New Age of Heroes"; "Street Fighter II" takes neither "...: The World Warrior" nor
 *    "...: Champion Edition" when both are listed); for an API's results only when its first result is that hit.
 * A shorter listed title never matches a longer user title, and words are never dropped from the middle, so
 * "Super Mario Bros" ≠ "Super Mario Bros. 3" and "Street Fighter II" ≠ "Street Fighter II Turbo".
 */
internal object TitleMatch {
    private val GROUP = Regex("""\s*(\([^)]*\)|\[[^\]]*\])""")
    private val ARTICLE = Regex("""^(.+?), (The|A|An)((?: - |:|_ ).*)?$""", RegexOption.IGNORE_CASE)
    private val SEPARATOR = Regex("""\s*:\s*|\s+[-–—]\s+|_\s+""")
    private val DROPPED = setOf('\'', '’', '.', '!', '?')
    private val NON_ALNUM = Regex("""[^\p{L}\p{N}]+""")
    private val V_NUMBER = Regex("""v\d+[a-z]?""")
    private val REV_VALUE = Regex("""[a-z0-9]{1,3}""")
    private val REV_WORDS = setOf("rev", "revision", "ver")
    private val ROMAN = mapOf("ii" to "2", "iii" to "3", "iv" to "4", "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9")
    private val SEARCH_VERSION = Regex("""\s+(v\d+(\.\d+)*[a-z]?|(rev|revision|ver\.?)\s+[a-z0-9.]{1,6})$""", RegexOption.IGNORE_CASE)

    /** The name without `(...)` / `[...]` groups when [tags] (else their words stay), a trailing article moved to the front. */
    private fun bare(name: String, tags: Boolean): String {
        val b = if (tags) name.replace(GROUP, "").trim() else name.trim()
        return ARTICLE.matchEntire(b)?.let { m -> "${m.groupValues[2]} ${m.groupValues[1]}${m.groupValues[3]}" } ?: b
    }

    private fun words(s: String): MutableList<String> {
        val folded = buildString(s.length) {
            for (c in s.lowercase()) when {
                c in DROPPED -> {}
                c == '&' -> append(" and ")
                else -> append(TITLE_FOLD[c] ?: c.toString())
            }
        }
        return folded.replace(TITLE_MARKS, " ").replace(NON_ALNUM, " ").trim().split(' ').filter { it.isNotEmpty() && it != "and" }.toMutableList()
    }

    /**
     * The loose key of [name] (rules in the class comment); "" when nothing is left. [tags]: its `(...)` / `[...]` groups
     * are tags and dropped (a ROM name, a No-Intro name); false for an API's title, whose groups tell games apart
     * ("Tetris (IBM DOS)", "Pokemon Emerald Version [Case Bundle]" are not "Tetris", "Pokemon Emerald").
     */
    fun loose(name: String, tags: Boolean = true): String {
        val w = words(bare(name, tags))
        if (w.size > 1 && w[0] == "the") w.removeAt(0)
        while (w.size > 1) {
            val last = w.last()
            when {
                V_NUMBER.matches(last) || last == "version" -> w.removeAt(w.lastIndex)
                w.size > 2 && w[w.size - 2] in REV_WORDS && REV_VALUE.matches(last) -> { w.removeAt(w.lastIndex); w.removeAt(w.lastIndex) }
                else -> break
            }
        }
        for (i in w.indices) ROMAN[w[i]]?.let { w[i] = it }
        if (w.size > 1 && w.last() == "i") w[w.lastIndex] = "1"
        if (w.size > 2 && w.last() == "1") w.removeAt(w.lastIndex)
        return w.joinToString(" ")
    }

    /** The [loose] key of [name]'s words before its first subtitle separator; null without one or for a one-word head. */
    fun head(name: String, tags: Boolean = true): String? {
        val b = bare(name, tags)
        val sep = SEPARATOR.find(b) ?: return null
        if (b.substring(sep.range.last + 1).isBlank()) return null
        return loose(b.substring(0, sep.range.first), tags).takeIf { ' ' in it }
    }

    /** The name a source is searched with: [Tgdb.queryName] without trailing version numbers (`v1.004`, `Rev A`). */
    fun searchTerm(name: String): String {
        var s = Tgdb.queryName(name)
        while (true) { val t = s.replace(SEARCH_VERSION, "").trim(); if (t == s || t.isEmpty()) return s; s = t }
    }

    /** Several hits are one title when their [loose] keys agree. */
    fun oneTitle(titles: List<String>, tags: Boolean = true): Boolean = titles.map { loose(it, tags) }.distinct().size == 1

    /**
     * An API's search results ([items], in its relevance order) whose [title] matches the user's [name] in the first
     * tier that has any, in the given order: exact ([normaliseTitle] of [Tgdb.queryName]), then [loose], then [head]
     * only when the **first** result is a head hit and all head hits are [oneTitle]. Titles keep their groups (tags =
     * false). The first-result rule keeps fan games and hacks out ("Pokémon FireRed: VR Missions" on TheGamesDB,
     * "Sonic the Hedgehog 2: Build a Burger" on SteamGridDB, both listed below the search's best answer).
     */
    fun <T> pick(items: List<T>, name: String, title: (T) -> String): List<T> {
        val exact = normaliseTitle(Tgdb.queryName(name))
        items.filter { normaliseTitle(title(it)) == exact }.let { if (it.isNotEmpty()) return it }
        val want = loose(name).ifEmpty { return emptyList() }
        items.filter { loose(title(it), tags = false) == want }.let { if (it.isNotEmpty()) return it }
        if (items.isEmpty() || head(title(items.first()), tags = false) != want) return emptyList()
        return items.filter { head(title(it), tags = false) == want }.takeIf { oneTitle(it.map(title), tags = false) }.orEmpty()
    }
}
