package com.nxredux.mobile.storage

enum class BatteryWrite { WRITE, UNCHANGED, SKIP }

/**
 * Whether a game's battery save is written. As minarch's SRAM_write, the game's save memory is written whole: a file that
 * held more (a VBA/mGBA clock footer after a Game Boy save) becomes the save memory's size. Empty SRAM (none, or a core
 * that keeps its own files) and a file the game could not have used ([writable] false: a damaged one that could not be
 * set aside) are skipped. [bytes] and [existing] are decoded (RZIP files compare by content); bytes equal to the file's
 * are left alone, so its mtime stays: a menu open, a quit and a Home in a row write a save at most once.
 */
object BatterySaveRules {
    fun decide(writable: Boolean, bytes: ByteArray, existing: () -> ByteArray?): BatteryWrite = when {
        !writable || bytes.isEmpty() -> BatteryWrite.SKIP
        existing()?.contentEquals(bytes) == true -> BatteryWrite.UNCHANGED
        else -> BatteryWrite.WRITE
    }
}
