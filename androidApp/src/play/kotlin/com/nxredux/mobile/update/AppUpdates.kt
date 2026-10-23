package com.nxredux.mobile.update

import android.content.Context

/** The play flavor carries no updater: a store build is updated by the store, never by downloading an APK itself. */
object AppUpdates {
    @Suppress("UNUSED_PARAMETER")
    fun get(context: Context): UpdateController? = null

    fun current(): UpdateController? = null
}
