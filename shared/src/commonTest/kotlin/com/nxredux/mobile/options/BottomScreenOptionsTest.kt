package com.nxredux.mobile.options

import com.nxredux.mobile.runner.BottomScreen
import com.nxredux.mobile.runner.CoreRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomScreenOptionsTest {
    @Test fun the_row_follows_the_spec() {
        val row = BottomScreenOptions.row
        assertEquals("nx_bottom_screen", row.key)
        assertEquals("Bottom screen", row.label)
        assertEquals(listOf("default", "main", "second"), row.values)
        assertEquals(listOf("System default", "Main screen", "Second screen"), row.valueLabels)
        assertEquals("default", row.default)
        assertEquals(false, row.atLaunch)   // changeable mid-game
        assertEquals("Main screen", row.labelOf("main"))
    }

    @Test fun every_console_offers_it() {
        for (tag in CoreRegistry.tags) assertTrue(BottomScreenOptions.row in ConsoleSettings.tableFor(tag), tag)
        assertEquals("default", FrontendOptions.defaults("GBA")[BottomScreenOptions.KEY])
        assertEquals(BottomScreenOptions.row, FrontendOptions.option(BottomScreenOptions.KEY))
    }

    @Test fun the_override_beats_the_system_setting_and_default_follows_it() {
        // The row's values are the BottomScreen vocabulary, so resolve reads them as stored.
        assertEquals(BottomScreen.MAIN, BottomScreen.resolve("main", BottomScreen.SECOND))
        assertEquals(BottomScreen.SECOND, BottomScreen.resolve("second", BottomScreen.AUTO))
        assertEquals(BottomScreen.MAIN, BottomScreen.resolve(BottomScreenOptions.DEFAULT, BottomScreen.MAIN))
        assertEquals(BottomScreen.AUTO, BottomScreen.resolve(null, BottomScreen.AUTO))
        assertEquals(BottomScreen.SECOND, BottomScreen.resolve("junk", BottomScreen.SECOND))
        assertEquals(listOf(BottomScreen.MAIN, BottomScreen.SECOND), BottomScreenOptions.row.values.drop(1).map(BottomScreen::parse))
    }
}
