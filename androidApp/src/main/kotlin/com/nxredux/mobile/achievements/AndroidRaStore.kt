package com.nxredux.mobile.achievements

import android.content.Context
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.writeAtomically
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RaStore under `sd/.userdata/`. Appends are fsync'd; whole-file writes go through writeAtomically (which
 * creates the parent). A missing file reads as null and lists as empty; every failure is an [RaStoreException].
 */
class AndroidRaStore(context: Context) : RaStore {
    private val root = File(NxStorage.root(context), RaPaths.ROOT).canonicalFile

    /** [path] under the ra root; anything that resolves outside it (`..`, an absolute path, a symlink) is refused. */
    private fun file(path: String): File {
        val f = try { File(root, path).canonicalFile } catch (e: IOException) { throw RaStoreException("resolve $path", e) }
        if (f != root && !f.path.startsWith(root.path + File.separator)) throw RaStoreException("outside ra root: $path")
        return f
    }

    private suspend fun <T> io(what: String, block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: RaStoreException) {
            throw e
        } catch (e: IOException) {
            throw RaStoreException(what, e)
        } catch (e: SecurityException) {
            throw RaStoreException(what, e)
        }
    }

    override suspend fun read(path: String): ByteArray? = io("read $path") { file(path).takeIf { it.isFile }?.readBytes() }

    override suspend fun writeAtomic(path: String, bytes: ByteArray) = io("write $path") { file(path).writeAtomically(bytes) }

    override suspend fun append(path: String, bytes: ByteArray) = io("append $path") {
        val f = file(path)
        f.parentFile?.mkdirs()
        FileOutputStream(f, true).use { it.write(bytes); it.fd.sync() }
    }

    override suspend fun size(path: String): Long? = io("size $path") { file(path).takeIf { it.isFile }?.length() }

    override suspend fun delete(path: String) { io("delete $path") { file(path).delete() } }

    /** Deletes [dir] recursively; the store root itself is refused (it holds every app's `.userdata`). */
    override suspend fun deleteTree(dir: String) {
        if (dir.isEmpty()) throw RaStoreException("refusing to delete the store root")
        io("delete $dir/") { file(dir).deleteRecursively() }
    }

    override suspend fun list(dir: String): List<String> = io("list $dir") { file(dir).list()?.toList() ?: emptyList() }

    override suspend fun exists(path: String): Boolean = io("exists $path") { file(path).exists() }
}
