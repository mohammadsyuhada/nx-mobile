package com.nxredux.mobile.runner

import com.nxredux.mobile.storage.BatterySaveRules
import com.nxredux.mobile.storage.BatteryWrite
import java.io.File

/**
 * minarch's clock file, `<rom.ext>.rtc` beside the battery save (`Saves/<console tag>/` here, minarch's `Saves/<TAG>/`;
 * ma_saves.c RTC_read/RTC_write): raw, any format; loaded into the core's clock memory at launch and written whenever
 * the battery save is. Empty clock memory (a core without one) and unchanged bytes are skipped ([BatterySaveRules]
 * with nothing to protect).
 */
class ClockSave(private val file: File) {
    fun load(): ByteArray? = runCatching { file.takeIf { it.isFile }?.readBytes() }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** Returns true when written. Throws IOException when the write fails. */
    fun write(bytes: ByteArray): Boolean =
        when (BatterySaveRules.decide(true, bytes) { runCatching { file.takeIf { it.isFile }?.readBytes() }.getOrNull() }) {
            BatteryWrite.WRITE -> { file.writeAtomically(bytes); true }
            else -> false
        }
}
