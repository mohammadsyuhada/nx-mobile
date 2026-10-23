package com.nxredux.mobile.storage

/** Rewrites home game keys to an extra folder's keys (when a home becomes an extra), and back. */
object GameKeys {
    private const val ROMS = "/Roms/"

    /** The `/Extra/<extraId>/...` key of a `/Roms/...` home [path]; null for any other path. */
    fun toExtra(path: String, extraId: String): String? =
        if (path.startsWith(ROMS)) "/Extra/$extraId/" + path.removePrefix(ROMS) else null

    /** The `/Roms/...` home key of the extra [extraId]'s `/Extra/<extraId>/...` [path] (its folder is the home's Roms again); null for any other path. */
    fun toHome(path: String, extraId: String): String? {
        val prefix = "/Extra/$extraId/"
        return if (path.startsWith(prefix)) ROMS + path.removePrefix(prefix) else null
    }

    /** [text] with the path before an optional tab rewritten on each line; the rest of the line (the recents alias) is kept. */
    fun rewriteLines(text: String, extraId: String): String = rewrite(text) { toExtra(it, extraId) }

    /** [rewriteLines] the other way: the extra [extraId]'s keys back to home keys ([toHome]). */
    fun rewriteLinesToHome(text: String, extraId: String): String = rewrite(text) { toHome(it, extraId) }

    private fun rewrite(text: String, key: (String) -> String?): String = text.split("\n").joinToString("\n") { line ->
        val tab = line.indexOf('\t')
        val path = if (tab < 0) line else line.substring(0, tab)
        (key(path) ?: path) + if (tab < 0) "" else line.substring(tab)
    }
}
