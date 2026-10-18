package com.nxredux.mobile.ui

import com.nxredux.mobile.settings.MenuCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainMenuTitleTest {
    private fun tab(c: MenuCategory) = MenuTab(c, emptyList())

    @Test fun twoTabsOrMoreDrawTheTabRow() {
        assertNull(mainMenuTitle(Screen.Systems(listOf(tab(MenuCategory.HOME), tab(MenuCategory.TOOLS)))))
    }

    @Test fun aSingleTabIsTitledWithTheAppsName() {
        assertEquals("NX Redux", mainMenuTitle(Screen.Systems(listOf(tab(MenuCategory.TOOLS)))))
        assertEquals("NX Redux", mainMenuTitle(Screen.Systems(listOf(tab(MenuCategory.HOME)))))
    }

    @Test fun aPushedTabIsTitledWithItsLabel() {
        assertEquals("Consoles", mainMenuTitle(Screen.Systems(listOf(tab(MenuCategory.CONSOLES)), pushed = true)))
    }
}
