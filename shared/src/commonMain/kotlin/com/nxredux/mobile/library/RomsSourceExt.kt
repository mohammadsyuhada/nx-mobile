package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef

/**
 * The folder at [segments] below [home], each segment reused when it is a folder and created when missing. Null when
 * [home] has no write grant, a file is in the way, or a create fails. Provider exceptions (a revoked or odd
 * provider) propagate: callers catch them.
 */
suspend fun RomsSource.ensurePath(home: ContentRef, segments: List<String>): ContentRef? {
    if (!canWrite(home)) return null
    var dir = home
    for (segment in segments) {
        val existing = child(dir, segment)
        dir = when {
            existing == null -> createDirectory(dir, segment)?.takeIf { it.isDirectory }?.ref ?: return null
            existing.isDirectory -> existing.ref
            else -> return null   // a file is in the way
        }
    }
    return dir
}
