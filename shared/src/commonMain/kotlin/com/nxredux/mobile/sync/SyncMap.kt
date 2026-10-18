package com.nxredux.mobile.sync

import com.nxredux.mobile.library.Names
import com.nxredux.mobile.runner.Console
import com.nxredux.mobile.runner.CoreRegistry

/** Maps a handheld's per-emulator `Name (TAG)` folders and `Saves/<TAG>` folders onto this phone's consoles. */
object SyncMap {
    /**
     * The phone console a `Name (TAG)` folder (either device's) belongs to, by the library's own rule
     * ([CoreRegistry.consoleOfFolder]). Null for a name that is not a system folder or names no console: a shared tag
     * (`GPGX`) with a name of no console is skipped, never guessed.
     */
    fun consoleOf(folder: String): Console? {
        val (display, tag) = Names.parseSystemFolder(folder) ?: return null
        return CoreRegistry.consoleOfFolder(display, tag)
    }

    /**
     * True when this phone's emulator tag [tag] (an extra unit's, a game's) runs [folder]'s console: a phone folder's tag
     * names only its console, so any of the console's tags belongs to it (`MGBA` to `Game Boy Advance (GBA)`).
     */
    fun ofConsole(tag: String, folder: String): Boolean {
        val c = consoleOf(folder) ?: return false
        return CoreRegistry.consoleOf(tag)?.id == c.id
    }

    /**
     * Handheld save folder tags → the console tags they may hold saves of. A tag counts when it is the tag of a
     * handheld ROM folder or one of some console's tags as the handheld names it ([CoreRegistry.handheldTag]), and the
     * phone has a core for it. Its consoles are its own console plus the console of every handheld folder carrying it
     * (`Saves/GPGX` serves Genesis and Master System).
     */
    fun saveConsoles(handheldFolders: List<String>): Map<String, Set<String>> {
        val folderTags = handheldFolders.mapNotNull { f ->
            Names.parseSystemFolder(f)?.second?.uppercase()?.let { it to f }
        }
        val tags = (folderTags.map { it.first } + CoreRegistry.consoles.flatMap { it.tags }.map(CoreRegistry::handheldTag)).toSet()
        return tags.filter { CoreRegistry.forTag(it) != null }.associateWith { t ->
            val named = folderTags.filter { it.first == t }.mapNotNull { consoleOf(it.second)?.defaultTag }
            (listOf(CoreRegistry.consoleTag(t)) + named).toSet()
        }
    }
}
