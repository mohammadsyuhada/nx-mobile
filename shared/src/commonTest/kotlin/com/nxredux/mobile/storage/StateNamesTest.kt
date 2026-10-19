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
}
