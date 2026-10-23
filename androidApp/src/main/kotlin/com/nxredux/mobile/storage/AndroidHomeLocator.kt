package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.settings.AndroidAppSettings
import java.io.File

/** [HomeLocator] on the saved `treeRef`, All files access and [MediaHome], whose skeleton every call makes sure of. */
class AndroidHomeLocator(context: Context) : HomeLocator {
    private val app = context.applicationContext
    private val settings = AndroidAppSettings(app)

    override val media: ContentRef get() = MediaHome.ref(app)

    override fun current(): ContentRef? {
        MediaHome.ensure(MediaHome.dir(app))
        return HomeChoice.effective(settings.treeRef, AndroidAllFilesAccess.isGranted(), { File(FileRefs.path(it)).isDirectory }, media)
    }
}
