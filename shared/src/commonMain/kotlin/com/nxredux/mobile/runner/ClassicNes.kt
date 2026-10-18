package com.nxredux.mobile.runner

/**
 * The "Classic NES Series" / "Famicom Mini" GBA carts (Pac-Man FP7E, Super Mario Bros. FSME ...) do not run
 * under gpSP (a 4 MiB dump crashes the app, a 1 MiB one shows black); mGBA runs them. Known by the GBA ROM header: the
 * fixed 0x96 at 0xB2 and a game code (0xAC, four ASCII letters or digits) starting with 'F', the family's own letter
 * (GBATEK). The header checksum at 0xBD is not checked: a false positive only means the game runs mGBA.
 */
object ClassicNes {
    /** The bytes [isClassicNes] reads from the ROM's start. */
    const val HEAD_BYTES = 0xC0
    const val GPSP_TAG = "GBA"
    const val MGBA_TAG = "MGBA"

    private const val GAME_CODE = 0xAC
    private const val FIXED = 0xB2

    /** [head] (the ROM's first bytes, null when unread) is a GBA header whose game code is a Classic NES Series one. */
    fun isClassicNes(head: ByteArray?): Boolean {
        if (head == null || head.size < FIXED + 1 || head[FIXED] != 0x96.toByte()) return false
        val code = (GAME_CODE until GAME_CODE + 4).map { head[it].toInt().toChar() }
        return code[0] == 'F' && code.all { it in 'A'..'Z' || it in '0'..'9' }
    }

    /**
     * The tag a game listed under [listedTag] launches with: [MGBA_TAG] for a Classic NES Series cart ([head], read only
     * when it matters) that would run gpSP only because gpSP is the console's default ([perGameTag], its Emulator pick
     * in `systems.txt` when it is one of the console's tags, is null); else [listedTag]. A per-game pick, even GBA, wins.
     */
    inline fun launchTag(listedTag: String, perGameTag: String?, head: () -> ByteArray?): String =
        if (listedTag == GPSP_TAG && perGameTag == null && isClassicNes(head())) MGBA_TAG else listedTag
}
