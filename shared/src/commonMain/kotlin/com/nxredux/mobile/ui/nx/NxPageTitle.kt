package com.nxredux.mobile.ui.nx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A titled page's grey title: the game list's [MenuTitleRow] (at the tab words' size, [MENU_TAB_ROW_HEIGHT] high), so
 * every tool and settings page title reads exactly like a console's game list title. Its first letter sits [textStart]
 * in, where the page's text below starts (the list labels' inset, [nxListInset], on every list and detail page).
 * Shared by the tool-page bodies and [NxScreenScaffold]. [bottom]: extra room under the row before the page's content
 * (a caption sits right under it, a headline a little lower). [suffix]: a part that is never cut ([MenuTitleRow]).
 * [trailing]: icons at the title's right (a touch-only page's [HintTouch.Icon]s).
 */
@Composable
internal fun NxPageTitle(text: String, bottom: Dp = 0.dp, suffix: String? = null, textStart: Dp = nxListInset(), trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth()) {
        MenuTitleRow(text, textStart = textStart, suffix = suffix, trailing = trailing)
        if (bottom > 0.dp) Spacer(Modifier.height(bottom))
    }
}
