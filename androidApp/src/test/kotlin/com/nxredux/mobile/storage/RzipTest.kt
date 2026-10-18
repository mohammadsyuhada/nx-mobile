package com.nxredux.mobile.storage

import java.io.File
import org.junit.Test
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue

class RzipTest {
    private fun res(name: String) = requireNotNull(javaClass.classLoader!!.getResource("rzip/$name")) { name }.readBytes()

    @Test fun decodes_files_written_by_libretro_common() {
        assertArrayEquals(res("small.raw"), Rzip.decode(res("small.rzip")))
        assertArrayEquals(res("big.raw"), Rzip.decode(res("big.rzip")))   // several chunks
    }
    @Test fun encode_round_trips_and_writes_the_libretro_header() {
        val raw = res("big.raw")
        val enc = Rzip.encode(raw)
        assertTrue(isRzipCompressed(enc))
        assertArrayEquals(byteArrayOf(1, '#'.code.toByte()), enc.copyOfRange(6, 8))
        assertArrayEquals(byteArrayOf(0, 0, 2, 0), enc.copyOfRange(8, 12))   // chunk size 131072, little endian
        assertArrayEquals(raw, Rzip.decode(enc))
    }
    @Test fun libretro_common_reads_what_we_write() {
        val tool = File("/tmp/rzip_tool")
        assumeTrue("the C tool is built by hand (tools/rzip-fixture/README.md)", tool.canExecute())   // skipped without it
        val raw = res("big.raw")
        val inFile = File.createTempFile("rzip", ".rzip").apply { writeBytes(Rzip.encode(raw)); deleteOnExit() }
        val outFile = File.createTempFile("rzip", ".raw").apply { deleteOnExit() }
        val exit = ProcessBuilder(tool.path, "decompress", inFile.path, outFile.path).inheritIO().start().waitFor()
        assertEquals(0, exit)
        assertArrayEquals(raw, outFile.readBytes())
    }
    @Test fun raw_bytes_pass_through() {
        val raw = byteArrayOf(1, 2, 3, 4)
        assertArrayEquals(raw, Rzip.decode(raw))
        assertArrayEquals(ByteArray(0), Rzip.decode(ByteArray(0)))
    }
    /**
     * libretro-common rzipstream_read_file_header (rzip_stream.c:99-116): unless all 8 magic bytes (`#RZIPv`, version 1,
     * `#`) match and the file has the 20 header bytes, it rewinds and reads the file raw. So does decode.
     */
    @Test fun a_header_libretro_does_not_take_for_rzip_is_read_raw() {
        val enc = Rzip.encode(res("small.raw"))
        val otherVersion = enc.copyOf().also { it[6] = 2 }
        assertArrayEquals(otherVersion, Rzip.decode(otherVersion))
        val noHash = enc.copyOf().also { it[7] = 'x'.code.toByte() }
        assertArrayEquals(noHash, Rzip.decode(noHash))
        val short = enc.copyOf(19)   // all 8 magic bytes, but shorter than the header
        assertArrayEquals(short, Rzip.decode(short))
    }
    @Test fun malformed_rzip_is_null() {
        val enc = Rzip.encode(res("small.raw"))
        assertNull(Rzip.decode(enc.copyOf(enc.size - 10)))                    // truncated
        assertNull(Rzip.decode(enc.copyOf(20)))                               // header only
        assertNull(Rzip.decode(enc.copyOf().also { it[8] = 0; it[9] = 0; it[10] = 0; it[11] = 0 }))   // chunk size 0
    }
    @Test fun a_header_claiming_huge_sizes_is_null_not_a_crash() {
        val header = Rzip.encode(res("small.raw")).copyOf(20)
        val hugeTotal = header.copyOf().also { for (i in 12..18) it[i] = 0; it[12] = -1; it[13] = -1; it[14] = -1; it[15] = 0x7F }
        assertNull(Rzip.decode(hugeTotal))
        val hugeChunk = header.copyOf().also { it[8] = -1; it[9] = -1; it[10] = -1; it[11] = 0x7F }
        assertNull(Rzip.decode(hugeChunk))
    }
}
