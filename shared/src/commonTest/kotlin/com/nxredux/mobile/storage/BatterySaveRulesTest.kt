package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BatterySaveRulesTest {
    private val sram = byteArrayOf(1, 2, 3, 4)

    @Test fun unchanged_bytes_are_not_rewritten_so_the_mtime_stays_and_no_export_follows() {
        assertEquals(BatteryWrite.UNCHANGED, BatterySaveRules.decide(true, sram) { byteArrayOf(1, 2, 3, 4) })
    }

    @Test fun changed_or_missing_saves_are_written() {
        assertEquals(BatteryWrite.WRITE, BatterySaveRules.decide(true, sram) { byteArrayOf(1, 2, 3, 5) })
        assertEquals(BatteryWrite.WRITE, BatterySaveRules.decide(true, sram) { null })
        // Same prefix, different length: a change.
        assertEquals(BatteryWrite.WRITE, BatterySaveRules.decide(true, sram) { byteArrayOf(1, 2, 3) })
    }

    @Test fun empty_sram_and_an_unwritable_file_are_skipped_without_reading_the_file() {
        var read = false
        val existing = { read = true; sram }
        assertEquals(BatteryWrite.SKIP, BatterySaveRules.decide(true, ByteArray(0), existing))
        assertEquals(BatteryWrite.SKIP, BatterySaveRules.decide(false, sram, existing))
        assertFalse(read)
    }

    // As minarch's SRAM_write, the game's save memory is written whole; a longer file (a VBA/mGBA clock footer
    // after a Game Boy save) is not protected, it becomes the save memory's size.
    @Test fun sram_smaller_than_the_file_is_written_as_the_handheld_does() {
        assertEquals(BatteryWrite.WRITE, BatterySaveRules.decide(true, sram) { sram + ByteArray(48) })
    }

    @Test fun decoded_bytes_equal_to_the_file_are_unchanged() {
        val raw = byteArrayOf(1, 2, 3)
        assertEquals(BatteryWrite.UNCHANGED, BatterySaveRules.decide(true, raw) { raw.copyOf() })
    }
}
