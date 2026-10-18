package com.nxredux.mobile.storage

/**
 * App-private home for small text files the home tree could not take: a folder's `systems.txt` or `map.txt`
 * (keyed by source id + relative dir) and the fallback copies of `Collections/<Name>.txt`.
 */
interface SidecarStore {
    suspend fun read(sourceId: String, relativeDir: String, file: String): String?
    suspend fun write(sourceId: String, relativeDir: String, file: String, text: String): Boolean
    /** The file names stored for that folder, in insertion order. */
    suspend fun list(sourceId: String, relativeDir: String): List<String>
    /** Deletes the stored file; false when there is none or the delete failed. */
    suspend fun delete(sourceId: String, relativeDir: String, file: String): Boolean
}

class MemorySidecarStore : SidecarStore {
    val map = LinkedHashMap<String, String>()
    private fun key(sourceId: String, relativeDir: String, file: String) = "$sourceId|$relativeDir|$file"
    override suspend fun read(sourceId: String, relativeDir: String, file: String): String? = map[key(sourceId, relativeDir, file)]
    override suspend fun write(sourceId: String, relativeDir: String, file: String, text: String): Boolean { map[key(sourceId, relativeDir, file)] = text; return true }
    override suspend fun list(sourceId: String, relativeDir: String): List<String> {
        val prefix = "$sourceId|$relativeDir|"
        return map.keys.filter { it.startsWith(prefix) }.map { it.substring(prefix.length) }
    }
    override suspend fun delete(sourceId: String, relativeDir: String, file: String): Boolean = map.remove(key(sourceId, relativeDir, file)) != null
}
