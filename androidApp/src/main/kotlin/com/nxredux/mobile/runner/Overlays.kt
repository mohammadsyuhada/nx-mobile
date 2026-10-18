package com.nxredux.mobile.runner

import android.content.Context
import com.nxredux.mobile.options.OverlayResolver
import com.nxredux.mobile.options.OverlaySets
import java.io.File

/** Overlay PNGs on the SD root (`Overlays/<TAG>/{768p,720p}/`, plus flat Brick copies): the bundled sets, app-owned. */
object Overlays {
    /** The overlay file names present for [tag] (a GPGX_<X> tag reads <X>'s, [OverlayResolver.folder]). Blocking I/O. */
    fun sets(sdRoot: File, tag: String): OverlaySets {
        val base = File(sdRoot, OverlayResolver.folder(tag))
        fun list(dir: File): List<String> =
            dir.listFiles()?.filter { it.isFile && OverlayResolver.isOverlayFile(it.name) }?.map { it.name }.orEmpty()
        return OverlaySets(p768 = list(File(base, "768p")), p720 = list(File(base, "720p")), flat = list(base))
    }

    /**
     * Syncs `sd/Overlays/` with the APK's overlay sets: the tree is app-owned and replaced whole on APK update
     * (see [BundledAssets.sync]). Blocking I/O: call off the main thread.
     */
    fun extractBundled(context: Context): Boolean = BundledAssets.sync(context, "overlays", "Overlays")
}
