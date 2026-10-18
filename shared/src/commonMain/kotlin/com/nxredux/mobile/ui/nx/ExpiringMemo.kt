package com.nxredux.mobile.ui.nx

/**
 * A value per key, loaded on first use and kept for [ttlMs] ([now] is the clock, ms), then loaded again: for a lookup
 * too slow to repeat per frame whose answer rarely changes (an installed app's version). A null value is kept too. Not
 * thread safe; the caller locks.
 */
internal class ExpiringMemo<K, V>(private val ttlMs: Long, private val now: () -> Long) {
    private class Entry<V>(val value: V, val at: Long)

    private val entries = HashMap<K, Entry<V>>()

    /** [key]'s value: the kept one while it is younger than [ttlMs], else [load]'s, kept from now. */
    fun get(key: K, load: (K) -> V): V {
        val t = now()
        entries[key]?.let { if (t - it.at in 0 until ttlMs) return it.value }
        return load(key).also { entries[key] = Entry(it, t) }
    }
}
