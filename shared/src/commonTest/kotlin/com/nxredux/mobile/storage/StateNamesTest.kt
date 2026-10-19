package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StateNamesTest {
    private val auto = NxPaths.AUTO_SLOT

    @Test
    fun parses_retroarch() {
        assertEquals(StateName("Pokemon - Lazarus", 0), StateNames.parse("Pokemon - Lazarus.state"))
        assertEquals(3, StateNames.parse("X.state3")!!.slot)
        assertEquals(109, StateNames.parse("X.state9")!!.slot)
        assertEquals(auto, StateNames.parse("X.state.auto")!!.slot)
    }

    @Test
    fun parses_retroarch_ish_and_minui() {
        assertEquals(4, StateNames.parse("X.state.4")!!.slot)
        assertEquals(StateName("X", 2), StateNames.parse("X.gba.st2"))
        assertEquals(auto, StateNames.parse("X.gba.st9")!!.slot)
    }

    @Test
    fun parses_mixed_case() {
        assertEquals(1, StateNames.parse("X.STATE1")!!.slot)
        assertEquals(auto, StateNames.parse("X.State.Auto")!!.slot)
    }

    @Test
    fun rejects_others() {
        assertNull(StateNames.parse("X.state.png"))
        assertNull(StateNames.parse("X.srm"))
    }

    @Test
    fun place_rules() {
        assertEquals(2, StateNames.place(2, emptySet()))
        assertEquals(1, StateNames.place(2, setOf(0, 2)))
        assertEquals(0, StateNames.place(auto, setOf(auto)))
        assertEquals(auto, StateNames.place(auto, emptySet()))
        assertEquals(1, StateNames.place(109, setOf(0)))
        assertEquals(null, StateNames.place(3, (0..7).toSet()))
    }

    @Test
    fun with_slot_keeps_the_scheme() {
        assertEquals("X.state1", StateNames.withSlot("X.state", 1))
        assertEquals("X.state", StateNames.withSlot("X.state3", 0))
        assertEquals("X.state.auto", StateNames.withSlot("X.state9", auto))
        assertEquals("X.state.0", StateNames.withSlot("X.state.2", 0))
        assertEquals("X.state.auto", StateNames.withSlot("X.state.2", auto))
        assertEquals("X.gba.st1", StateNames.withSlot("X.gba.st0", 1))
        assertEquals("X.gba.st0", StateNames.withSlot("X.gba.st9", 0))
        assertEquals("X.gba.st9", StateNames.withSlot("X.gba.st0", auto))
        assertNull(StateNames.withSlot("X.srm", 1))
    }

    @Test
    fun with_slot_moves_auto_in_the_given_family() {
        assertEquals("X.state", StateNames.withSlot("X.state.auto", 0))
        assertEquals("X.state2", StateNames.withSlot("X.state.auto", 2, StateFormat.RETROARCH_COMPRESSED))
        assertEquals("X.state.2", StateNames.withSlot("X.state.auto", 2, StateFormat.RETROARCH_ISH))
        assertEquals("X.state.0", StateNames.withSlot("X.State.Auto", 0, StateFormat.RETROARCH_ISH_COMPRESSED))
    }

    @Test
    fun with_slot_round_trips_through_parse() {
        for (name in listOf("A - B.state", "A - B.state.4", "A - B.gba.st2", "A - B.state.auto")) {
            for (slot in listOf(0, 1, 7, auto)) {
                assertEquals(StateName(StateNames.parse(name)!!.stem, slot), StateNames.parse(StateNames.withSlot(name, slot)!!))
            }
        }
    }

    @Test
    fun parses_slot_pictures() {
        assertEquals(StateName("X", 0), StateNames.picture("X.gba.0.bmp"))
        assertEquals(StateName("X", auto), StateNames.picture("X.gba.9.bmp"))
        assertEquals(StateName("Pokemon - Crystal", 1), StateNames.picture("Pokemon - Crystal.gbc.1.BMP"))
        assertNull(StateNames.picture("X.gba.8.bmp"))
        assertNull(StateNames.picture("X.gba.txt"))
        assertNull(StateNames.picture("X.bmp"))
    }
}
