package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A touch-only screen ([FooterMode.BUTTONS]): no controller, no on-screen pad, no hint bar. */
@Composable
fun touchOnly(): Boolean = LocalFooterMode.current == FooterMode.BUTTONS

/** An empty state's hints as buttons on touch: all but B Back (Android's back), A filled. */
fun emptyStateButtons(hints: List<Hint>): List<FooterAction> =
    hints.filterNot { it.button == NxButton.B && it.label == "Back" }.map { FooterAction(it.button, it.label, primary = it.button == NxButton.A) }

/** [actions] as the dialog footer's pill buttons ([NxModalButton]), 8 dp apart; a tap sends the action's button. */
@Composable
fun NxTouchButtons(
    actions: List<FooterAction>, onHint: (NxButton) -> Unit, modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    if (actions.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, alignment), verticalAlignment = Alignment.CenterVertically) {
        actions.forEach { a -> NxModalButton(a) { onHint(a.button) } }
    }
}
