package com.nxredux.mobile.storage

/** Small app-private text files (Android: `.userdata/android/<name>`), never in the ROM tree. */
interface PrivateFiles {
    /**
     * The file's text, or null only when it is absent; a file that is there but can't be read throws (a caller must
     * never take an unreadable file for a missing one and save over it).
     */
    suspend fun read(name: String): String?
    /** Replaces the file atomically; throws when it cannot be written. */
    suspend fun write(name: String, text: String)
    /**
     * Adds [text] to the end of the file (created when absent); throws when it cannot. The default reads and rewrites
     * the whole file; a platform with real appends (Android) overrides it with a durable one, which a failure part-way
     * may leave with a torn last line.
     */
    suspend fun append(name: String, text: String) = write(name, (read(name) ?: "") + text)
}

/** In memory (tests, previews, a platform without storage). */
class MemoryPrivateFiles : PrivateFiles {
    val files = HashMap<String, String>()
    override suspend fun read(name: String): String? = files[name]
    override suspend fun write(name: String, text: String) { files[name] = text }
}
