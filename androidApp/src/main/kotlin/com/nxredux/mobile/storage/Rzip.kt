package com.nxredux.mobile.storage

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * RetroArch/minarch RZIP (libretro-common rzip_stream.c), the handheld's "(compressed)" save and state formats: a
 * 20-byte header (`#RZIPv`, version 1, `#`, chunk size u32 LE, total size u64 LE), then per chunk its compressed size
 * (u32 LE) and a zlib stream of at most chunk-size bytes. Each chunk carries the zlib header (trans_stream_zlib.c uses
 * deflateInit / inflateInit2 with default window bits, not raw deflate), so the plain [Deflater] and [Inflater] apply.
 */
object Rzip {
    private const val HEADER_SIZE = 20
    private const val VERSION: Byte = 1
    const val CHUNK_SIZE = 131072
    private const val LEVEL = 6
    /** libretro-common always writes 128 KiB chunks; refusing far larger ones keeps a corrupt header from forcing a huge buffer. */
    private const val MAX_CHUNK_SIZE = 16L shl 20
    /** Output starts small and grows, so a header's claimed total never decides the allocation. */
    private const val MAX_INITIAL_CAPACITY = 4L shl 20

    /**
     * [bytes] decoded; unchanged when they are not RZIP (as rzipstream reads a raw file); null when RZIP but malformed.
     * "RZIP" is libretro-common's test (rzipstream_read_file_header): all 8 magic bytes, version included, and the full
     * 20-byte header; a `#RZIPv` file with another version, a different 8th byte, or a short header is read raw there too.
     */
    fun decode(bytes: ByteArray): ByteArray? {
        if (!isHeader(bytes)) return bytes
        val chunk = u32(bytes, 8)
        val total = u64(bytes, 12)
        if (chunk <= 0L || chunk > MAX_CHUNK_SIZE || total <= 0L || total > Int.MAX_VALUE) return null
        val out = ByteArrayOutputStream(minOf(total, bytes.size * 8L, MAX_INITIAL_CAPACITY).toInt())
        var at = HEADER_SIZE
        val buf = ByteArray(chunk.toInt())
        try {
            while (out.size() < total) {
                if (at + 4 > bytes.size) return null
                val size = u32(bytes, at).toInt(); at += 4
                if (size <= 0 || at + size > bytes.size) return null
                val inflater = Inflater()
                try {
                    inflater.setInput(bytes, at, size)
                    var n = 0
                    while (!inflater.finished()) {
                        val r = inflater.inflate(buf, n, buf.size - n)
                        if (r == 0 && (inflater.needsInput() || inflater.needsDictionary() || n == buf.size)) return null
                        n += r
                    }
                    out.write(buf, 0, n)
                } finally { inflater.end() }
                at += size
            }
        } catch (e: DataFormatException) { return null }
        catch (e: OutOfMemoryError) { return null }
        return if (out.size().toLong() == total) out.toByteArray() else null
    }

    /**
     * The decoded size an RZIP file's first 20 bytes ([header]) claim; null when [decode] reads the file raw. Lets a
     * caller rule out a match without reading a whole file.
     */
    fun rawSize(header: ByteArray): Long? = if (isHeader(header)) u64(header, 12) else null

    /** libretro-common's RZIP test, as [decode] describes it. */
    private fun isHeader(b: ByteArray): Boolean =
        isRzipCompressed(b) && b.size >= HEADER_SIZE && b[6] == VERSION && b[7] == '#'.code.toByte()

    /** [raw] as an RZIP file, as libretro-common writes it (chunk size [CHUNK_SIZE], zlib level 6). */
    fun encode(raw: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(raw.size / 2 + HEADER_SIZE)
        out.write("#RZIPv".encodeToByteArray()); out.write(VERSION.toInt()); out.write('#'.code)
        putLe(out, CHUNK_SIZE.toLong(), 4); putLe(out, raw.size.toLong(), 8)
        val deflated = ByteArray(CHUNK_SIZE + CHUNK_SIZE / 4 + 64)
        var at = 0
        while (at < raw.size) {
            val len = minOf(CHUNK_SIZE, raw.size - at)
            val deflater = Deflater(LEVEL)
            try {
                deflater.setInput(raw, at, len); deflater.finish()
                val n = deflater.deflate(deflated)
                check(deflater.finished()) { "chunk did not fit" }
                putLe(out, n.toLong(), 4); out.write(deflated, 0, n)
            } finally { deflater.end() }
            at += len
        }
        return out.toByteArray()
    }

    private fun u32(b: ByteArray, at: Int): Long = (0 until 4).fold(0L) { acc, i -> acc or ((b[at + i].toLong() and 0xFF) shl (8 * i)) }
    private fun u64(b: ByteArray, at: Int): Long = (0 until 8).fold(0L) { acc, i -> acc or ((b[at + i].toLong() and 0xFF) shl (8 * i)) }
    private fun putLe(out: ByteArrayOutputStream, v: Long, n: Int) { for (i in 0 until n) out.write(((v shr (8 * i)) and 0xFF).toInt()) }
}
