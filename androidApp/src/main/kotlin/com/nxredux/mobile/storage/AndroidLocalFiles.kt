package com.nxredux.mobile.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [LocalFiles] over java.io.File (All files access); [volumes] from the platform ([deviceVolumes]). */
class AndroidLocalFiles(private val volumes: () -> List<LocalVolume>) : LocalFiles {
    override suspend fun volumes(): List<LocalVolume> = withContext(Dispatchers.IO) { runCatching(volumes).getOrDefault(emptyList()) }

    override suspend fun list(path: String): List<LocalEntry> = withContext(Dispatchers.IO) {
        runCatching { File(path).listFiles()?.map { LocalEntry(it.name, it.isDirectory) } }.getOrNull() ?: emptyList()
    }

    override suspend fun mkdir(parent: String, name: String): Boolean = withContext(Dispatchers.IO) {
        FileRefs.isPlainName(name) && File(parent, name).mkdir()
    }

    override suspend fun isDirectory(path: String): Boolean = withContext(Dispatchers.IO) { File(path).isDirectory }

    override suspend fun canWrite(path: String): Boolean = withContext(Dispatchers.IO) { File(path).let { it.isDirectory && it.canWrite() } }

    companion object {
        /** The mounted volumes (read-only ones too) by their directory (Android 11+) and the name Android gives them. */
        fun deviceVolumes(context: Context): () -> List<LocalVolume> = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) emptyList()
            else context.getSystemService(StorageManager::class.java).storageVolumes
                .filter { it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY }
                .mapNotNull { v -> v.directory?.let { LocalVolume(v.getDescription(context), it.absolutePath, primary = v.isPrimary) } }
        }
    }
}
