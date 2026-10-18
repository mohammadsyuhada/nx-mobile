package com.nxredux.mobile.storage

import com.nxredux.mobile.library.LibraryIndex
import com.nxredux.mobile.runner.CoreRegistry

/**
 * A save in a handheld folder several consoles share (`Saves/GPGX`: the GPGX pak's Genesis, Master System, Game Gear,
 * Sega CD and SG-1000 games) belongs to the console of its game, found by ROM name in this phone's library, as Device
 * Sync's save plan finds it in the handheld's ROM folders ([SaveKeys.romKey] against each console's games).
 */
object SharedSaves {
    /** Console tag → the ROM file names the library lists for that console (Unassigned left out). */
    fun gamesByConsole(index: LibraryIndex?): Map<String, Set<String>> {
        val out = HashMap<String, MutableSet<String>>()
        index?.systems?.forEach { s ->
            if (s.unassigned || CoreRegistry.consoleOf(s.tag) == null) return@forEach
            s.games.mapTo(out.getOrPut(CoreRegistry.consoleTag(s.tag)) { HashSet() }) { it.file }
        }
        return out
    }

    /**
     * The console tag [saveName] (a battery save or clock file at the top of `Saves/[sharedTag]`) belongs to: the one
     * console the handheld folder serves whose games ([games], [gamesByConsole]) hold its game. Null when [sharedTag] is
     * not shared, the name is no save, or no console or several do: the caller keeps its fallback.
     */
    fun consoleOf(sharedTag: String, saveName: String, games: Map<String, Set<String>>): String? {
        val t = sharedTag.trim().uppercase()
        if (t !in CoreRegistry.SHARED_HANDHELD_TAGS) return null
        val key = SaveKeys.of(saveName) ?: return null
        val served = CoreRegistry.consoles.filter { c -> c.tags.any { CoreRegistry.handheldTag(it) == t } }.map { it.defaultTag }
        return served.filter { c -> games[c].orEmpty().any { SaveKeys.romKey(it) == key.game } }.singleOrNull()
    }
}
