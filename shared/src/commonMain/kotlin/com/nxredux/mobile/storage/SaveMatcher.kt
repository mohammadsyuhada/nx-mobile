package com.nxredux.mobile.storage

import com.nxredux.mobile.library.IndexedGame
import com.nxredux.mobile.library.LibraryIndex
import com.nxredux.mobile.runner.Console
import com.nxredux.mobile.runner.CoreRegistry

/** Which library game a loose save belongs to. */
sealed interface SaveMatch {
    /** The one game: [rom] is the library ROM file name on [console]. */
    data class Matched(val console: Console, val rom: String) : SaveMatch

    /** No library game has that name. */
    data object NoGame : SaveMatch

    /** Games of several [consoles] (sorted by display name) have that name. */
    data class Ambiguous(val consoles: List<Console>) : SaveMatch
}

/** Maps a loose save's game key ([SaveKeys.romKey]) to one library game. */
class SaveMatcher(private val index: LibraryIndex?) {
    /** Console → game key → that console's games with the key. Unassigned and unknown tags are left out. */
    private val byConsole: Map<Console, Map<String, List<IndexedGame>>> by lazy {
        val out = LinkedHashMap<Console, MutableMap<String, MutableList<IndexedGame>>>()
        index?.systems?.forEach { s ->
            if (s.unassigned) return@forEach
            val console = CoreRegistry.consoleOf(s.tag) ?: return@forEach
            val keys = out.getOrPut(console) { HashMap() }
            s.games.forEach { keys.getOrPut(SaveKeys.romKey(it.file)) { ArrayList() }.add(it) }
        }
        out
    }

    /** The game named [gameKey], limited to the [clue] consoles when given. */
    fun match(gameKey: String, clue: List<Console>?): SaveMatch {
        val hits = byConsole.entries
            .filter { (c, _) -> clue == null || c in clue }
            .mapNotNull { (c, keys) -> keys[gameKey]?.let { c to it } }
        return when (hits.size) {
            0 -> SaveMatch.NoGame
            1 -> SaveMatch.Matched(hits[0].first, pick(hits[0].second).file)
            else -> SaveMatch.Ambiguous(hits.map { it.first }.sortedBy { it.displayName })
        }
    }

    /** Console tag → the ROM file names of its games, as [SharedSaves.gamesByConsole]. */
    fun gamesByConsole(): Map<String, Set<String>> = SharedSaves.gamesByConsole(index)

    /** A plain ROM before an archive, the home before an extra folder, then by file name. */
    private fun pick(games: List<IndexedGame>): IndexedGame =
        games.sortedWith(
            compareBy<IndexedGame>({ it.file.substringAfterLast('.', "").lowercase() in SaveKeys.ARCHIVES },
                { it.source != Recents.HOME }, { it.file }),
        ).first()
}
