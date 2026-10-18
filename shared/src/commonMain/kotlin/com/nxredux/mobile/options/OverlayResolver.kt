package com.nxredux.mobile.options

import com.nxredux.mobile.runner.CoreRegistry

/** Overlay file names found for one console: `768p/`, `720p/` and directly in `Overlays/<TAG>/` (a Brick copy). */
data class OverlaySets(
    val p768: List<String> = emptyList(),
    val p720: List<String> = emptyList(),
    val flat: List<String> = emptyList(),
)

/** An overlay picked for the current orientation: [path] relative to the SD root, [aspect] of its set. */
data class OverlayFile(val path: String, val aspect: Float)

/**
 * nx-redux ships a 768p (Brick, 4:3) and a 720p (Smart Pro S, 16:9) overlay set per console; the phone uses
 * the 768p set in portrait and the 720p set in landscape, falling back to the other set for a name only one
 * has. Flat files count as 768p.
 */
object OverlayResolver {
    const val ASPECT_768P = 4f / 3f
    const val ASPECT_720P = 16f / 9f

    /** A visible `.png` (skips `.keep`, macOS `._*` files and anything else). */
    fun isOverlayFile(name: String): Boolean = !name.startsWith(".") && name.endsWith(".png", ignoreCase = true)

    /** The names offered in the Overlay setting: the union of every set, sorted case-insensitively. */
    fun names(sets: OverlaySets): List<String> =
        (sets.p768 + sets.p720 + sets.flat).filter(::isOverlayFile).distinct()
            .sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })

    /** [tag]'s overlay folder: a console's screen, so a GPGX_<X> tag uses <X>'s ([CoreRegistry.baseTag]: GPGX_GG → `Overlays/GG`). */
    fun folder(tag: String): String = "Overlays/${CoreRegistry.baseTag(tag)}"

    fun resolve(tag: String, sets: OverlaySets, name: String?, landscape: Boolean): OverlayFile? {
        if (name == null || name == FrontendOptions.NONE || !isOverlayFile(name)) return null
        val dir = folder(tag)
        val p768 = when (name) {
            in sets.p768 -> OverlayFile("$dir/768p/$name", ASPECT_768P)
            in sets.flat -> OverlayFile("$dir/$name", ASPECT_768P)
            else -> null
        }
        val p720 = if (name in sets.p720) OverlayFile("$dir/720p/$name", ASPECT_720P) else null
        return if (landscape) p720 ?: p768 else p768 ?: p720
    }
}
