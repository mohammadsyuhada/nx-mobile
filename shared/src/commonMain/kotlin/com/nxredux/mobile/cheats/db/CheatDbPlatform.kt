package com.nxredux.mobile.cheats.db

/** The downloaded archive in the platform's cache; [stamp] is the response's Last-Modified header. */
class CheatDbArchive(val path: String, val stamp: String?)

/** A failure the UI has words for. Anything else is reported by exception class name. */
class CheatDbException(val kind: Kind, cause: Throwable? = null) : Exception(kind.message, cause) {
    enum class Kind(val message: String) {
        NETWORK("Download failed (check Wi-Fi)"),
        CORRUPT("Downloaded archive is corrupt"),
        NO_SPACE("Not enough space (needs about 400 MB free)"),
        STORAGE("Could not write the cheat files"),
    }
}

/**
 * What the Cheat Database tool needs from the platform: network, the zip, and the sd root. Every suspend
 * function is cancellable and cleans up its own partial output on cancellation; [download] and [install]
 * throw [CheatDbException] for the failures the UI names. Implemented on Android with HttpURLConnection and
 * java.util.zip; faked in tests.
 */
interface CheatDbPlatform {
    /** Free bytes on the sd root's volume. */
    fun freeBytes(): Long
    /** The server's Last-Modified for the archive (HEAD), or null when it can't be reached. */
    suspend fun remoteStamp(): String?
    /** Downloads the archive to the cache. [onProgress] gets (bytes so far, total or -1 when unknown). */
    suspend fun download(onProgress: (done: Long, total: Long) -> Unit): CheatDbArchive
    /**
     * Extracts every `.cht` under each tag's libretro [folders] into `Cheats/<TAG>/`, in map order, calling
     * [onProgress] (tag, 1-based index, count) per tag. When two of a tag's folders hold the same file name, the
     * first folder's file is kept. Returns the written paths relative to the sd root.
     * On failure or cancellation, files this call created are deleted again; files that existed before are
     * left as they are.
     */
    suspend fun install(archive: CheatDbArchive, folders: Map<String, List<String>>, onProgress: (tag: String, index: Int, count: Int) -> Unit): List<String>
    /** Deletes the archive (and any partial download). */
    suspend fun discard(archive: CheatDbArchive)
    /** The manifest text at [CheatDbManifest.PATH], or null when absent. */
    suspend fun readManifest(): String?
    suspend fun writeManifest(text: String)
    /** Deletes [files] (relative paths), prunes emptied `Cheats/<TAG>/` folders, and removes the manifest. */
    suspend fun deleteInstalled(files: List<String>)
    /** Deletes [files] (relative paths) and prunes emptied `Cheats/<TAG>/` folders; the manifest stays. Best effort. */
    suspend fun deleteFiles(files: List<String>)
}
