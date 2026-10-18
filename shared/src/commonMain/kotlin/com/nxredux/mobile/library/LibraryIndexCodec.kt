package com.nxredux.mobile.library

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** JSON for the stored index. Anything that does not decode, or another [LibraryIndex.VERSION], is null: the caller scans. */
object LibraryIndexCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(index: LibraryIndex): String = json.encodeToString(LibraryIndex.serializer(), index)

    fun decode(text: String): LibraryIndex? =
        runCatching { json.decodeFromString(LibraryIndex.serializer(), text) }.getOrNull()
            ?.takeIf { it.version == LibraryIndex.VERSION }

    /** [encode] on [context] (`Dispatchers.Default` in the app): a whole library's JSON is too much work for the main thread. */
    suspend fun encode(index: LibraryIndex, context: CoroutineContext): String = withContext(context) { encode(index) }

    /** [decode] on [context], as [encode]. */
    suspend fun decode(text: String, context: CoroutineContext): LibraryIndex? = withContext(context) { decode(text) }
}
