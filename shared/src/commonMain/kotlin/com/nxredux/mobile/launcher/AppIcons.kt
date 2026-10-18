package com.nxredux.mobile.launcher

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Android apps' icons by id, each asked of [launcher] once: tiles look an app's icon up here instead of loading it on
 * every composition. A missing one (null) is kept too; its tile draws the generic app glyph. Loads run in [scope], so
 * tiles asking at once share one, and one finishes for the cache when its tile leaves. [retain] drops the icons no
 * longer shown (the picker's), so the cache does not grow for the session. Main thread only.
 */
class AppIcons(private val launcher: AppLauncher, private val scope: CoroutineScope) {
    private val loaded = HashMap<String, ImageBitmap?>()
    private val loading = HashMap<String, Deferred<ImageBitmap?>>()

    /** [id]'s icon if it has loaded already (a tile's first frame); null before then, or when it has none. */
    fun cached(id: String): ImageBitmap? = loaded[id]

    /** [id]'s icon, loaded through [launcher] the first time (or joining the load under way) and cached. */
    suspend fun load(id: String): ImageBitmap? {
        if (id in loaded) return loaded[id]
        val pending = loading.getOrPut(id) {
            scope.async {
                try { launcher.icon(id) } catch (e: CancellationException) { throw e } catch (e: Exception) { null }   // a failure is a missing icon, never the scope's end
            }
        }
        val icon = pending.await()
        // The first caller back caches it; none does when [retain] dropped the app meanwhile.
        if (loading[id] === pending) { loading.remove(id); loaded[id] = icon }
        return icon
    }

    /** Keeps only [ids]' icons (and loads), dropping the rest. */
    fun retain(ids: Collection<String>) {
        val keep = ids.toSet()
        loaded.keys.retainAll(keep)
        loading.keys.retainAll(keep)
    }
}
