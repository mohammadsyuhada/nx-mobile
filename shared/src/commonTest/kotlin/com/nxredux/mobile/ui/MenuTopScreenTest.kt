package com.nxredux.mobile.ui

import com.nxredux.mobile.library.GameEntry
import com.nxredux.mobile.library.SystemFolder
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.ui.nx.FooterMode
import com.nxredux.mobile.ui.nx.launcherChromeFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuTopScreenTest {
    @Test fun the_preview_hides_its_hint_bar_keeping_its_room() {
        val chrome = launcherChromeFor(pageTitle = true, buttonHints = true, mode = FooterMode.HINTS)
        val preview = previewChrome(chrome)
        assertTrue(preview.emptyBarRoom)
        assertFalse(preview.buttonHints)
        assertEquals(chrome.copy(buttonHints = false), preview)   // the title, info and the rest as the settings say
    }

    @Test fun a_touch_only_screen_has_no_bar_to_hide() {
        val chrome = launcherChromeFor(pageTitle = true, buttonHints = true, mode = FooterMode.BUTTONS)
        assertEquals(chrome, previewChrome(chrome))
        assertFalse(previewChrome(chrome).emptyBarRoom)
    }

    // Hosted only while the menu is on the second display; should it still draw meanwhile, never a list.
    @Test fun out_of_dual_mode_it_draws_the_backdrop_even_on_a_game_list() {
        val game = GameEntry(ContentRef("r/a.rom"), "a.rom", "A", folder = ContentRef("f"), sourceId = "home", relativeDir = "Roms/x")
        val system = SystemFolder("Console X", "X", null, listOf(game), null, null, "Console (X)", false)
        val source = ListSource.Console(listOf(system))
        val games = Screen.Games(source, listOf(Listed(system, game)))
        assertEquals(TopContent.Backdrop, menuTop(dual = false, games, null))
        assertEquals(TopContent.GameList, menuTop(dual = true, games, null))
    }
}
