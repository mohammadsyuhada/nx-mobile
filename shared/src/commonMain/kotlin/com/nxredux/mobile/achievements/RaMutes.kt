package com.nxredux.mobile.achievements

/** Per-game muted achievement ids (`muted/<md5>.txt`, one id per line). */
class RaMutes(private val store: RaStore) {
    suspend fun load(hash: String): Set<Int> = store.read(RaPaths.muted(hash))?.decodeToString()?.lineSequence()?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    suspend fun toggle(hash: String, id: Int): Set<Int> {
        val next = load(hash).let { if (id in it) it - id else it + id }
        store.writeAtomic(RaPaths.muted(hash), next.joinToString("") { "$it\n" }.encodeToByteArray())
        return next
    }
}
