package com.nxredux.mobile.achievements

import com.nxredux.mobile.storage.GamePath

/** nx-redux `ra_badge_path.h`: a game's badges in `Roms/<Folder>/.retroachievements/badges/<game_id>/`, one folder per game. */
object RaBadgeFolders {
    const val DATA_DIR = ".retroachievements"
    private const val BADGES = "badges"
    private val badgeRe = Regex("[A-Za-z0-9_-]+")

    /** The top console folder holding a home ROM (`Roms/<Folder>/…`); null for an extra, a ROM directly in `Roms/` or a dot folder. */
    fun ownFolder(p: RaGamePointer): String? {
        if (p.source != GamePath.HOME) return null
        val parts = p.dir.split('/').filter { it.isNotEmpty() }
        if (parts.size < 2 || parts[0] != "Roms" || parts[1].startsWith(".")) return null
        return parts[1]
    }

    /** `Roms/<consoleFolder>/.retroachievements` (also where `hashes.json` lives). */
    fun dataDir(consoleFolder: String): List<String> = listOf("Roms", consoleFolder, DATA_DIR)

    /** The badge folder for [gameId]; null for 0 (an unknown game has no badges). */
    fun segments(consoleFolder: String, gameId: Int): List<String>? = if (gameId <= 0) null else dataDir(consoleFolder) + BADGES + gameId.toString()

    /** `<badge>.png` / `<badge>_lock.png`; null for a name that is not a plain badge id. */
    fun fileName(badge: String, locked: Boolean): String? = badge.takeIf { badgeRe.matches(it) }?.let { "$it${if (locked) "_lock" else ""}.png" }
}
