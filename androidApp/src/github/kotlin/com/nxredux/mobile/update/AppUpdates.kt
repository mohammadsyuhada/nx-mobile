package com.nxredux.mobile.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * One updater per process, on a scope no activity cancels: a download survives rotation and a game launch. The scope
 * runs on [Dispatchers.Main.immediate]: the controller's fields and the platform's busy checks are main-thread state;
 * the platform moves its I/O to IO itself.
 */
object AppUpdates {
    @Volatile private var instance: UpdateController? = null

    fun get(context: Context): UpdateController? = instance ?: synchronized(this) {
        instance ?: run {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            UpdateController(AndroidUpdatePlatform(context.applicationContext, scope), scope, System::currentTimeMillis)
        }.also { instance = it }
    }

    fun current(): UpdateController? = instance
}
