package com.nxredux.mobile.ui.nx

import androidx.compose.ui.graphics.Color
import com.nxredux.mobile.settings.AccentPreset
import com.nxredux.mobile.settings.SecondaryAccent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A selected options row's label rect and label text (NX Redux UI_renderSettingsRow: accentPill() under
 * UI_getListTextColor(1), the accent's own text): the Settings value pages and the in-game menu follow the accent as the
 * main menu's pill does. The bar behind the value and the value on it follow the Secondary accent (default Teal, NX teal
 * under light text).
 */
class OptionsRowAccentTest {
    private val teal = Color(0xFF006666)
    private val light = Color(0xFFDCDCDC)

    @Test fun aSelectedOptionsRowTakesTheAccentAndItsText() {
        val red = nxColorsFor(AccentPreset.RED)
        assertEquals(NxOptionsSelection(Color(0xFF660000), Color(0xFFDCDCDC), teal, light), nxOptionsSelection(red))
        val sky = nxColorsFor(AccentPreset.SKY)
        assertEquals(NxOptionsSelection(Color(0xFF38BDF8), Color.Black, teal, light), nxOptionsSelection(sky))
    }

    @Test fun theBarTakesTheSecondaryAccentAndItsText() {
        val lime = nxColorsFor(AccentPreset.SKY, SecondaryAccent.Preset(AccentPreset.LIME))
        val s = nxOptionsSelection(lime)
        assertEquals(Color(0xFFA3E635) to Color.Black, s.bar to s.barText)
        val red = nxOptionsSelection(nxColorsFor(AccentPreset.SKY, SecondaryAccent.Preset(AccentPreset.RED)))
        assertEquals(Color(0xFF660000) to Color(0xFFDCDCDC), red.bar to red.barText)
        val auto = nxOptionsSelection(nxColorsFor(AccentPreset.SKY, SecondaryAccent.Auto))
        assertEquals(Color(0xFF195570) to Color(0xFFDCDCDC), auto.bar to auto.barText)
        val autoOnWhite = nxOptionsSelection(nxColorsFor(AccentPreset.WHITE, SecondaryAccent.Auto))
        assertEquals(teal to Color(0xFFDCDCDC), autoOnWhite.bar to autoOnWhite.barText)
    }

    @Test fun theInGameMenuTakesTheAccentAndTheSecondaryAccentLikeSettings() {
        // GameActivity builds the in-game colours from both settings; its options rows and adjust strip resolve through here.
        val red = nxColorsFor(AccentPreset.RED, SecondaryAccent.Preset(AccentPreset.LIME))
        assertEquals(NxOptionsSelection(Color(0xFF660000), light, Color(0xFFA3E635), Color.Black), nxOptionsSelection(red))
    }
}
