package com.nxredux.mobile.cheats

import com.nxredux.mobile.runner.CoreRegistry

/**
 * Which `Cheats/<TAG>/` folders a game's cheats come from: one per console, as in `Roms/`, so a second core's tag reads
 * its console's folder (MGBA → GBA, SUPA → SFC, PSX → PS, GPGX → MD, GPGX_SMS → SMS). A console's folder can hold another
 * system's games (a Sega CD disc or a Game Gear game in Sega Genesis (MD), a `.fds` in Nintendo ES (FC)), and libretro
 * keeps each system's cheats apart: the same file name is often a different game on another system, so folders are never
 * merged. The ROM's extension picks the system's folder instead; an extension shared by several systems (`bin`, `zip`)
 * keeps the console's own.
 */
object CheatFolders {
    private val segaCd = mapOf("cue" to "SEGACD", "chd" to "SEGACD", "iso" to "SEGACD", "m3u" to "SEGACD")
    private val satellaview = mapOf("bs" to "BS")

    /** Tag → ROM extension → the tag whose folder holds that system's cheats. */
    val ROUTES: Map<String, Map<String, String>> = mapOf(
        "MD" to mapOf("sms" to "SMS", "gg" to "GG", "sg" to "SG1000") + segaCd,
        "PCE" to listOf("cue", "ccd", "chd", "toc", "m3u").associateWith { "PCECD" },
        "SFC" to satellaview,
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

    /** [tag]'s console's tag (its first, as `Roms/` names the console's folder); an unknown tag stays as it is. */
    fun console(tag: String): String = CoreRegistry.consoleOf(tag)?.tags?.first() ?: CoreRegistry.baseTag(tag)

    /** The tags whose folders [tag]'s games can read (the database installs these along with [tag]). */
    fun targets(tag: String): Set<String> =
        console(tag).let { t -> setOf(t) + ROUTES[t].orEmpty().values + listOfNotNull(PARTNERS[t]) }

    /**
     * The folder tags to look in for [romFileName] run under [tag], in order; the first that yields cheats wins. A routed
     * game reads its system's folder and not the tag's own (that holds another system's cheats under the same names),
     * except where the two share a library ([PARTNERS]: then the other is read second).
     */
    fun tagsFor(tag: String, romFileName: String): List<String> {
        val key = console(tag)
        val system = systemOf(key, romFileName)
        return if (key in PARTNERS) listOf(system) + listOf(key, PARTNERS.getValue(key)).filter { it != system } else listOf(system)
    }

    /** The folder the Cheats page names when nothing matched: where the database's files for this game go. */
    fun primary(tag: String, romFileName: String): String = "Cheats/${systemOf(console(tag), romFileName)}"

    private fun systemOf(key: String, romFileName: String): String {
        val ext = romFileName.substringAfterLast('.', "").lowercase()
        return ROUTES[key]?.get(ext) ?: key
    }
}
