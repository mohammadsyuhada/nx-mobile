package com.nxredux.mobile.ui

import com.nxredux.mobile.settings.MenuCategory
import kotlin.test.Test
import kotlin.test.assertEquals

/** The main menu List band's shade and info per tab ([mainMenuListBand]). */
class MainMenuListBandTest {
    @Test fun consolesHasNeitherShadeNorInfo() {
        assertEquals(MainMenuListBand(shade = false, info = false), mainMenuListBand(MenuCategory.CONSOLES))
    }

    @Test fun everyOtherTabKeepsItsShadeAndInfo() {
        for (c in listOf(MenuCategory.HOME, MenuCategory.COLLECTIONS, MenuCategory.TOOLS, null))
            assertEquals(MainMenuListBand(shade = true, info = true), mainMenuListBand(c), "$c")
    }
}
