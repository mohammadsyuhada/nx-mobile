package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals

class MapTxtTest {
    @Test fun parses_tab_separated_lines_and_ignores_the_rest() {
        val text = "Mario Kart DS.nds\tMario Kart DS (USA)\r\n" +
            "no tab on this line\n" +
            "\n" +
            "Contra 4.nds\t Contra 4 \n"
        assertEquals(
            mapOf("Mario Kart DS.nds" to "Mario Kart DS (USA)", "Contra 4.nds" to "Contra 4"),
            MapTxt.parse(text),
        )
    }
    @Test fun empty_text_gives_empty_map() { assertEquals(emptyMap(), MapTxt.parse("")) }

    @Test fun with_upserts_a_key_and_renders_sorted_by_key() {
        assertEquals("Mario.gba\tSuper Mario\n", MapTxt.with(null, "Mario.gba", "Super Mario"))
        val two = MapTxt.with("Zelda.gba\tZelda\n", "Mario.gba", "Super Mario")
        assertEquals("Mario.gba\tSuper Mario\nZelda.gba\tZelda\n", two)
        assertEquals("Mario.gba\tMario 2\nZelda.gba\tZelda\n", MapTxt.with(two, "Mario.gba", "Mario 2"))
        assertEquals(mapOf("Mario.gba" to "Mario 2", "Zelda.gba" to "Zelda"), MapTxt.parse(MapTxt.with(two, "Mario.gba", "Mario 2")))
    }

    @Test fun with_refuses_tabs_and_newlines_and_without_drops_a_key() {
        assertEquals("a.gb\tA\n", MapTxt.with("a.gb\tA\n", "b\tc.gb", "B"))
        assertEquals("a.gb\tA\n", MapTxt.with("a.gb\tA\n", "b.gb", "line\nbreak"))
        assertEquals("", MapTxt.with(null, "", "Empty"))
        assertEquals("b.gb\tB\n", MapTxt.without("a.gb\tA\nb.gb\tB\n", "a.gb"))
        assertEquals("a.gb\tA\n", MapTxt.without("a.gb\tA\n", "zzz.gb"))
    }
}
