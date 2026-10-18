package com.nxredux.mobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MainMenuContentAlphaTest {
    @Test fun theWholeContentDimsWhileTheTabRowHasTheDpad() {
        assertEquals(TAB_FOCUS_CONTENT_ALPHA, mainMenuContentAlpha(tabFocus = true))
        assertEquals(0.4f, TAB_FOCUS_CONTENT_ALPHA)
    }

    @Test fun theContentIsFullyLitOnItsOwnFocus() {
        assertEquals(1f, mainMenuContentAlpha(tabFocus = false))
    }
}
