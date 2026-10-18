package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef

/**
 * Art layout inside a folder's `.media`, each `.png`, else `.jpg`:
 *   - 3D box art:      .media/boxart/<name>
 *   - 2D front cover:  .media/boxart2d/<name> (NX Redux's folder), else the legacy .media/box2d/<name>
 *   - screenshot:      .media/screenshot/<name>
 *   - SteamGridDB:     .media/grid/<name> (2:3 portrait), .media/hero/<name> (wide banner)
 *   - wheel (logo):    .media/wheel/<name>  (known, not shown)
 *   - mix:             .media/mix/<name>    (NX Redux's mix; known, not shown)
 *   - folder bg:       .media/bg.png   (key "bg" in the top-level map)
 * NX Redux's old untyped mix `.media/<name>.png` is not read for games; the top-level map still gives a system its
 * `Roms/.media/<System folder>.png` and a folder its bg. Folders (systems) use their full name; files use the name
 * without extension.
 */
class ArtworkResolver(private val source: RomsSource) {
    companion object {
        /** The typed `.media` subfolders a game's art is read from. */
        val TYPED = listOf("boxart", "boxart2d", "box2d", "screenshot", "grid", "hero", "wheel", "mix")
        private val JPEG = listOf(".jpg", ".jpeg")
    }

    /** Children of a folder's `.media` (listed through [list]: a scan passes its shared listings), or null if there is none. */
    suspend fun mediaListing(children: List<Entry>, list: suspend (ContentRef) -> List<Entry> = { source.listChildren(it) }): List<Entry>? {
        val media = children.firstOrNull { it.isDirectory && it.name == ".media" } ?: return null
        return list(media.ref)
    }

    /** baseName -> ref for every .png directly in a listing. */
    fun pngMap(entries: List<Entry>): Map<String, ContentRef> =
        entries.filter { !it.isDirectory && it.name.endsWith(".png", ignoreCase = true) }
            .associate { it.name.dropLast(4) to it.ref }

    /**
     * baseName -> ref for the files in `.media/<sub>` (one of [TYPED]): `.png`, else `.jpg`, else `.jpeg` (the extension
     * in any case), whatever the listing order; the scrape engine reads a slot the same way.
     */
    suspend fun subArtMap(mediaEntries: List<Entry>, sub: String): Map<String, ContentRef> {
        val dir = mediaEntries.firstOrNull { it.isDirectory && it.name == sub } ?: return emptyMap()
        val files = source.listChildren(dir.ref).filter { !it.isDirectory }
        fun withExt(ext: String) = files.filter { it.name.endsWith(ext, ignoreCase = true) && it.name.length > ext.length }
            .associate { it.name.dropLast(ext.length) to it.ref }
        // Lowest priority first: each later map overrides the names it shares.
        return JPEG.reversed().fold(emptyMap<String, ContentRef>()) { m, ext -> m + withExt(ext) } + pngMap(files)
    }

    /** Top-level `.media` map from a folder's already-listed children. */
    suspend fun mediaMap(children: List<Entry>, list: suspend (ContentRef) -> List<Entry> = { source.listChildren(it) }): Map<String, ContentRef> =
        pngMap(mediaListing(children, list) ?: emptyList())

    fun artFor(map: Map<String, ContentRef>, entryName: String, isDirectory: Boolean): ContentRef? =
        map[if (isDirectory) entryName else Names.stripExtension(entryName)]
}
