package com.nxredux.mobile.runner

/**
 * The message for ERROR_LOAD_LIBRARY. The runner keeps the system loader's reason (dlerror) in the core log
 * as "E core load: <reason>" (core.cpp), so a report from a device names why the core was refused instead of guessing.
 */
internal object CoreLoadFailure {
    const val PREFIX = "E core load: "

    fun message(coreFile: String, recent: List<String>): String {
        val reason = recent.lastOrNull { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
        return if (reason != null) "The core $coreFile could not be loaded: $reason"
        else "The core $coreFile could not be loaded (ERROR_LOAD_LIBRARY)."
    }
}
