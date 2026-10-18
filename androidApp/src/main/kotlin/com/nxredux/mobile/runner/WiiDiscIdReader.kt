package com.nxredux.mobile.runner

import com.nxredux.mobile.storage.AndroidRefs
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.Inflater

/** Opens a Wii image for [WiiDiscId]: positional reads on a file channel, zlib for GCZ blocks. Blocking I/O; null on any failure. */
object WiiDiscIdReader {
    fun read(refs: AndroidRefs, ref: ContentRef, fileName: String): WiiDisc? = runCatching {
        refs.openFd(ref, "r")?.use { pfd -> FileInputStream(pfd.fileDescriptor).channel.use { read(it, fileName) } }
    }.getOrNull()

    fun read(file: File): WiiDisc? = runCatching { FileInputStream(file).channel.use { read(it, file.name) } }.getOrNull()

    private fun read(ch: FileChannel, fileName: String): WiiDisc? =
        WiiDiscId.read(fileName.substringAfterLast('.', ""), { off, len -> readAt(ch, off, len) }, ::inflate)

    private fun readAt(ch: FileChannel, offset: Long, length: Int): ByteArray? {
        if (offset < 0 || length <= 0) return null
        val buf = ByteBuffer.allocate(length)
        var at = offset
        while (buf.hasRemaining()) { val n = ch.read(buf, at); if (n <= 0) break; at += n }
        return buf.array().copyOf(buf.position()).takeIf { it.isNotEmpty() }
    }

    private fun inflate(input: ByteArray, want: Int): ByteArray? {
        val inf = Inflater()
        return try {
            inf.setInput(input)
            val out = ByteArray(want)
            var n = 0
            while (n < want && !inf.finished()) { val r = inf.inflate(out, n, want - n); if (r == 0 && (inf.needsInput() || inf.needsDictionary())) break; n += r }
            out.copyOf(n)
        } catch (e: java.util.zip.DataFormatException) { null } finally { inf.end() }
    }
}
