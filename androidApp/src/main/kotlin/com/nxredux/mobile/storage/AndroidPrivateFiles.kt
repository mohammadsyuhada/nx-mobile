package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/** App-private text files at `filesDir/sd/.userdata/android/<name>` (the artwork job and its written-art log). */
class AndroidPrivateFiles internal constructor(private val dir: File) : PrivateFiles {
    constructor(context: Context) : this(File(NxStorage.root(context), ".userdata/android"))

    private fun file(name: String): File {
        require(name.isNotEmpty() && '/' !in name && name != "." && name != "..") { "bad private file name" }
        return File(dir, name)
    }

    /** Null only when the file is absent; one that is there but can't be read (I/O error, permissions) throws. */
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val f = file(name)
        try { f.readText() } catch (e: FileNotFoundException) { if (f.exists()) throw e else null }
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        file(name).writeAtomically(text.encodeToByteArray())
    }

    /** Appends in place (created when absent), synced to disk before returning; a failure part-way may tear the last line. */
    override suspend fun append(name: String, text: String) = withContext(Dispatchers.IO) {
        val f = file(name)
        f.parentFile?.mkdirs()
        FileOutputStream(f, true).use { out ->
            out.write(text.encodeToByteArray())
            out.flush()
            out.fd.sync()
        }
    }
}
