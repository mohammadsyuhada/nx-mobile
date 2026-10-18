package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.writeAtomically
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The library index at `filesDir/sd/.userdata/android/library.json`. A cache: deleting it only costs a scan. */
class AndroidLibraryIndexStore(context: Context) : LibraryIndexStore {
    private val file = File(NxStorage.root(context), ".userdata/android/library.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { file.takeIf { it.exists() }?.readText() }
            .onFailure { NxLog.w(TAG, "read failed", it) }.getOrNull()
    }

    override suspend fun write(text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { file.writeAtomically(text.encodeToByteArray()); true }
            .onFailure { NxLog.w(TAG, "write failed; the next start scans", it) }.getOrDefault(false)
    }

    override suspend fun delete() {
        withContext(Dispatchers.IO) { if (file.exists() && !file.delete()) NxLog.w(TAG, "could not delete ${file.path}") }
    }

    private companion object { const val TAG = "LibraryIndexStore" }
}
