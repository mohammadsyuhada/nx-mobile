package com.nxredux.mobile.ui

import com.nxredux.mobile.settings.MenuOrientation
import com.nxredux.mobile.settings.MenuStyle
import com.nxredux.mobile.settings.VerticalListAlignment
import kotlin.test.Test
import kotlin.test.assertEquals

class GameListLayoutsTest {
    private val L = VerticalListAlignment.LEFT
    private val R = VerticalListAlignment.RIGHT

    @Test fun eightChoicesEachVerticalOncePerAlignment() {
        assertEquals(
            listOf("List", "Grid", "Carousel", "Carousel, vertical left", "Carousel, vertical right", "Backdrop", "Backdrop, vertical left", "Backdrop, vertical right"),
            GameListLayouts.all.map { it.label },
        )
    }

    @Test fun theChoiceInUseIgnoresWhatItsStyleDoesNotUse() {
        assertEquals(0, GameListLayouts.indexOf(MenuStyle.LIST, MenuOrientation.VERTICAL, R))
        assertEquals(1, GameListLayouts.indexOf(MenuStyle.GRID, MenuOrientation.VERTICAL, R))
        assertEquals(2, GameListLayouts.indexOf(MenuStyle.CAROUSEL, MenuOrientation.HORIZONTAL, R))   // a row has no side
        assertEquals(3, GameListLayouts.indexOf(MenuStyle.CAROUSEL, MenuOrientation.VERTICAL, L))
        assertEquals(4, GameListLayouts.indexOf(MenuStyle.CAROUSEL, MenuOrientation.VERTICAL, R))
        assertEquals(5, GameListLayouts.indexOf(MenuStyle.BACKDROP, MenuOrientation.HORIZONTAL, L))
        assertEquals(7, GameListLayouts.indexOf(MenuStyle.BACKDROP, MenuOrientation.VERTICAL, R))
    }
}
