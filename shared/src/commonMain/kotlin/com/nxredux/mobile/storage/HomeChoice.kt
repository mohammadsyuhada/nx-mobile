package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef

/** Which folder is the home now: the saved one when it can be read, else the app's Android/media folder. */
object HomeChoice {
    /**
     * A saved file ref is kept while All files access is [granted] and it is a directory ([isDirectory]); a saved SAF ref
     * is kept as is (legacy until the upgrade screen runs). Anything else falls back to [media].
     */
    fun effective(saved: ContentRef?, granted: Boolean, isDirectory: (ContentRef) -> Boolean, media: ContentRef?): ContentRef? = when {
        saved == null -> media
        !FileRefs.isFile(saved) -> saved
        granted && isDirectory(saved) -> saved
        else -> media
    }
}
