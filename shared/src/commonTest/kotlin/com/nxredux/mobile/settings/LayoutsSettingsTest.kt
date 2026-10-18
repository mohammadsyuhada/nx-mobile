package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutsSettingsTest {
    @Test fun console_art_reads_its_stored_name_else_the_default() {
        assertEquals(ConsoleArt.BACKGROUND, ConsoleArt.fromStored("BACKGROUND"))
        assertEquals(ConsoleArt.NONE, ConsoleArt.fromStored("NONE"))
        assertEquals(ConsoleArt.CONTROLLER, ConsoleArt.fromStored(null))
        assertEquals(ConsoleArt.CONTROLLER, ConsoleArt.fromStored("GARBAGE"))
    }

    @Test fun labels_in_cycle_order_and_steps_wrap() {
        assertEquals(listOf("Controller", "Background", "None"), ConsoleArt.entries.map { it.label })
        assertEquals(listOf("Screenshot", "Mix", "3D box art", "2D box art", "Wheel", "None"), GameListArt.entries.map { it.label })
        assertEquals(listOf("3D box art", "2D box art", "Wheel"), BackdropArt.entries.map { it.label })
        assertEquals(ConsoleArt.NONE, ConsoleArt.CONTROLLER.step(-1))
        assertEquals(GameListArt.SCREENSHOT, GameListArt.NONE.step(1))
        assertEquals(BackdropArt.BOX3D, BackdropArt.WHEEL.step(1))
    }

    @Test fun defaults_and_stored_names() {
        assertEquals(ConsoleArt.CONTROLLER, ConsoleArt.DEFAULT)
        assertEquals(GameListArt.SCREENSHOT, GameListArt.DEFAULT)
        assertEquals(BackdropArt.BOX3D, BackdropArt.DEFAULT)
        assertTrue(MenuStyles.DEFAULT_TAB_SHOWN)
        assertEquals(GameListArt.WHEEL, GameListArt.fromStored("WHEEL"))
        assertEquals(GameListArt.SCREENSHOT, GameListArt.fromStored(null))
        assertEquals(BackdropArt.BOX3D, BackdropArt.fromStored("nope"))
    }

    @Test fun boxed_list_art_is_every_picture_but_the_screenshot_and_none() {
        assertEquals(listOf(GameListArt.MIX, GameListArt.BOX3D, GameListArt.BOX2D, GameListArt.WHEEL), GameListArt.entries.filter { it.boxed })
        assertFalse(GameListArt.SCREENSHOT.boxed)
    }
}
