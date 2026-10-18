package com.nxredux.mobile.library.disc

/** What a CHD holds, by its metadata: a GD-ROM (Dreamcast), a DVD (PSP here), a CD, or something else. */
enum class ChdMedia { GD, DVD, CD, OTHER }

/**
 * A CHD v5 header (`MComprHD`, big-endian; libchdr's chd.h) and the metadata entries chained from its `metaoffset`:
 * tag (4), flags (1), length (3), next (8), then the data. Only entries inside the bytes given are read.
 */
object ChdHeader {
    fun media(head: ByteArray): ChdMedia? {
        if (head.size < 124 || head.copyOfRange(0, 8).decodeToString() != "MComprHD" || u32(head, 12) != 5L) return null
        var at = u64(head, 48)
        var found = ChdMedia.OTHER
        val seen = HashSet<Long>()
        while (at > 0 && at + 16 <= head.size && seen.add(at)) {
            when (head.copyOfRange(at.toInt(), at.toInt() + 4).decodeToString()) {
                "CHGD", "CHGT" -> return ChdMedia.GD
                "DVD " -> return ChdMedia.DVD
                "CHT2", "CHTR", "CHCD" -> found = ChdMedia.CD
            }
            at = u64(head, at.toInt() + 8)
        }
        return found
    }

    private fun u32(b: ByteArray, at: Int): Long = (0 until 4).fold(0L) { v, i -> (v shl 8) or (b[at + i].toLong() and 0xFF) }
    private fun u64(b: ByteArray, at: Int): Long = (0 until 8).fold(0L) { v, i -> (v shl 8) or (b[at + i].toLong() and 0xFF) }
}
