package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef

/** Where the home folder is now ([HomeChoice.effective] on the platform's settings and access), and the app's own fallback. */
interface HomeLocator {
    /** The home to read and write now: the saved one while it can be read, else [media]. */
    fun current(): ContentRef?

    /** The app's own folder (`Android/media/<package>/` on Android), the home without All files access. */
    val media: ContentRef?
}
