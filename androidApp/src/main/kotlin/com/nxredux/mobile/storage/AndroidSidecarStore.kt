package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.writeAtomically
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Small text files for folders whose tree cannot be written: `.userdata/android/sidecars/<sourceId>/<relativeDir>/<file>`. */
class AndroidSidecarStore(context: Context) : SidecarStore {
    private val root = File(NxStorage.root(context), ".userdata/android/sidecars")

    private fun dir(sourceId: String, relativeDir: String) = File(File(root, sourceId), relativeDir)

    override suspend fun read(sourceId: String, relativeDir: String, file: String): String? = withContext(Dispatchers.IO) {
        runCatching { File(dir(sourceId, relativeDir), file).takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override suspend fun write(sourceId: String, relativeDir: String, file: String, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(dir(sourceId, relativeDir), file).writeAtomically(text.encodeToByteArray()); true }
            .onFailure { NxLog.e("SidecarStore", "write failed", it) }.getOrDefault(false)
    }

    override suspend fun list(sourceId: String, relativeDir: String): List<String> = withContext(Dispatchers.IO) {
        runCatching { dir(sourceId, relativeDir).listFiles()?.filter { it.isFile }?.map { it.name }?.sorted() }.getOrNull() ?: emptyList()
    }

    override suspend fun delete(sourceId: String, relativeDir: String, file: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(dir(sourceId, relativeDir), file).let { it.isFile && it.delete() } }
            .onFailure { NxLog.w("SidecarStore", "delete failed", it) }.getOrDefault(false)
    }
}
