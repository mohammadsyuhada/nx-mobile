package com.nxredux.mobile.achievements

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A fingerprint entry [from] (the sender's `.sources/<id>/…` key) that lands as [to] in the receiver's console folder. */
data class Rekey(val from: String, val to: String)

/** The texts to write: [own] this phone's file, [peer] the file to push; null = leave that side's file as it is. */
data class MergedHashes(val own: String?, val peer: String?)

/** One console's `hashes.json` across a Device Sync. */
object RaFingerprintMerge {
    private const val SOURCES = ".sources/"

    /**
     * own = merge(own, peer without `.sources/` keys) + the peer's [peerSent] entries re-keyed;
     * peer = merge(peer, own without `.sources/` keys) + our [ownSent] entries re-keyed. In a merge the larger `mtime`
     * wins, the base's entry on a tie. A side is written only when its result differs from what it holds (a missing
     * file that stays empty is not written).
     */
    fun plan(own: String?, peer: String?, ownSent: List<Rekey>, peerSent: List<Rekey>): MergedHashes {
        val o = RaFingerprintFile.parse(own); val p = RaFingerprintFile.parse(peer)
        val newOwn = build(base = own, other = p, sent = peerSent, sender = p)
        val newPeer = build(base = peer, other = o, sent = ownSent, sender = o)
        return MergedHashes(newOwn, newPeer)
    }

    private fun build(base: String?, other: RaFingerprintFile, sent: List<Rekey>, sender: RaFingerprintFile): String? {
        val out = RaFingerprintFile.parse(base)
        for (k in other.keys()) if (!k.startsWith(SOURCES)) offer(out, k, other.entry(k)!!)
        for (r in sent) sender.entry(r.from)?.let { offer(out, r.to, it) }
        return if (out.changed) out.encode() else null
    }

    private fun offer(into: RaFingerprintFile, key: String, value: JsonObject) {
        val mine = into.entry(key)
        if (mine == null || mtime(value) > mtime(mine)) into.put(key, value)
    }

    private fun mtime(e: JsonObject): Long = (e["mtime"] as? JsonPrimitive)?.longOrNull ?: Long.MIN_VALUE
}
