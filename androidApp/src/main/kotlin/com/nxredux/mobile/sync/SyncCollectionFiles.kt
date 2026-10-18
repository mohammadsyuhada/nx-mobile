package com.nxredux.mobile.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * Collections sync's file side: the peer's `collections` module pulled into a fresh cache folder and read, and the
 * changed files written into another fresh cache folder and pushed. Each cache is removed again on every path.
 * [run] starts rsync with the given argv, each output line to its callback; the exit code.
 */
object SyncCollectionFiles {
    suspend fun pull(run: suspend (List<String>, (String) -> Unit) -> Int, binary: String, peerIp: String, cache: File): CollectionsPull {
        try {
            withContext(Dispatchers.IO) { cache.deleteRecursively(); cache.mkdirs() }
            val lines = ArrayList<String>()
            val code = run(SyncCommands.collectionsArgs(binary, peerIp, cache.path, push = false)) { lines += it }
            if (code != 0) return CollectionsPull(code, lines)
            val files = LinkedHashMap<String, String>()
            val unreadable = LinkedHashSet<String>()
            withContext(Dispatchers.IO) {
                for (f in cache.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".txt", ignoreCase = true) }) {
                    // Strict UTF-8: a lenient decode would put U+FFFD in the lines, and the merge would spread them back.
                    val text = try { strictUtf8(f.readBytes()) } catch (e: CharacterCodingException) { null }
                    if (text == null) unreadable += f.name else files[f.name] = text
                }
            }
            return CollectionsPull(0, lines, files, unreadable)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { cache.deleteRecursively() }
        }
    }

    private fun strictUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()

    /** [files]: file name (a plain name) → text. */
    suspend fun push(
        run: suspend (List<String>, (String) -> Unit) -> Int, binary: String, peerIp: String, cache: File, files: Map<String, String>,
    ): Pair<Int, List<String>> {
        try {
            withContext(Dispatchers.IO) {
                cache.deleteRecursively(); cache.mkdirs()
                for ((name, text) in files) {
                    require(name.isNotEmpty() && name != "." && name != ".." && name.none { it == '/' || it == '\\' || it == '\n' || it == '\r' }) { "not a plain name: $name" }
                    File(cache, name).writeText(text)
                }
            }
            val lines = ArrayList<String>()
            val code = run(SyncCommands.collectionsArgs(binary, peerIp, cache.path, push = true)) { lines += it }
            return code to lines
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { cache.deleteRecursively() }
        }
    }
}
