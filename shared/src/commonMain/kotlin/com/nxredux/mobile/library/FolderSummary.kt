package com.nxredux.mobile.library

import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.storage.Recents

/**
 * An extra folder's games after a scan: [games] in all, [byConsole] (display name, count; largest first), [unassigned].
 * For the home ([homeSummary]): [outside] of them lie outside its `Roms/`, and [split] consoles span several folders.
 */
data class FolderSummary(
    val games: Int, val byConsole: List<Pair<String, Int>>, val unassigned: Int, val outside: Int = 0,
    /** The home: consoles whose games lie in more than one `Roms/` folder ([splitFolders]). */
    val split: Int = 0,
)

/** [extraId]'s games in [index], per console and unassigned. */
fun folderSummary(index: LibraryIndex, extraId: String): FolderSummary {
    var unassigned = 0
    val counts = ArrayList<Pair<String, Int>>()
    for (system in index.systems) {
        val n = system.games.count { it.source == extraId }
        if (n == 0) continue
        if (system.unassigned) unassigned += n else counts += system.displayName to n
    }
    val byConsole = counts.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
    return FolderSummary(byConsole.sumOf { it.second } + unassigned, byConsole, unassigned)
}

/**
 * The home's games: those in its `Roms/` ([index]'s home games) and the placed ones [outside] it
 * ([LibraryScanner.scanOutsideRoms]), per console by the index's name for it (else the console's); files nothing places
 * are not counted. The home has no unassigned games.
 */
fun homeSummary(index: LibraryIndex, outside: List<LooseFile>): FolderSummary {
    val counts = LinkedHashMap<String, Int>()
    val names = HashMap<String, String>()
    for (system in index.systems) {
        if (system.unassigned) continue
        val key = CoreRegistry.consoleTag(system.tag)
        names.putIfAbsent(key, system.displayName)
        val n = system.games.count { it.source == Recents.HOME }
        if (n > 0) counts[key] = (counts[key] ?: 0) + n
    }
    var moved = 0
    for (f in outside) {
        val tag = f.tag ?: continue
        val key = CoreRegistry.consoleTag(tag)
        names.putIfAbsent(key, CoreRegistry.consoleOf(tag)?.displayName ?: tag)
        counts[key] = (counts[key] ?: 0) + 1
        moved++
    }
    val byConsole = counts.map { (k, n) -> names.getValue(k) to n }
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
    return FolderSummary(byConsole.sumOf { it.second }, byConsole, 0, outside = moved)
}

/** "SNES (40) · GBA (35)": the per-console counts on one line. */
fun FolderSummary.line(): String = byConsole.joinToString(" · ") { (name, n) -> "$name ($n)" }   // in brackets: "PlayStation 2" would read as the console

/** Where [n] unassigned games are, or null when there are none. */
fun unassignedHint(n: Int): String? = when {
    n <= 0 -> null
    n == 1 -> "1 game needs a console. Find them in Settings → Library → Unassigned games."
    else -> "$n games need a console. Find them in Settings → Library → Unassigned games."
}
