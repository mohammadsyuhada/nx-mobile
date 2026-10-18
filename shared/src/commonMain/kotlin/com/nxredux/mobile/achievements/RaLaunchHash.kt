package com.nxredux.mobile.achievements

/** What a launch hashes: the library entry, its file stamp ("size:millis"), the RC console and whether the hash may be stored. */
data class RaLaunchTarget(val pointer: RaGamePointer, val stamp: String?, val console: Int, val storable: Boolean)

/** The launch's place in `hashes.json` and the entry it found ([cached], null on a miss); answered once rc_client has loaded. */
class RaLaunchFingerprint(val key: RaFingerprintKey, val size: Long, val mtime: Long, val console: Int, val storable: Boolean, val cached: RaFingerprint?)

/**
 * A game's hash at launch through the shared fingerprints: hashed once while its file is unchanged (a disc takes tens of
 * seconds). Nothing is written until rc_client answers ([answer]): the game id, or 0 when RA does not know the hash.
 */
object RaLaunchHash {
    /**
     * [target]'s hash: the entry in its folder's `hashes.json` when the file's stamp still matches, else [hash]'s. The
     * fingerprint is null (nothing will be stored) without a target, a key or a stamp.
     */
    suspend fun of(fp: RaFingerprints, target: RaLaunchTarget?, hash: suspend () -> String?): Pair<String?, RaLaunchFingerprint?> {
        val key = target?.let { fp.keyFor(it.pointer) }
        val stamp = target?.let { RaStamp.parse(it.stamp) }
        if (target == null || key == null || stamp == null) return hash() to null
        val cached = fp.snapshot(key.consoleFolder).lookup(key.key, stamp.first, stamp.second, target.console)
        val l = RaLaunchFingerprint(key, stamp.first, stamp.second, target.console, target.storable, cached)
        return (cached?.hash ?: hash()) to l
    }

    /** rc_client's answer for [hash]: [game] = its id, 0 = unknown. A known game turned unknown is forgotten (rehashed next launch). */
    suspend fun answer(fp: RaFingerprints, l: RaLaunchFingerprint, hash: String, game: Int) {
        if (!l.storable) return
        fp.update(l.key.consoleFolder) { f ->
            // forget(key, hash): a prefetch that rewrote the entry meanwhile (another hash) keeps it.
            if (l.cached != null && l.cached.game != 0 && game == 0) f.forget(l.key.key, hash)
            else f.record(l.key.key, l.size, l.mtime, l.console, hash, game)
        }
    }
}
