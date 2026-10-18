package com.nxredux.mobile.ui.nx

import androidx.compose.ui.graphics.Color
import com.nxredux.mobile.settings.AccentPreset
import kotlin.test.Test
import kotlin.test.assertEquals

/** Text on the accent is the preset's own: black, but Red's and Teal's light #DCDCDC; the default is the #E4E4E7 pill. */
class AccentContrastTest {
    @Test fun theUiWhiteIsTheD8InkAndTheAccentOneStepBrighter() {
        val c = NxColors()
        assertEquals(Color(0xFFD8D8D8), c.main)
        assertEquals(Color(0xFFD8D8D8), c.listText)
        assertEquals(Color(0xFFE4E4E7), c.accent)   // White, Tailwind zinc-200
        assertEquals(Color.Black, c.onAccent)
        assertEquals(Color.Black, c.listTextSelected)
        assertEquals(Color(0xFF000000), c.background)
    }

    @Test fun theDefaultAccentKeepsBlackTextAndItsMutedShade() {
        val c = NxColors()
        assertEquals(c.secondarySelected, c.onAccentMuted)          // 55% black, as before
        assertEquals(Color(0xFF006666), c.optionBar)               // the renamed teal
    }

    @Test fun aCopiedAccentKeepsBlackText() {
        val red = NxColors().copy(accent = Color(0xFFE60012))
        assertEquals(Color.Black, red.onAccent)
        assertEquals(Color.Black.copy(alpha = 0.55f), red.onAccentMuted)
    }

    @Test fun redGetsTheHandheldsLightText() {
        val c = nxColorsFor(AccentPreset.RED)
        assertEquals(Color(0xFF660000), c.accent)
        assertEquals(Color(0xFFDCDCDC), c.onAccent)
        assertEquals(Color(0xFFDCDCDC).copy(alpha = 0.55f), c.onAccentMuted)
        assertEquals(c.success, c.successOnAccent)   // the plain tones read on the dark red
        assertEquals(c.warning, c.warningOnAccent)
    }

    @Test fun tealIsNxTealWithTheHandheldsLightText() {
        val c = nxColorsFor(AccentPreset.TEAL)
        assertEquals(Color(0xFF006666), c.accent)
        assertEquals(c.optionBar, c.accent)            // the in-game bar's teal
        assertEquals(Color(0xFFDCDCDC), c.onAccent)
        assertEquals(c.success, c.successOnAccent)
        assertEquals(c.warning, c.warningOnAccent)
    }

    @Test fun everyOtherPresetGetsBlackText() {
        for (p in AccentPreset.entries - AccentPreset.RED - AccentPreset.TEAL) {
            val c = nxColorsFor(p)
            assertEquals(Color(p.argb), c.accent)
            assertEquals(Color.Black, c.onAccent, p.label)
            assertEquals(Color.Black.copy(alpha = 0.55f), c.onAccentMuted, p.label)
        }
    }

    @Test fun toneColoursOnABlackTextAccentAreTheDarkOnes() {
        for (p in AccentPreset.entries - AccentPreset.RED - AccentPreset.TEAL) {
            val c = nxColorsFor(p)
            assertEquals(c.successSelected, c.successOnAccent, p.label)
            assertEquals(c.warningSelected, c.warningOnAccent, p.label)
        }
    }
}
