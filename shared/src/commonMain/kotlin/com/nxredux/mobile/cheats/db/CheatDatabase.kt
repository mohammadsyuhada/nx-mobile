package com.nxredux.mobile.cheats.db

import com.nxredux.mobile.cheats.CheatFolders

/**
 * libretro's cheat collection as nx-redux's `cheatdb` tool installs it: one buildbot zip whose top-level
 * folders are libretro system names. [FOLDERS] maps NX console tags to those folders (`cheatdb_data.c`).
 */
object CheatDatabase {
    const val URL = "https://buildbot.libretro.com/assets/frontend/cheats.zip"
    const val DOWNLOAD_HINT = "about 37 MB"
    /**
     * Measured: the extracted subset takes ~250 MB on disk because its ~16k small files each round up to whole
     * blocks, plus the 35 MB zip and the temp files written during install.
     */
    const val NEEDED_FREE_BYTES: Long = 400L shl 20

    /**
     * NX tag → libretro folders, in nx-redux's order. Several tags share a folder (GB/SGB, GBA/MGBA, SFC/SUPA); one tag
     * can take several (WSC runs both WonderSwan systems, where nx-redux has WS and WSC). PCECD and BS are not tags: they
     * hold the cheats [CheatFolders] routes PC Engine CD and Satellaview games to. GPGX has no row (its games route to
     * MD, SMS, GG, SG1000 and SEGACD); nx-redux's MSX row is dropped (no MSX tag here, and libretro split that folder).
     */
    val FOLDERS: Map<String, List<String>> = linkedMapOf(
        "FC" to listOf("Nintendo - Nintendo Entertainment System"),
        "FDS" to listOf("Nintendo - Family Computer Disk System"),
        "GB" to listOf("Nintendo - Game Boy"),
        "SGB" to listOf("Nintendo - Game Boy"),
        "GBC" to listOf("Nintendo - Game Boy Color"),
        "GBA" to listOf("Nintendo - Game Boy Advance"),
        "MGBA" to listOf("Nintendo - Game Boy Advance"),
        "SFC" to listOf("Nintendo - Super Nintendo Entertainment System"),
        "SUPA" to listOf("Nintendo - Super Nintendo Entertainment System"),
        "BS" to listOf("Nintendo - Satellaview"),
        "VB" to listOf("Nintendo - Virtual Boy"),
        "N64" to listOf("Nintendo - Nintendo 64"),
        "NDS" to listOf("Nintendo - Nintendo DS"),
        "SMS" to listOf("Sega - Master System - Mark III"),
        "GG" to listOf("Sega - Game Gear"),
        "MD" to listOf("Sega - Mega Drive - Genesis"),
        "32X" to listOf("Sega - 32X"),
        "SEGACD" to listOf("Sega - Mega-CD - Sega CD"),
        "SG1000" to listOf("Sega - SG-1000"),
        "DC" to listOf("Sega - Dreamcast"),
        "PS" to listOf("Sony - PlayStation"),
        "PSX" to listOf("Sony - PlayStation"),
        "PSP" to listOf("Sony - PlayStation Portable"),
        "PCE" to listOf("NEC - PC Engine - TurboGrafx 16"),
        "PCECD" to listOf("NEC - PC Engine CD - TurboGrafx-CD"),
        "A2600" to listOf("Atari - 2600"),
        "A5200" to listOf("Atari - 5200"),
        "A7800" to listOf("Atari - 7800"),
        "LYNX" to listOf("Atari - Lynx"),
        "COLECO" to listOf("Coleco - ColecoVision"),
        "NGP" to listOf("SNK - Neo Geo Pocket"),
        "NGPC" to listOf("SNK - Neo Geo Pocket Color"),
        "WSC" to listOf("Bandai - WonderSwan Color", "Bandai - WonderSwan"),
        "FBN" to listOf("FBNeo - Arcade Games"),
        "PRBOOM" to listOf("PrBoom"),
    )

    /**
     * The rows for [tags] (case-insensitive) and the folders their games route to ([CheatFolders.targets]), in table
     * order; tags without a libretro folder are dropped.
     */
    fun foldersFor(tags: Collection<String>): Map<String, List<String>> {
        val given = tags.map { it.trim().uppercase() }
        val wanted = given.toSet() + given.flatMap(CheatFolders::targets)
        return FOLDERS.filterKeys { it in wanted }
    }
}
