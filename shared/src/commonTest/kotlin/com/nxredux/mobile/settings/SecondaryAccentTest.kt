package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Secondary accent setting (the bar behind a selected option's value): Auto (the accent at 45 % brightness), then the
 * Accent presets in their order; stored by id, Teal (NX Redux's #006666, the default) when missing or unknown.
 */
class SecondaryAccentTest {
    @Test fun theDefaultIsTeal() {
        assertEquals(SecondaryAccent.Preset(AccentPreset.TEAL), SecondaryAccent.DEFAULT)
        assertEquals(0x006666, SecondaryAccent.DEFAULT.rgb(AccentPreset.DEFAULT))
        assertEquals(0xDCDCDC, SecondaryAccent.DEFAULT.onRgb(AccentPreset.DEFAULT))
    }

    @Test fun autoThenTheAccentPresetsInOrder() {
        assertEquals(
            listOf("Auto", "White", "Red", "Orange", "Amber", "Lime", "Teal", "Sky", "Violet", "Pink"),
            SecondaryAccent.entries.map { it.label },
        )
        for (p in AccentPreset.entries) assertEquals(p.rgb, SecondaryAccent.Preset(p).rgb(AccentPreset.SKY), p.label)
    }

    @Test fun everyIdParsesBackAndUnknownIsTeal() {
        val teal = SecondaryAccent.Preset(AccentPreset.TEAL)
        for (s in SecondaryAccent.entries) assertEquals(s, SecondaryAccent.fromId(s.id))
        assertEquals(teal, SecondaryAccent.fromId(null))
        assertEquals(teal, SecondaryAccent.fromId(""))
        assertEquals(teal, SecondaryAccent.fromId("green"))
    }

    @Test fun stepWrapsBothWays() {
        assertEquals(SecondaryAccent.Preset(AccentPreset.WHITE), SecondaryAccent.Auto.step(1))
        assertEquals(SecondaryAccent.Auto, SecondaryAccent.Preset(AccentPreset.PINK).step(1))
        assertEquals(SecondaryAccent.Preset(AccentPreset.PINK), SecondaryAccent.Auto.step(-1))
    }

    /** Each channel times 0.45, rounded to the nearest (half up). */
    @Test fun autoIsTheAccentAt45PercentWhiteGivesTeal() {
        assertEquals(0x2E0000, SecondaryAccent.Auto.rgb(AccentPreset.RED))     // 0x66 x 0.45 = 45.9 -> 46
        assertEquals(0x195570, SecondaryAccent.Auto.rgb(AccentPreset.SKY))     // (56, 189, 248) -> (25.2, 85.05, 111.6) -> (25, 85, 112)
        assertEquals(0x006666, SecondaryAccent.Auto.rgb(AccentPreset.WHITE))
    }

    @Test fun theTextOnTheBar() {
        assertEquals(0xDCDCDC, SecondaryAccent.Auto.onRgb(AccentPreset.SKY))     // always dark
        assertEquals(0xDCDCDC, SecondaryAccent.Auto.onRgb(AccentPreset.WHITE))
        for (p in AccentPreset.entries) assertEquals(p.onRgb, SecondaryAccent.Preset(p).onRgb(AccentPreset.SKY), p.label)
    }
}
