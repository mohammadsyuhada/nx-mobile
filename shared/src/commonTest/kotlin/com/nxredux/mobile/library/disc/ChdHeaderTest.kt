package com.nxredux.mobile.library.disc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChdHeaderTest {
    private fun chd(vararg meta: String): ByteArray {
        val out = ByteArray(124 + meta.size * 32)
        "MComprHD".encodeToByteArray().copyInto(out, 0)
        out[15] = 5                                                   // version 5, big-endian u32 at 12
        out[55] = 124                                                 // metaoffset = 124, big-endian u64 at 48
        meta.forEachIndexed { i, tag ->
            val at = 124 + i * 32
            tag.encodeToByteArray().copyInto(out, at)
            out[at + 7] = 8                                           // length 8
            if (i < meta.lastIndex) out[at + 15] = (at + 32).toByte() // next
        }
        return out
    }

    internal fun chdBytes(vararg meta: String) = chd(*meta)

    @Test fun media_from_the_metadata() {
        assertEquals(ChdMedia.GD, ChdHeader.media(chd("CHGD")))
        assertEquals(ChdMedia.DVD, ChdHeader.media(chd("DVD ")))
        assertEquals(ChdMedia.CD, ChdHeader.media(chd("CHT2", "CHT2")))
        assertNull(ChdHeader.media(ByteArray(124)))                    // not a CHD
    }
}
