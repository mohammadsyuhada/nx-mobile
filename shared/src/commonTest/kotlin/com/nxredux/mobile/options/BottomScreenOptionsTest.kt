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
        assertEquals(BottomScreen.MAIN, BottomScreenOptions.resolve(mapOf(BottomScreenOptions.KEY to "main"), "second"))
        assertEquals(BottomScreen.SECOND, BottomScreenOptions.resolve(mapOf(BottomScreenOptions.KEY to "second"), "auto"))
        assertEquals(BottomScreen.MAIN, BottomScreenOptions.resolve(mapOf(BottomScreenOptions.KEY to "default"), "main"))
        assertEquals(BottomScreen.AUTO, BottomScreenOptions.resolve(emptyMap(), "auto"))
        assertEquals(BottomScreen.SECOND, BottomScreenOptions.resolve(mapOf(BottomScreenOptions.KEY to "junk"), "second"))
    }
}
