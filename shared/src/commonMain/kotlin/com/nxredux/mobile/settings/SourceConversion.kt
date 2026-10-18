package com.nxredux.mobile.settings

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.FileRefs

/**
 * With All files access on, the home and each extra that is an external-storage SAF tree whose path
 * ([Sources.mappedPath]) is a folder become that path's file ref. Extras keep their id (hidden, renamed and collections
 * are keyed by it); refs without a mapped path, or whose path is not a folder, stay SAF. A runtime convenience, not a
 * migration: the caller keeps the SAF grants.
 */
object SourceConversion {
    /** The home and extras after conversion (same order, same ids). */
    data class Converted(val home: ContentRef?, val extras: List<ExtraFolder>)

    /** Converts [home] and [extras]; [isDirectory] says whether a mapped path is a folder now. */
    suspend fun convert(home: ContentRef?, extras: List<ExtraFolder>, isDirectory: suspend (String) -> Boolean): Converted {
        suspend fun fileRefOf(ref: ContentRef): ContentRef {
            if (FileRefs.isFile(ref)) return ref
            val path = Sources.mappedPath(ref) ?: return ref
            return if (isDirectory(path)) FileRefs.of(path) else ref
        }
        return Converted(home?.let { fileRefOf(it) }, extras.map { it.copy(ref = fileRefOf(it.ref)) })
    }
}
