package com.nxredux.mobile.cheats

/**
 * Dolphin's cheats: the codes it ships (it writes them to [BUILT_IN_DIR] at boot, as a `.cht` named after the game
 * file) plus the player's own. The patched `retro_cheat_set` matches a built-in code by its text and toggles it, and
 * adds an unknown one (Gecko, or Action Replay when the code starts with `ar:`), so built-in codes go back verbatim.
 */
object DolphinCheats {
    const val BUILT_IN_DIR = "cheats/dolphin-emu"

    /** A built-in code's info (and stored key) prefix, which tells it from the player's own. */
    const val BUILT_IN = "Built-in:"

    private const val AR_DESC = "[AR]"
    private const val AR_PREFIX = "ar:"

    /** Dolphin names the file after the game file it loaded: the ROM-cache copy, so [romFileName] without its extension. */
    fun builtInFile(filesDir: String, romFileName: String): String =
        "$filesDir/$BUILT_IN_DIR/${romFileName.substringBeforeLast('.')}.cht"

    /** Built-in codes first (off, whatever the file says), then the player's; a code already listed is not listed again. */
    fun combine(builtInText: String?, player: CheatSet): CheatSet {
        val builtIn = builtInText?.let(CheatFile::parse).orEmpty().map {
            CheatEntry(it.desc, it.code, "$BUILT_IN ${it.desc}", enabled = false)
        }
        val own = player.entries.map {
            if (it.desc.startsWith(AR_DESC) && !it.code.startsWith(AR_PREFIX)) it.copy(code = AR_PREFIX + it.code) else it
        }
        val seen = HashSet<String>()
        return player.copy(entries = (builtIn + own).filter { seen.add(it.code) })
    }

    /**
     * A first launch combined again once Dolphin has written its list (by the first frame). Toggles made meanwhile stay:
     * the [enabledKeys] now are kept, plus the [saved] built-in ones (absent from the first list).
     */
    fun refresh(builtInText: String, player: CheatSet, enabledKeys: List<String>, saved: List<String>): CheatSet =
        combine(builtInText, player).withSaved(enabledKeys + saved.filter { it.startsWith(BUILT_IN) })
}
