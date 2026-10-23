package com.nxredux.mobile.settings

import kotlin.math.roundToInt

/**
 * The selection colour (Appearance -> Accent): every selection mark (list pills, lit tiles, lit tool squares,
 * game rings, the tab underline and focus plate) draws in it, and the text on it is the preset's [onRgb]
 * ([com.nxredux.mobile.ui.nx.NxColors.onAccent]): black, except Red's and Teal's light text. Tailwind CSS palette colours
 * (Red and Teal are the NX Redux handheld's), in the picker's order; [WHITE] is the default. [id] is what is stored; [rgb] and [onRgb] are 0xRRGGBB.
 */
enum class AccentPreset(val id: String, val label: String, val rgb: Int, val onRgb: Int = 0x000000) {
    WHITE("white", "White", 0xE4E4E7),     // zinc-200
    // Not a Tailwind token: the NX Redux handheld's theme (TrimUI Smart Pro S), its #660000 pill with #DCDCDC text.
    RED("red", "Red", 0x660000, onRgb = 0xDCDCDC),
    ORANGE("orange", "Orange", 0xF97316),  // orange-500
    AMBER("amber", "Amber", 0xFBBF24),     // amber-400
    LIME("lime", "Lime", 0xA3E635),        // lime-400
    // Not a Tailwind token: NX teal, NX Redux's THEME_COLOR2 (CFG_DEFAULT_COLOR2 #006666), with Red's light text.
    TEAL("teal", "Teal", 0x006666, onRgb = 0xDCDCDC),
    SKY("sky", "Sky", 0x38BDF8),           // sky-400
    VIOLET("violet", "Violet", 0x8B5CF6),  // violet-500
    PINK("pink", "Pink", 0xEC4899);        // pink-500

    /** 0xFFRRGGBB. */
    val argb: Int get() = (0xFF shl 24) or rgb

    /** [onRgb] as 0xFFRRGGBB: the text on this accent. */
    val onArgb: Int get() = (0xFF shl 24) or onRgb

    /** [rgb] as six lower-case hex digits, no '#'. */
    val hex: String get() = rgb.toString(16).padStart(6, '0')

    /** The preset [step] places after this one, wrapping (Settings' LEFT is -1, RIGHT and A +1). */
    fun step(step: Int): AccentPreset = entries.stepFrom(this, step)

    companion object {
        val DEFAULT = WHITE

        /** The preset stored as [id]; missing or unknown: [DEFAULT]. */
        fun fromId(id: String?): AccentPreset = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * The bar behind a selected option's value (Appearance -> Secondary accent), on the Settings value pages and the Game
 * Settings modal; the in-game menu keeps NX teal. [Auto] darkens the accent; [Preset] borrows an [AccentPreset]'s colour
 * and text, [AccentPreset.TEAL] (NX teal) the default. [entries] is the picker's order; [id] is what is stored. [rgb] and
 * [onRgb] (the value's text on the bar) are 0xRRGGBB for the current accent.
 */
sealed class SecondaryAccent(val id: String, val label: String) {
    abstract fun rgb(accent: AccentPreset): Int
    abstract fun onRgb(accent: AccentPreset): Int

    /** The accent at [AUTO_BRIGHTNESS] (each channel, rounded to the nearest), always dark so light text; White gives Teal. */
    data object Auto : SecondaryAccent("auto", "Auto") {
        override fun rgb(accent: AccentPreset): Int =
            if (accent == AccentPreset.WHITE) AccentPreset.TEAL.rgb
            else (0..2).fold(0) { acc, i -> acc or (((accent.rgb shr (8 * i)) and 0xFF) * AUTO_BRIGHTNESS).roundToInt().shl(8 * i) }
        override fun onRgb(accent: AccentPreset): Int = 0xDCDCDC
    }

    /** [preset]'s own colour and text, whatever the accent. */
    data class Preset(val preset: AccentPreset) : SecondaryAccent(preset.id, preset.label) {
        override fun rgb(accent: AccentPreset): Int = preset.rgb
        override fun onRgb(accent: AccentPreset): Int = preset.onRgb
    }

    /** The value [step] places after this one in [entries], wrapping (Settings' LEFT is -1, RIGHT and A +1). */
    fun step(step: Int): SecondaryAccent = entries.stepFrom(this, step)

    companion object {
        private const val AUTO_BRIGHTNESS = 0.45

        /** Auto, then the [AccentPreset]s in their order. */
        val entries: List<SecondaryAccent> by lazy { listOf(Auto) + AccentPreset.entries.map(::Preset) }

        val DEFAULT: SecondaryAccent get() = Preset(AccentPreset.TEAL)

        /** The value stored as [id]; [DEFAULT] when missing or unknown. */
        fun fromId(id: String?): SecondaryAccent = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
