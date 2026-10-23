package com.nxredux.mobile.storage

/** Rewrites home game keys to an extra folder's keys (when a home becomes an extra). */
object GameKeys {
    private const val ROMS = "/Roms/"

    /** The `/Extra/<extraId>/...` key of a `/Roms/...` home [path]; null for any other path. */
    fun toExtra(path: String, extraId: String): String? =
        if (path.startsWith(ROMS)) "/Extra/$extraId/" + path.removePrefix(ROMS) else null

    /** [text] with the path before an optional tab rewritten on each line; the rest of the line (the recents alias) is kept. */
    fun rewriteLines(text: String, extraId: String): String = text.split("\n").joinToString("\n") { line ->
        val tab = line.indexOf('\t')
        val path = if (tab < 0) line else line.substring(0, tab)
        (toExtra(path, extraId) ?: path) + if (tab < 0) "" else line.substring(tab)
    }
}
