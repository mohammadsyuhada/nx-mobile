package com.nxredux.mobile.cheats

import com.nxredux.mobile.runner.CoreRegistry

/**
 * Which `Cheats/<TAG>/` folders a game's cheats come from. A tag's folder can hold another system's games (a Sega CD
 * disc in Sega Genesis (MD), a `.fds` in Nintendo ES (FC), Game Gear in Sega Game Gear (GPGX)), and libretro keeps
 * each system's cheats apart: the same file name is often a different game on another system, so folders are never
 * merged. The ROM's extension picks the system's folder instead; an extension shared by several systems (`bin`, `zip`)
 * keeps the tag's own.
 */
object CheatFolders {
    private val segaCd = mapOf("cue" to "SEGACD", "chd" to "SEGACD", "iso" to "SEGACD", "m3u" to "SEGACD")
    private val satellaview = mapOf("bs" to "BS")

    /** Tag → ROM extension → the tag whose folder holds that system's cheats. */
    val ROUTES: Map<String, Map<String, String>> = mapOf(
        "GPGX" to mapOf("sms" to "SMS", "gg" to "GG", "sg" to "SG1000") + segaCd,
        "MD" to segaCd,
        "PCE" to listOf("cue", "ccd", "chd", "toc", "m3u").associateWith { "PCECD" },
        "SFC" to satellaview,
        "SUPA" to satellaview,
        "FC" to mapOf("fds" to "FDS"),
        "GB" to mapOf("gbc" to "GBC"),
        "SGB" to mapOf("gbc" to "GBC"),
        "GBC" to mapOf("gb" to "GB", "dmg" to "GB"),
    )

    /**
     * Tags whose system shares its library with another: a dual-mode Game Boy cartridge is filed under Game Boy whatever
     * its extension, so a game that finds nothing in its folder tries the partner's. Not for Sega or PC Engine, where the
     * same name in the other folder is another game.
     */
    val PARTNERS: Map<String, String> = mapOf("GB" to "GBC", "SGB" to "GBC", "GBC" to "GB")

    /**
     * Tags with no folder of their own in the database, and the folder their unrouted games read. GPGX is Sega Genesis's
     * Genesis Plus GX tag: its folder still holds whatever the user put there, read first.
     */
    val FALLBACKS: Map<String, String> = mapOf("GPGX" to "MD")

    /**
     * The tags whose folders [tag]'s games can read (the database installs these along with [tag]). Here and below a
     * GPGX_<X> tag reads as <X> ([CoreRegistry.baseTag]): Genesis Plus GX on Master System reads `Cheats/SMS`.
     */
    fun targets(tag: String): Set<String> =
        CoreRegistry.baseTag(tag).let { t -> ROUTES[t].orEmpty().values.toSet() + listOfNotNull(FALLBACKS[t], PARTNERS[t]) }

    /**
     * The folder tags to look in for [romFileName] run under [tag], in order; the first that yields cheats wins. A routed
     * game reads its system's folder and not the tag's own (that holds another system's cheats under the same names),
     * except where the two share a library ([PARTNERS]: then the other is read second). A tag with no database folder
     * ([FALLBACKS]) is read first, as only the user puts files there.
     */
    fun tagsFor(tag: String, romFileName: String): List<String> {
        val key = CoreRegistry.baseTag(tag)
        val system = systemOf(key, romFileName)
        return when {
            key in FALLBACKS -> listOf(key, system)
            key in PARTNERS -> listOf(system) + listOf(key, PARTNERS.getValue(key)).filter { it != system }
            else -> listOf(system)
        }
    }

    /** The folder the Cheats page names when nothing matched: where the database's files for this game go. */
    fun primary(tag: String, romFileName: String): String = "Cheats/${systemOf(CoreRegistry.baseTag(tag), romFileName)}"

    private fun systemOf(key: String, romFileName: String): String {
        val ext = romFileName.substringAfterLast('.', "").lowercase()
        return ROUTES[key]?.get(ext) ?: FALLBACKS[key] ?: key
    }
}
