package com.nxredux.mobile.storage

/**
 * All files access (Android's MANAGE_EXTERNAL_STORAGE). [declared]: this build asks for it and the platform has it
 * (Android 11+); false on a Play build or Android 8–10, where everything stays on SAF. [granted] is read again at every
 * resume (the user can revoke it in Android's settings at any time).
 */
interface AllFilesAccess {
    val declared: Boolean
    fun granted(): Boolean
    /** Opens Android's All files access page for this app (or the app list where that page is missing). */
    fun openSettings()
}
