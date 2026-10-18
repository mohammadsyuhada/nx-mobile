package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Accent setting's presets: Tailwind colours (Red and Teal the handheld's) in the picker's order, stored by id, White when missing or unknown. */
class AccentPresetTest {
    @Test fun presetsAreTheirColoursInOrder() {
        assertEquals(
            listOf(
                "White" to 0xE4E4E7, "Red" to 0x660000, "Orange" to 0xF97316, "Amber" to 0xFBBF24, "Lime" to 0xA3E635,
                "Teal" to 0x006666, "Sky" to 0x38BDF8, "Violet" to 0x8B5CF6, "Pink" to 0xEC4899,
            ),
            AccentPreset.entries.map { it.label to it.rgb },
        )
        assertEquals(AccentPreset.WHITE, AccentPreset.DEFAULT)
        assertEquals("e4e4e7", AccentPreset.WHITE.hex)
        assertEquals(0xFF660000.toInt(), AccentPreset.RED.argb)
        assertEquals("660000", AccentPreset.RED.hex)
    }

    @Test fun redAndTealTakeTheHandheldsLightTextTheRestBlack() {
        assertEquals(0xDCDCDC, AccentPreset.RED.onRgb)
        assertEquals(0xFFDCDCDC.toInt(), AccentPreset.RED.onArgb)
        assertEquals(0xDCDCDC, AccentPreset.TEAL.onRgb)
        assertEquals("006666", AccentPreset.TEAL.hex)
        for (p in AccentPreset.entries - AccentPreset.RED - AccentPreset.TEAL) assertEquals(0x000000, p.onRgb, p.label)
    }

    @Test fun everyIdParsesBackToItsPreset() {
        for (p in AccentPreset.entries) assertEquals(p, AccentPreset.fromId(p.id))
    }

    @Test fun missingOrUnknownIdsFallBackToWhite() {
        assertEquals(AccentPreset.WHITE, AccentPreset.fromId(null))
        assertEquals(AccentPreset.WHITE, AccentPreset.fromId(""))
        assertEquals(AccentPreset.WHITE, AccentPreset.fromId("nx-red"))   // a retired or foreign value
        assertEquals(AccentPreset.WHITE, AccentPreset.fromId("RED"))      // ids are lower case, as stored
    }

    @Test fun stepWrapsBothWays() {
        assertEquals(AccentPreset.RED, AccentPreset.WHITE.step(1))
        assertEquals(AccentPreset.PINK, AccentPreset.WHITE.step(-1))
        assertEquals(AccentPreset.WHITE, AccentPreset.PINK.step(1))
    }
}
