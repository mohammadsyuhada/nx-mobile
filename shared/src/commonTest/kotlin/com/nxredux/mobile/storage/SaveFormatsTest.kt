package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveFormatsTest {
    @Test fun save_formats_follow_the_handheld_order_labels_and_values() {
        assertEquals(listOf("MinUI", "Retroarch (compressed)", "Retroarch (uncompressed)", "Generic"), SaveFormat.entries.map { it.label })
        assertEquals(listOf(0, 1, 3, 2), SaveFormat.entries.map { it.stored })
        assertEquals(SaveFormat.RETROARCH, SaveFormat.DEFAULT)
        assertEquals(listOf(SaveFormat.RETROARCH_COMPRESSED), SaveFormat.entries.filter { it.compressed })
    }
    @Test fun state_formats_follow_the_handheld_order_labels_and_values() {
        assertEquals(
            listOf("MinUI", "Retroarch-ish (compressed)", "Retroarch-ish (uncompressed)", "Retroarch (compressed)", "Retroarch (uncompressed)"),
            StateFormat.entries.map { it.label },
        )
        assertEquals(listOf(0, 1, 2, 3, 4), StateFormat.entries.map { it.stored })
        assertEquals(StateFormat.RETROARCH, StateFormat.DEFAULT)
        assertEquals(listOf(StateFormat.RETROARCH_ISH_COMPRESSED, StateFormat.RETROARCH_COMPRESSED), StateFormat.entries.filter { it.compressed })
    }
    @Test fun stored_values_round_trip_and_unknown_reads_as_default() {
        SaveFormat.entries.forEach { assertEquals(it, SaveFormat.fromStored(it.stored)) }
        StateFormat.entries.forEach { assertEquals(it, StateFormat.fromStored(it.stored)) }
        assertEquals(SaveFormat.DEFAULT, SaveFormat.fromStored(null)); assertEquals(SaveFormat.DEFAULT, SaveFormat.fromStored(7))
        assertEquals(StateFormat.DEFAULT, StateFormat.fromStored(-1))
    }
    @Test fun step_wraps_both_ways() {
        assertEquals(SaveFormat.MINUI, SaveFormat.GENERIC.step(1)); assertEquals(SaveFormat.GENERIC, SaveFormat.MINUI.step(-1))
        assertEquals(StateFormat.MINUI, StateFormat.RETROARCH.step(1)); assertEquals(StateFormat.RETROARCH, StateFormat.MINUI.step(-1))
    }
    @Test fun naming_defaults_to_the_handheld_defaults() {
        assertEquals(SaveNaming(SaveFormat.RETROARCH, StateFormat.RETROARCH, false), SaveNaming.DEFAULT)
        assertFalse(SaveNaming.DEFAULT.useExtractedFileName)
        assertTrue(SaveNaming(useExtractedFileName = true).useExtractedFileName)
    }
}
