package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class ToolPinsTest {
    @Test fun round_trips_in_pin_order() {
        val keys = listOf("SETTINGS", "GAME_TIME", "CHEAT_DB")
        assertEquals("SETTINGS\nGAME_TIME\nCHEAT_DB", ToolPins.encode(keys))
        assertEquals(keys, ToolPins.decode(ToolPins.encode(keys)))
        assertEquals(emptyList(), ToolPins.decode(null))
        assertEquals(emptyList(), ToolPins.decode(ToolPins.encode(emptyList())))
    }

    @Test fun an_unknown_name_is_dropped() {
        assertEquals(listOf("ARTWORK", "SETTINGS"), ToolPins.decode("ARTWORK\nNOT_A_TOOL\r\n\n  SETTINGS  \nsettings\n"))
    }

    @Test fun duplicates_are_removed() {
        assertEquals(listOf("GAME_TIME", "ARTWORK"), ToolPins.decode("GAME_TIME\nARTWORK\nGAME_TIME\n"))
        assertEquals("GAME_TIME\nARTWORK", ToolPins.encode(listOf("GAME_TIME", "ARTWORK", "GAME_TIME")))
    }

    @Test fun add_is_idempotent_and_remove_drops_the_key() {
        val one = ToolPins.add(emptyList(), "CHEAT_DB")
        assertEquals(listOf("CHEAT_DB"), one)
        assertEquals(one, ToolPins.add(one, "CHEAT_DB"))
        assertEquals(listOf("CHEAT_DB", "SETTINGS"), ToolPins.add(one, "SETTINGS"))
        assertEquals(listOf("SETTINGS"), ToolPins.remove(listOf("CHEAT_DB", "SETTINGS"), "CHEAT_DB"))
        assertEquals(one, ToolPins.remove(one, "ARTWORK"))
    }

    @Test fun an_app_pin_keeps_its_id_and_a_bare_app_is_dropped() {
        assertEquals(listOf("SETTINGS", "APP:a/A"), ToolPins.decode("SETTINGS\nAPP:a/A\nAPP\nAPP:\nAPP:a/A\n"))
    }
}
