package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.compositionLocalOf

/** What a hint bar draws for a hint's button. */
sealed interface HintGlyph {
    /** The standard `nav_*` art for [button]. */
    data class Art(val button: NxButton) : HintGlyph

    /** A round badge with [text]: the on-screen pad's own label where it is not the standard letter (Sega C, PS ○). */
    data class Badge(val text: String) : HintGlyph

    /** Button art for a pad symbol ([PadIcon]): PlayStation ○ × △ □ as Kenney's PlayStation prompts. */
    data class Icon(val icon: PadIcon) : HintGlyph

    /**
     * The on-screen pad does not draw the button that performs the hint (Sega X in 3-button mode): the hint shows its
     * text alone, still tappable, so the action stays reachable by touch.
     */
    data object TextOnly : HintGlyph
}

/** How a hint bar shows each hint's button: the standard art, or the on-screen pad's label for the button that does it. */
fun interface HintGlyphs {
    fun glyphFor(button: NxButton): HintGlyph

    companion object {
        /** The standard A/B/X/Y art: library screens, and in game while a physical controller is connected. */
        val STANDARD: HintGlyphs = HintGlyphs { HintGlyph.Art(it) }

        private val PAD_POSITIONS = listOf(
            NxButton.A, NxButton.B, NxButton.X, NxButton.Y,
            NxButton.L1, NxButton.R1, NxButton.L2, NxButton.R2,
            NxButton.START, NxButton.SELECT,
        )

        /**
         * The on-screen pad's glyphs: a hint for menu button b shows the label of the pad position whose
         * [PadProfile.menuButton] is b (the button the player taps to do it), or [HintGlyph.TextOnly] when that position is not drawn.
         * A label that is a standard button's own glyph ("A", "X", "L1") uses that button's art (the Sega drawn X doing
         * menu Y shows the X art); a position drawn as a symbol ([PadProfile.icon]) shows that symbol's [HintGlyph.Icon];
         * any other label is a [HintGlyph.Badge].
         */
        fun forPad(profile: PadProfile): HintGlyphs = HintGlyphs { b ->
            val p = PAD_POSITIONS.firstOrNull { profile.menuButton(it) == b }
            when {
                p == null -> HintGlyph.Art(b)
                profile.isHidden(p) || profile.isHiddenInCluster(p) -> HintGlyph.TextOnly
                profile.icon(p) != null -> HintGlyph.Icon(profile.icon(p)!!)
                else -> profile.label(p).let { text ->
                    PAD_POSITIONS.firstOrNull { it.glyph() == text }?.let { HintGlyph.Art(it) } ?: HintGlyph.Badge(text)
                }
            }
        }
    }
}

/** The hint glyphs for the hint bars below; the in-game overlay provides the on-screen pad's while no controller is connected. */
val LocalHintGlyphs = compositionLocalOf { HintGlyphs.STANDARD }
