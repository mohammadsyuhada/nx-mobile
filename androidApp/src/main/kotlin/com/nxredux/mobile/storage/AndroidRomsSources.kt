package com.nxredux.mobile.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import com.nxredux.mobile.library.RomsSource
import com.nxredux.mobile.library.RoutingRomsSource
import java.io.File

/** The app's RomsSource: SAF trees, and file refs read with All files access or inside the app's media folder ([RoutingRomsSource]). */
object AndroidRomsSources {
    fun of(context: Context): RomsSource {
        val app = context.applicationContext
        val storage = app.getSystemService(StorageManager::class.java)
        val access = AndroidAllFilesAccess(app)
        // The app's own media folder (the home without All files access) reads and writes without the permission.
        val media = MediaHome.cachedDir(app)
        return RoutingRomsSource(SafRomsSource(app.contentResolver, storage), FileRomsSource(access::granted, mountedOn(storage)) { MediaHome.owns(media, it) })
    }

    /** True when a path's volume is mounted (read-only too) and, on Android 11+, its root reads; a path on no volume is not. */
    fun mountedOn(storage: StorageManager?): (File) -> Boolean = { file ->
        val volume = storage?.getStorageVolume(file)
        val state = volume?.state
        (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || volume?.directory?.canRead() == true)
    }
}
