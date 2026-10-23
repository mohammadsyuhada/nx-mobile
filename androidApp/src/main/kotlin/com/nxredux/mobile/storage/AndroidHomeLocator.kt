package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.settings.AndroidAppSettings
import java.io.File

/**
 * [HomeLocator] on the saved `treeRef`, All files access and [MediaHome]. Cheap enough for the main thread: the media
 * folder is looked up and its skeleton made once per process, so a call is a settings read, the access check and one stat.
 */
class AndroidHomeLocator(context: Context) : HomeLocator {
    private val app = context.applicationContext
    private val settings = AndroidAppSettings(app)

    override val media: ContentRef get() = mediaRef(app)

    override fun current(): ContentRef? =
        HomeChoice.effective(settings.treeRef, AndroidAllFilesAccess.isGranted(), { File(FileRefs.path(it)).isDirectory }, media)

    private companion object {
        @Volatile private var dir: File? = null

        /** [MediaHome.dir], found once; [MediaHome.ensure] returns at once after its first success (a failure retries next call). */
        fun mediaRef(app: Context): ContentRef {
            val d = dir ?: MediaHome.dir(app).also { dir = it }
            MediaHome.ensure(d)
            return FileRefs.of(d.path)
        }
    }
}
