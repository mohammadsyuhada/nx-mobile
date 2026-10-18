package com.nxredux.mobile.ui.nx

import org.jetbrains.compose.resources.DrawableResource

/**
 * What stands in for a hint on a touch-only screen, where there is no hint bar. [None]: a tap, a long press
 * or Android's back already does it. [Icon]: an icon at the page title's right ([lit]: a toggle that is on). [Button]:
 * a button on the page, in the dialog footer's style ([primary]: filled).
 */
sealed interface HintTouch {
    data object None : HintTouch
    data class Icon(val art: DrawableResource, val lit: Boolean = false) : HintTouch
    data class Button(val primary: Boolean) : HintTouch
}

/** The [HintTouch.Icon] hints as title icons; a tap sends the hint's button to [onHint]. */
fun List<Hint>.touchIcons(onHint: (NxButton) -> Unit): List<TopIconAction> = mapNotNull { h ->
    (h.touch as? HintTouch.Icon)?.let { TopIconAction(it.art, h.label, it.lit) { onHint(h.button) } }
}

/** The [HintTouch.Button] hints as page buttons, in hint order. */
fun List<Hint>.touchButtons(): List<FooterAction> = mapNotNull { h ->
    (h.touch as? HintTouch.Button)?.let { FooterAction(h.button, h.label, it.primary) }
}
