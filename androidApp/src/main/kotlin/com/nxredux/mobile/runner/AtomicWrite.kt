package com.nxredux.mobile.runner

import java.io.File
import java.io.IOException
import kotlin.random.Random

/**
 * Writes via a sibling temp file + rename, so a crash or kill never leaves a half-written file. The temp
 * name is unique per call, so concurrent writers of one file don't share it: the last rename wins. The
 * temp file is removed when the write or the rename fails.
 */
fun File.writeAtomically(bytes: ByteArray) {
    val dir = parentFile
    dir?.mkdirs()
    // createNewFile is atomic, so two writers can never claim the same name. A short suffix keeps long ROM
    // names under the file-name length limit.
    val tmp = generateSequence { File(dir, "$name.${Random.nextInt().toUInt().toString(16)}.tmp") }
        .first { it.createNewFile() }
    try {
        tmp.outputStream().use { it.write(bytes); it.fd.sync() }
        if (!tmp.renameTo(this)) throw IOException("rename failed: $path")
    } catch (e: Throwable) {
        tmp.delete()
        throw e
    }
}
