package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.runner.SegaPad
import kotlin.test.Test
import kotlin.test.assertEquals

class HintGlyphsTest {
    private fun g(tag: String, b: NxButton, sega: SegaPad? = null) = HintGlyphs.forPad(PadProfile.forTag(tag, sega)).glyphFor(b)

    @Test fun standard_is_the_art_for_everything() {
        for (b in NxButton.entries) assertEquals(HintGlyph.Art(b), HintGlyphs.STANDARD.glyphFor(b))
    }

    @Test fun plain_letter_consoles_keep_the_art() {
        for (tag in listOf("SFC", "FBN", ""))
            for (b in listOf(NxButton.A, NxButton.B, NxButton.X, NxButton.Y, NxButton.START)) assertEquals(HintGlyph.Art(b), g(tag, b), "$tag $b")
    }

    @Test fun turbo_labels_show_as_badges() {
        // GB/GBC/GBA draw X and Y as turbo A and B ("TA", "TB"), so the menu X/Y hints show those; A/B keep the art.
        assertEquals(HintGlyph.Badge("TA"), g("GBA", NxButton.X)); assertEquals(HintGlyph.Badge("TB"), g("GBA", NxButton.Y))
        assertEquals(HintGlyph.Art(NxButton.A), g("GBA", NxButton.A)); assertEquals(HintGlyph.Art(NxButton.B), g("GBA", NxButton.B))
    }

    @Test fun sega_hints_show_the_drawn_letters() {
        assertEquals(HintGlyph.Art(NxButton.A), g("MD", NxButton.A))      // drawn A (Y position) confirms: letter matches
        assertEquals(HintGlyph.Art(NxButton.B), g("MD", NxButton.B))      // drawn B (X position) goes back
        assertEquals(HintGlyph.Badge("C"), g("MD", NxButton.X))           // menu X is the drawn C
        assertEquals(HintGlyph.Art(NxButton.X), g("MD", NxButton.Y, SegaPad.SIX))   // menu Y is the drawn X
        assertEquals(HintGlyph.TextOnly, g("MD", NxButton.Y, SegaPad.THREE))        // drawn X hidden in 3-button: the text alone
        assertEquals(HintGlyph.Badge("Mode"), g("MD", NxButton.SELECT, SegaPad.SIX))
    }

    @Test fun ps_and_psp_hints_show_the_playstation_art() {
        for (tag in listOf("PS", "PSP")) {
            // Menu A (select) is the drawn ×, menu B (back) the drawn ○.
            assertEquals(HintGlyph.Icon(PadIcon.PS_CROSS), g(tag, NxButton.A), tag); assertEquals(HintGlyph.Icon(PadIcon.PS_CIRCLE), g(tag, NxButton.B), tag)
            assertEquals(HintGlyph.Icon(PadIcon.PS_TRIANGLE), g(tag, NxButton.X), tag); assertEquals(HintGlyph.Icon(PadIcon.PS_SQUARE), g(tag, NxButton.Y), tag)
            assertEquals(HintGlyph.Art(NxButton.START), g(tag, NxButton.START), tag)
        }
    }

    @Test fun dc_and_ngp_hints_follow_their_drawn_letters() {
        for (tag in listOf("DC", "NGP")) { assertEquals(HintGlyph.Art(NxButton.A), g(tag, NxButton.A)); assertEquals(HintGlyph.Art(NxButton.B), g(tag, NxButton.B)) }
    }

    @Test fun sms_hints_show_one_and_two() {
        assertEquals(HintGlyph.Badge("2"), g("SMS", NxButton.A)); assertEquals(HintGlyph.Badge("1"), g("SMS", NxButton.B))
    }

    @Test fun n64_hides_only_select() {
        assertEquals(HintGlyph.TextOnly, g("N64", NxButton.SELECT))   // not drawn: the text alone
        assertEquals(HintGlyph.Art(NxButton.R2), g("N64", NxButton.R2))   // drawn like every console's R2
    }

    @Test fun directions_and_menu_are_always_art() {
        for (b in listOf(NxButton.UP, NxButton.DOWN, NxButton.LEFT, NxButton.RIGHT, NxButton.MENU)) assertEquals(HintGlyph.Art(b), g("MD", b))
    }
}
