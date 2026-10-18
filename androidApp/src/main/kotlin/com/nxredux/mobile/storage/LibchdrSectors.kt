package com.nxredux.mobile.storage

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.nxredux.mobile.library.disc.ChdSectors
import com.nxredux.mobile.runner.ContentRef
import com.swordfish.libretrodroid.LibretroDroid
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [ChdSectors] through libretrodroid's libchdr: the CHD's fd (a file path, or a SAF document) is handed over and closed there. */
class LibchdrSectors(private val resolver: ContentResolver) : ChdSectors {
    override suspend fun read(file: ContentRef, sectors: List<Int>): List<ByteArray?>? = withContext(Dispatchers.IO) {
        runCatching {
            val fd = if (FileRefs.isFile(file)) ParcelFileDescriptor.open(File(FileRefs.path(file)), ParcelFileDescriptor.MODE_READ_ONLY).detachFd()
                else resolver.openFileDescriptor(Uri.parse(file.value), "r")?.detachFd() ?: return@runCatching null
            LibretroDroid.chdReadSectors(fd, sectors.toIntArray())?.map { b -> b?.takeIf { it.size == 2048 } }
        }.getOrNull()
    }
}
