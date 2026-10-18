package com.nxredux.mobile.storage

/** One top-level entry of the ROM cache: its [name], its size ([bytes], a directory's recursive sum) and its [lastUsedMillis] (its mtime, touched on every use). */
data class CacheEntry(val name: String, val bytes: Long, val lastUsedMillis: Long)

/**
 * Which ROM-cache entries (`cacheDir/roms/`: archive extractions and real-path copies) to delete. Entries unused for
 * more than the max age go first; then, while what is left is over the cap, the least recently used (ties by name, so
 * the choice is stable). An entry in `keep` (the running game's, one being unpacked) is never chosen, but its bytes still
 * count toward the cap: the space is taken either way. Pure; the platform lists, sizes and deletes.
 */
object CacheSweep {
    /**
     * 256 MiB: retro ROMs are small, so this holds hundreds of cartridge games, enough that replaying a recent game
     * never unpacks it again, and small next to what "Download all game data" can unpack (every archive in the library).
     * An entry bigger than the cap alone (a zipped PS1 or PSP image) stays while it is in use and goes on the next sweep
     * after.
     */
    const val CAP_BYTES = 256L shl 20

    /** 30 days: a game not played for a month costs one unpack when it next is; nothing stale sits in the cache for ever. */
    const val MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000

    fun victims(entries: List<CacheEntry>, now: Long, capBytes: Long = CAP_BYTES, maxAgeMillis: Long = MAX_AGE_MILLIS, keep: Set<String> = emptySet()): List<String> {
        val (stale, fresh) = entries.partition { it.name !in keep && now - it.lastUsedMillis > maxAgeMillis }
        val out = stale.sortedWith(ORDER).mapTo(ArrayList()) { it.name }
        var total = fresh.sumOf { it.bytes }
        for (e in fresh.filter { it.name !in keep }.sortedWith(ORDER)) {
            if (total <= capBytes) break
            out += e.name
            total -= e.bytes
        }
        return out
    }

    private val ORDER = compareBy<CacheEntry>({ it.lastUsedMillis }, { it.name })
}
