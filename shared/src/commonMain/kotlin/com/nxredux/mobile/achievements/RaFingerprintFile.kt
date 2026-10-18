package com.nxredux.mobile.achievements

import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A remembered fingerprint: the md5 (lowercase) and the RA game id (0 = the server does not know the hash). */
data class RaFingerprint(val hash: String, val game: Int)

/**
 * One console folder's `hashes.json` (nx-redux `ra_hash_cache.h`), kept as JSON so unknown keys survive, at the top level
 * and in each entry. Keys are paths below the console folder (mobile's extras: `.sources/<id>/…`).
 */
class RaFingerprintFile private constructor(private var top: Map<String, JsonElement>, private val files: LinkedHashMap<String, JsonObject>) {
    var changed = false; private set

    fun lookup(key: String, size: Long, mtime: Long, console: Int): RaFingerprint? {
        val e = files[key] ?: return null
        if (e.long("size") != size || e.int("console") != console) return null
        if (!mtimeMatches(e.long("mtime") ?: return null, mtime)) return null
        val hash = e.str("hash")?.lowercase()?.takeIf { RaPaths.isHash(it) } ?: return null
        return RaFingerprint(hash, e.int("game") ?: 0)
    }

    /** Stores an answer; an equivalent entry (same size, console, hash, game, mtime within tolerance) is left alone. */
    fun record(key: String, size: Long, mtime: Long, console: Int, hash: String, game: Int) {
        val old = files[key]
        if (old != null && old.long("size") == size && old.int("console") == console && old.str("hash")?.lowercase() == hash.lowercase() &&
            (old.int("game") ?: 0) == game && old.long("mtime")?.let { mtimeMatches(it, mtime) } == true) return
        val fields = LinkedHashMap(old.orEmpty())
        fields["size"] = JsonPrimitive(size); fields["mtime"] = JsonPrimitive(mtime); fields["console"] = JsonPrimitive(console)
        fields["hash"] = JsonPrimitive(hash.lowercase()); fields["game"] = JsonPrimitive(game)
        files[key] = JsonObject(fields); changed = true
    }

    fun forget(key: String) { if (files.remove(key) != null) changed = true }

    /** [forget], only while the entry still holds [hash] (case-insensitive): an entry re-recorded since is kept. */
    fun forget(key: String, hash: String) { if (files[key]?.str("hash")?.lowercase() == hash.lowercase()) forget(key) }

    /** Every entry's key, in file order. */
    fun keys(): Set<String> = files.keys.toSet()
    /** [key]'s raw entry (unknown fields kept), or null. */
    fun entry(key: String): JsonObject? = files[key]
    /** Stores [value] as [key]'s entry as is (the Device Sync merge); marks the file changed when it differs. */
    fun put(key: String, value: JsonObject) { if (files[key] != value) { files[key] = value; changed = true } }

    fun encode(): String {
        val out = LinkedHashMap(top)
        if (out["version"] == null) out["version"] = JsonPrimitive(1)
        out["files"] = JsonObject(files)
        return Json.encodeToString(JsonObject.serializer(), JsonObject(out))
    }

    private fun JsonObject.prim(k: String) = this[k] as? JsonPrimitive
    private fun JsonObject.long(k: String) = prim(k)?.longOrNull
    private fun JsonObject.int(k: String) = prim(k)?.intOrNull
    private fun JsonObject.str(k: String) = prim(k)?.takeIf { it.isString }?.content

    companion object {
        /** An absent (null) or wrong-shaped file is empty (and replaced on the next write). */
        fun parse(text: String?): RaFingerprintFile {
            val root = text?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject
            val files = LinkedHashMap<String, JsonObject>()
            (root?.get("files") as? JsonObject)?.forEach { (k, v) -> (v as? JsonObject)?.let { files[k] = it } }
            return RaFingerprintFile(root?.filterKeys { it != "files" }.orEmpty(), files)
        }

        /** Equal, or off by whole 15-minute steps up to 14 h (FAT stores local time; the two apps may read it in different zones). */
        fun mtimeMatches(cached: Long, actual: Long): Boolean { val d = abs(cached - actual); return d == 0L || (d % 900L == 0L && d <= 50_400L) }
    }
}
