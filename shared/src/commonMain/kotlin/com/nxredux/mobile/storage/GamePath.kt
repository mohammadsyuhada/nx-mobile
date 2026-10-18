package com.nxredux.mobile.storage

/** Where a game's file lives: [sourceId] is [GamePath.HOME] or an extra folder's id; [relativeDir] is "" at an extra's root. */
data class GameRef(val sourceId: String, val relativeDir: String, val fileName: String)

/**
 * The one path scheme for recent.txt, shortcuts.txt, hidden.txt and collection files. A home game is the handheld's
 * SD-relative path (`/Roms/<System (TAG)>/<file>`, a disc folder's sheet `/Roms/<System (TAG)>/<D>/<D>.m3u`); an
 * extra's game is `/Extra/<id>/<dir>/<file>`, which the handheld skips as a missing file.
 */
object GamePath {
    const val HOME = "home"
    private const val ROMS = "/Roms/"
    private const val EXTRA = "/Extra/"

    fun of(ref: GameRef): String = when (ref.sourceId) {
        HOME -> "/${ref.relativeDir}/${ref.fileName}"
        else -> EXTRA + ref.sourceId + "/" + (if (ref.relativeDir.isEmpty()) "" else ref.relativeDir + "/") + ref.fileName
    }

    /** Inverse of [of]; null for other roots and malformed paths (no system folder, no file name). */
    fun split(path: String): GameRef? {
        if (path.startsWith(ROMS)) {
            val last = path.lastIndexOf('/')
            val file = path.substring(last + 1)
            if (file.isEmpty()) return null
            val dir = path.substring(1, last)                     // "Roms/<System (TAG)>[/<D>]"
            if (dir.count { it == '/' } < 1 || dir.endsWith("/")) return null   // needs Roms + a system folder
            return GameRef(HOME, dir, file)
        }
        if (!path.startsWith(EXTRA)) return null
        val rest = path.substring(EXTRA.length)
        val slash = rest.indexOf('/')
        if (slash <= 0) return null
        val id = rest.substring(0, slash)
        val tail = rest.substring(slash + 1)
        if (tail.isEmpty() || tail.endsWith("/")) return null
        val last = tail.lastIndexOf('/')
        return if (last < 0) GameRef(id, "", tail) else GameRef(id, tail.substring(0, last), tail.substring(last + 1))
    }
}
