package com.nxredux.mobile.storage

/** Where the library index JSON lives between runs. A cache: losing it only costs a scan. */
interface LibraryIndexStore {
    suspend fun read(): String?
    /** False when the text could not be stored. */
    suspend fun write(text: String): Boolean
    suspend fun delete()
}

/** In-memory store (tests, previews). [failWrites] makes every write fail, as a full disk would. */
class MemoryLibraryIndexStore(var text: String? = null) : LibraryIndexStore {
    var failWrites = false
    var writes = 0
        private set
    var deletes = 0
        private set

    override suspend fun read(): String? = text

    override suspend fun write(text: String): Boolean {
        writes++
        if (failWrites) return false
        this.text = text
        return true
    }

    override suspend fun delete() {
        deletes++
        text = null
    }
}
