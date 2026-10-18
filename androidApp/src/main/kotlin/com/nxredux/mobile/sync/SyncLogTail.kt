package com.nxredux.mobile.sync

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Follows the rsync daemon's [log] (written by the daemon, read here): [readNew] hands each newly completed line to
 * [onLine]; [flush] the unterminated rest once the daemon is gone. Lines split on the `\n` byte and decode as UTF-8
 * whole, so a character cut by a read boundary survives; a trailing `\r` is dropped. Not thread-safe: one reader at a
 * time (the controller's tallies assume lines arrive in order from one reader).
 */
internal class SyncLogTail(private val log: File, private val onLine: (String) -> Unit) {
    private var pos = 0L
    private val partial = ByteArrayOutputStream()

    /** Reads what was appended since the last call; a log shorter than what was read (re-created) restarts at 0. */
    fun readNew() {
        val len = log.length()   // 0 when missing
        if (len < pos) { pos = 0; partial.reset() }
        if (len == pos) return
        RandomAccessFile(log, "r").use { f ->
            f.seek(pos)
            val buf = ByteArray(CHUNK)
            while (true) {
                val n = f.read(buf)
                if (n <= 0) break
                pos += n
                var start = 0
                for (i in 0 until n) {
                    if (buf[i] == NL) {
                        partial.write(buf, start, i - start)
                        emit()
                        start = i + 1
                    }
                }
                partial.write(buf, start, n - start)
            }
        }
    }

    /** Delivers a last line that never got its newline; nothing when there is none. */
    fun flush() {
        if (partial.size() > 0) emit()
    }

    private fun emit() {
        val line = partial.toString(Charsets.UTF_8.name()).removeSuffix("\r")
        partial.reset()
        onLine(line)
    }

    private companion object {
        const val CHUNK = 8192
        const val NL = '\n'.code.toByte()
    }
}
