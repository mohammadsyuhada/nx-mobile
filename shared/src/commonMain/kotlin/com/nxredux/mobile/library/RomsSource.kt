package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef

data class Entry(
    val ref: ContentRef,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?,
    /** Provider last-modified in epoch millis; null when the provider does not report one. */
    val lastModified: Long? = null,
)

/**
 * The platform answered a listing of [dir] with nothing although the folder is provably not empty (Android's
 * external-storage provider hides every folder while its FUSE root reads as missing). Never a lost tree or grant:
 * callers keep what they had, or look names up with [RomsSource.child]. A platform that answers `child` by a direct
 * lookup (SAF) still answers it; the default `child` lists, so it throws this too.
 */
class ListingUnavailable(val dir: String) : Exception("The storage provider did not list $dir")

/**
 * One level of a document tree. Platform implementations wrap SAF (Android) or the file system.
 * Writes default to "unsupported" so read-only sources need nothing; writable ones override them.
 */
interface RomsSource {
    /** [dir]'s children. Throws [ListingUnavailable] when the platform's empty answer is provably wrong. */
    suspend fun listChildren(dir: ContentRef): List<Entry>
    suspend fun readText(file: ContentRef): String?

    /**
     * The direct child [name] of [dir], or null if there is none. The default lists [dir]; platforms
     * that can address a child by name override it so a single lookup never lists a whole folder.
     */
    suspend fun child(dir: ContentRef, name: String): Entry? = listChildren(dir).firstOrNull { it.name == name }

    /** The first [max] bytes of [file] (fewer if the file is shorter), or null when it cannot be read. */
    suspend fun readHead(file: ContentRef, max: Int): ByteArray? = null

    /** Up to [length] bytes of [file] from [offset] (fewer at its end), or null when it cannot be read: a disc probe's seek. */
    suspend fun readRange(file: ContentRef, offset: Long, length: Int): ByteArray? = null

    /** The folder name the user sees for a tree root (the last path segment). */
    fun displayName(tree: ContentRef): String =
        tree.value.trimEnd('/').substringAfterLast('/').substringAfterLast(':').ifEmpty { tree.value }

    /**
     * True when [tree] sits on storage that is not there right now (an unmounted SD card), as opposed to a folder that
     * was deleted or a grant that was revoked: a failed listing of an offline tree keeps its settings and grant. Cheap
     * (no tree query) and never throws: false when the platform cannot tell.
     */
    suspend fun isOffline(tree: ContentRef): Boolean = false

    /** True when files can be created in [tree] (a persisted write grant on Android). */
    suspend fun canWrite(tree: ContentRef): Boolean = false

    /** Creates (or returns the existing) directory [name] under [parent]; null when unsupported or failed. */
    suspend fun createDirectory(parent: ContentRef, name: String): Entry? = null

    /**
     * Creates or replaces the text file [name] in [dir], through `<name>.part` as [writeBytes] does where the platform can
     * (a failed write leaves the old [name] whole). Where it cannot swap files, [name] is written in place. False when
     * unsupported or failed.
     */
    suspend fun writeText(dir: ContentRef, name: String, text: String): Boolean = false

    /**
     * Creates or replaces the file [name] in [dir] with [bytes], atomically: the bytes go to `<name>.part`, then an existing
     * [name] is deleted and the part renamed to [name]. A failed rename deletes the part, so no partial [name] is left.
     * Not atomic where the platform has no replacing rename (SAF): false may mean the old [name] is already gone.
     * False when unsupported or failed.
     */
    suspend fun writeBytes(dir: ContentRef, name: String, bytes: ByteArray): Boolean = false

    /** Deletes the file [name] in [dir]. False when there is none, or when unsupported or failed. */
    suspend fun delete(dir: ContentRef, name: String): Boolean = false

    /**
     * Deletes the folder [name] in [parent] only when it holds nothing (never its contents). False when there is no such
     * folder, it is not empty, or the delete is unsupported or failed.
     */
    suspend fun deleteEmptyDirectory(parent: ContentRef, name: String): Boolean = false

    /**
     * Renames the file [from] in [dir] to [to] in one step. False when there is no such file, [to] is taken, the
     * platform would give it another name, or the rename is unsupported or failed; then [from] is left as it was.
     */
    suspend fun rename(dir: ContentRef, from: String, to: String): Boolean = false

    /**
     * Moves the file or folder [name] from [fromDir] into [toDir] without copying it (a rename, or the provider's own
     * move). False when the platform cannot, or [toDir] already holds [name].
     */
    suspend fun moveFile(fromDir: ContentRef, name: String, toDir: ContentRef): Boolean = false

    /**
     * Copies the file or folder [name] from [fromDir] into [toDir], whole: false on any failure (or [name] already in
     * [toDir]), and nothing is left behind in [toDir] then.
     */
    suspend fun copyFile(fromDir: ContentRef, name: String, toDir: ContentRef): Boolean = false
}
