package com.nxredux.mobile.scraper

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidRomHasherTest {
    private fun zip(vararg files: Pair<String, ByteArray>, stored: Boolean = false): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, bytes) in files) {
                val e = ZipEntry(name)
                if (stored) { e.method = ZipEntry.STORED; e.size = bytes.size.toLong(); e.crc = crcOf(bytes) }
                z.putNextEntry(e); z.write(bytes); z.closeEntry()
            }
        }
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun crcOf(b: ByteArray) = CRC32().apply { update(b) }.value

    private val rom = ByteArray(32_768) { (it * 7).toByte() }

    @Test fun theRomInsideIsHashed() {
        assertEquals(crcOf(rom) to rom.size.toLong(), ZipRomCrc.read(zip("Tetris.gb" to rom), limit = 1_000_000))
        assertEquals(crcOf(rom) to rom.size.toLong(), ZipRomCrc.read(zip("Tetris.gb" to rom, stored = true), limit = 1_000_000))
    }

    // A readme or a folder beside the ROM: the largest file is the ROM.
    @Test fun theLargestFileIsTheRom() {
        val z = zip("readme.txt" to "hi".toByteArray(), "game/" to ByteArray(0), "game/zz2.gb" to rom, "__MACOSX/._zz2.gb" to ByteArray(120))
        assertEquals(crcOf(rom) to rom.size.toLong(), ZipRomCrc.read(z, limit = 1_000_000))
    }

    @Test fun anEmptyOrOversizedOrBrokenZipIsNull() {
        assertNull(ZipRomCrc.read(zip(), limit = 1_000_000))
        assertNull(ZipRomCrc.read(zip("Tetris.gb" to rom), limit = 32_767))   // unpacked past the limit
        assertNull(ZipRomCrc.read(ByteArrayInputStream(rom), limit = 1_000_000))   // not a zip
    }
}
