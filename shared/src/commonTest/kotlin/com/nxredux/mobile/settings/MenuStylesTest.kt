package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class MenuStylesTest {
    @Test fun aMissingTabIsItsDefaultAndAStoredOneWins() {
        for (c in listOf(MenuCategory.CONSOLES, MenuCategory.COLLECTIONS))
            assertEquals(MenuStyle.CAROUSEL, MenuStyles.styleOf(emptyMap(), c))
        assertEquals(MenuStyle.GRID, MenuStyles.styleOf(emptyMap(), MenuCategory.TOOLS))   // the Tools tab's own default
        assertEquals(MenuStyle.GRID, MenuStyles.styleOf(mapOf(MenuCategory.CONSOLES to MenuStyle.GRID), MenuCategory.CONSOLES))
        assertEquals(MenuStyle.LIST, MenuStyles.styleOf(mapOf(MenuCategory.TOOLS to MenuStyle.LIST), MenuCategory.TOOLS))
    }

    @Test fun aStoredStyleATabDoesNotOfferReadsAsNoneStored() {
        // Backdrop is a game-list style: a tab line naming it is dropped, so the tab takes its default.
        assertEquals(mapOf(MenuCategory.TOOLS to MenuStyle.LIST), MenuStyles.decode("HOME=BACKDROP\nTOOLS=LIST"))
        assertEquals(MenuStyle.GRID, MenuStyles.styleOf(MenuStyles.decode("HOME=BACKDROP"), MenuCategory.HOME))
    }

    @Test fun mainMenuStylesCycleListGridCarousel() {
        assertEquals(MenuStyle.LIST, MenuStyles.cycleMainMenu(MenuStyle.CAROUSEL, 1))
        assertEquals(MenuStyle.CAROUSEL, MenuStyles.cycleMainMenu(MenuStyle.LIST, -1))
        assertEquals(MenuStyle.CAROUSEL, MenuStyles.cycleMainMenu(MenuStyle.GRID, 1))
    }

    @Test fun encodeWritesOneLinePerCategoryInCategoryOrder() {
        val styles = mapOf(MenuCategory.TOOLS to MenuStyle.LIST, MenuCategory.CONSOLES to MenuStyle.CAROUSEL)
        assertEquals("CONSOLES=CAROUSEL\nTOOLS=LIST", MenuStyles.encode(styles))
    }

    @Test fun decodeReadsWhatEncodeWrote() {
        val styles = mapOf(MenuCategory.COLLECTIONS to MenuStyle.CAROUSEL, MenuCategory.CONSOLES to MenuStyle.GRID)
        assertEquals(styles, MenuStyles.decode(MenuStyles.encode(styles)))
    }

    @Test fun decodeSkipsUnknownAndMalformedLines() {
        assertEquals(
            mapOf(MenuCategory.CONSOLES to MenuStyle.GRID),
            MenuStyles.decode("CONSOLES=GRID\nNOPE=LIST\nPINNED=WHEEL\ngarbage\n\n"),
        )
        assertEquals(emptyMap(), MenuStyles.decode(null))
    }

    @Test fun storedRecentAndPinnedStylesFromBeforeHomeAreDropped() {
        // Written when Recent and Pinned were tabs: those lines no longer name a category, the rest still reads.
        assertEquals(
            mapOf(MenuCategory.TOOLS to MenuStyle.GRID),
            MenuStyles.decode("RECENT=CAROUSEL\nPINNED=BACKDROP\nTOOLS=GRID"),
        )
    }

    @Test fun orientationDefaultsToHorizontalAndAStoredOneWins() {
        assertEquals(MenuOrientation.HORIZONTAL, MenuStyles.orientationOf(emptyMap(), MenuCategory.CONSOLES))
        assertEquals(
            MenuOrientation.VERTICAL,
            MenuStyles.orientationOf(mapOf(MenuCategory.TOOLS to MenuOrientation.VERTICAL), MenuCategory.TOOLS),
        )
    }

    @Test fun orientationAppliesOnlyToCarouselAndBackdrop() {
        assertEquals(MenuOrientation.VERTICAL, MenuStyles.effectiveOrientation(MenuStyle.CAROUSEL, MenuOrientation.VERTICAL))
        assertEquals(MenuOrientation.VERTICAL, MenuStyles.effectiveOrientation(MenuStyle.BACKDROP, MenuOrientation.VERTICAL))
        assertEquals(MenuOrientation.HORIZONTAL, MenuStyles.effectiveOrientation(MenuStyle.GRID, MenuOrientation.VERTICAL))
        assertEquals(MenuOrientation.HORIZONTAL, MenuStyles.effectiveOrientation(MenuStyle.LIST, MenuOrientation.VERTICAL))
        assertEquals(listOf(false, false, true, true), MenuStyle.entries.map(MenuStyles::hasOrientation))
    }

    @Test fun orientationsRoundTripAndIgnoreJunk() {
        val m = mapOf(MenuCategory.TOOLS to MenuOrientation.VERTICAL, MenuCategory.CONSOLES to MenuOrientation.HORIZONTAL)
        assertEquals("CONSOLES=HORIZONTAL\nTOOLS=VERTICAL", MenuStyles.encodeOrientations(m))
        assertEquals(m, MenuStyles.decodeOrientations(MenuStyles.encodeOrientations(m)))
        assertEquals(
            m,
            MenuStyles.decodeOrientations("CONSOLES=HORIZONTAL\nTOOLS=VERTICAL\nRECENT=VERTICAL\nCOLLECTIONS=SIDEWAYS\nHOME=VERTICAL\njunk"),
        )
        assertEquals(emptyMap(), MenuStyles.decodeOrientations(null))
    }

    @Test fun flipToggles() {
        assertEquals(MenuOrientation.VERTICAL, MenuStyles.flip(MenuOrientation.HORIZONTAL))
        assertEquals(MenuOrientation.HORIZONTAL, MenuStyles.flip(MenuOrientation.VERTICAL))
    }

    @Test fun home_defaults_to_grid_and_cycles_list_grid_carousel() {
        assertEquals(MenuStyle.GRID, MenuStyles.styleOf(emptyMap(), MenuCategory.HOME))
        assertEquals(MenuStyle.CAROUSEL, MenuStyles.cycleMainMenu(MenuStyle.GRID, 1))
        assertEquals(MenuStyle.LIST, MenuStyles.cycleMainMenu(MenuStyle.GRID, -1))
        assertEquals(mapOf(MenuCategory.HOME to MenuStyle.LIST), MenuStyles.decode(MenuStyles.encode(mapOf(MenuCategory.HOME to MenuStyle.LIST))))
    }
}
