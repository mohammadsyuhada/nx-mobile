package com.nxredux.mobile.ui

import com.nxredux.mobile.settings.MenuCategory
import kotlin.test.Test
import kotlin.test.assertEquals

/** The main menu List band's shade and info per tab ([mainMenuListBand]). */
class MainMenuListBandTest {
    @Test fun consolesCollectionsAndToolsHaveNeitherShadeNorInfo() {
        for (c in listOf(MenuCategory.CONSOLES, MenuCategory.COLLECTIONS, MenuCategory.TOOLS))
            assertEquals(MainMenuListBand(shade = false, info = false), mainMenuListBand(c), "$c")
    }

    @Test fun homeAndATablessListKeepTheirShadeAndInfo() {
        for (c in listOf(MenuCategory.HOME, null))
            assertEquals(MainMenuListBand(shade = true, info = true), mainMenuListBand(c), "$c")
    }
}
