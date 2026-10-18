package com.nxredux.mobile.cheats.db

/**
 * What the Cheat Database tool installed: the server's Last-Modified stamp (for update checks), when, and
 * every file written (paths relative to the sd root), so Remove deletes exactly those and nothing the user
 * added. [tags] are the table rows it was installed for (null when the file has none): when the app's table changes,
 * an update check reinstalls even though the server's archive is the same. One text file at [PATH]; nx-redux keeps the
 * same information as a manifest + `db_lastmod` pair.
 */
data class CheatDbManifest(val stamp: String, val installedAt: Long, val files: List<String>, val tags: List<String>? = null) {
    /** Files per tag (`Cheats/<TAG>/…`), in first-seen order. */
    val countsByTag: Map<String, Int> get() = files.groupingBy { tagOf(it) }.eachCount()
    val systems: Int get() = countsByTag.size
    val fileCount: Int get() = files.size
    /** nx-redux's rule: installed means the manifest exists and lists something. */
    val installed: Boolean get() = files.isNotEmpty()

    /** The on-disk text: [HEADER], the `stamp` and `installed` keys, then one file path per line. */
    fun render(): String = buildString {
        append(HEADER).append('\n')
        append("stamp = ").append(stamp).append('\n')
        append("installed = ").append(installedAt).append('\n')
        tags?.let { append("tags = ").append(it.joinToString(",")).append('\n') }
        files.forEach { append(it).append('\n') }
    }

    companion object {
        const val PATH = ".userdata/shared/xtras/cheatdb.manifest"
        const val HEADER = "# nx-mobile cheatdb 1"
        /** Stored when the server sent no Last-Modified: an update check then always downloads. */
        const val UNKNOWN_STAMP = "installed"

        /** Null when the header line is missing or names another version. Blank and `#` lines are skipped. */
        fun parse(text: String): CheatDbManifest? {
            val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
            if (lines.firstOrNull()?.trim() != HEADER) return null
            var stamp = UNKNOWN_STAMP
            var installedAt = 0L
            var tags: List<String>? = null
            val files = ArrayList<String>()
            for (raw in lines.drop(1)) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val eq = line.indexOf(" = ")
                // Key lines never contain a slash; every file path does (`Cheats/<TAG>/…`).
                if (eq > 0 && !line.contains('/')) {
                    val value = line.substring(eq + 3)
                    when (line.substring(0, eq)) {
                        "stamp" -> stamp = value
                        "installed" -> installedAt = value.toLongOrNull() ?: 0L
                        "tags" -> tags = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    continue
                }
                files += line
            }
            return CheatDbManifest(stamp, installedAt, files, tags)
        }

        private fun tagOf(path: String): String = path.split('/').getOrNull(1) ?: ""
    }
}
