package com.nxredux.mobile.cheats

/**
 * Turns a cheat folder's file names plus a reader into the [CheatSet] for a ROM: the first exact candidate
 * (`CheatMatch.candidates`) that reads and parses to at least one cheat wins, merged with the other files of
 * its own game+region group (`CheatMatch.variants`, e.g. its `(GameShark)` sibling); then, when the platform read the
 * game's serial (PSP), every file tagged `[<serial>]`; otherwise the wildcard stage (`CheatMatch.select`) merges every
 * file of the best game+region group, and when no stem is exact, the first file whose name starts with the game's
 * (nx-redux's glob `<name>*.cht` falling back to its first hit). Pure: the platform lists and reads.
 */
object CheatResolver {

    /**
     * [files] are the base names in `Cheats/<TAG>/`; [read] returns a file's text or null when it can't be read.
     * An exact candidate that loads brings in its same-group variants, in `CheatMatch.variants` order (files
     * without a source tag first). A file that fails to parse is skipped (the next candidate, or the other files
     * of the group, still load); [onSkipped] hears about each file that read but was rejected, with a short
     * reason (not about failed reads).
     */
    fun resolve(
        files: List<String>,
        romFileName: String,
        title: String,
        coreExtensions: Set<String>,
        read: (String) -> String?,
        serial: String? = null,
        onSkipped: (name: String, reason: String) -> Unit = { _, _ -> },
    ): CheatSet {
        val candidates = CheatMatch.candidates(romFileName, title, coreExtensions)
        val expected = candidates.first()
        val present = files.toSet()
        for (name in candidates) {
            if (name !in present) continue
            val cheats = load(name, read, onSkipped) ?: continue
            val loaded = CheatMatch.variants(name, files).mapNotNull { other ->
                if (other == name) name to cheats else load(other, read, onSkipped)?.let { other to it }
            }
            return CheatSet(merge(loaded), expected)
        }
        if (serial != null) {
            val tag = "[$serial]"
            val loaded = files.filter { it.endsWith(".cht", ignoreCase = true) && it.contains(tag, ignoreCase = true) }.sorted()
                .mapNotNull { name -> load(name, read, onSkipped)?.let { name to it } }
            if (loaded.isNotEmpty()) return CheatSet(merge(loaded), expected)
        }
        val display = CheatMatch.displayName(romFileName)
        // A map.txt alias (the launcher's title) can name the game when the file name doesn't (`1941.zip`).
        val names = listOf(display) + listOf(title).filter { it.isNotBlank() && !it.equals(display, ignoreCase = true) }
        for (name in names) {
            val selected = CheatMatch.select(name, romFileName, files)
            if (selected.isEmpty()) continue
            val loaded = selected.mapNotNull { file -> load(file, read, onSkipped)?.let { file to it } }
            return CheatSet(merge(loaded), expected)
        }
        // No stem is exact (`Pokemon - Yellow.gbc` against `Pokemon - Yellow Version - … (USA, Europe).cht`): the first
        // file, in name order, that starts with the game's name and loads.
        for (name in names) {
            for (file in files.filter { it.endsWith(".cht", ignoreCase = true) && it.startsWith(name, ignoreCase = true) }.sorted()) {
                load(file, read, onSkipped)?.let { return CheatSet(merge(listOf(file to it)), expected) }
            }
        }
        return CheatSet(emptyList(), expected)
    }

    /** The files' cheats in order; with more than one file, each cheat's info is prefixed `(<source>) ` when its file has one. */
    fun merge(loaded: List<Pair<String, List<Cheat>>>): List<CheatEntry> {
        val multi = loaded.size > 1
        return loaded.flatMap { (name, cheats) ->
            val source = CheatMatch.sourceLabel(name)
            cheats.map { c ->
                val info = if (multi && source.isNotEmpty()) "($source) ${c.desc}" else c.desc
                CheatEntry(c.desc, c.code, info, c.enabled)
            }
        }
    }

    /** Null when unreadable, rejected, or empty: "try the next one". */
    private fun load(name: String, read: (String) -> String?, onSkipped: (String, String) -> Unit): List<Cheat>? {
        val text = read(name) ?: return null   // the platform reader already reports its own failures
        val cheats = CheatFile.parse(text)
        when {
            cheats == null -> onSkipped(name, "not a cheat file")
            cheats.isEmpty() -> onSkipped(name, "no usable cheats")
            else -> return cheats
        }
        return null
    }
}
