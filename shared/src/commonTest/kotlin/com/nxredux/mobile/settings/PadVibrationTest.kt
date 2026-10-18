package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** Controls → Pad vibration: Off / Light / Medium / Strong, Light by default, stored by its name. */
class PadVibrationTest {
    @Test fun defaultsToLight() { assertEquals(PadVibration.LIGHT, PadVibration.DEFAULT) }

    @Test fun cyclesBothWaysAndWraps() {
        assertEquals(PadVibration.MEDIUM, PadVibration.LIGHT.step(1))
        assertEquals(PadVibration.STRONG, PadVibration.MEDIUM.step(1))
        assertEquals(PadVibration.OFF, PadVibration.STRONG.step(1))
        assertEquals(PadVibration.LIGHT, PadVibration.OFF.step(1))
        assertEquals(PadVibration.STRONG, PadVibration.OFF.step(-1))
        assertEquals(PadVibration.OFF, PadVibration.LIGHT.step(-1))
    }

    @Test fun labels() {
        assertEquals(listOf("Off", "Light", "Medium", "Strong"), PadVibration.entries.map { it.label })
    }

    @Test fun storedByNameAndAnUnknownOrMissingValueReadsAsTheDefault() {
        for (v in PadVibration.entries) assertEquals(v, PadVibration.fromStored(v.name))
        assertEquals(PadVibration.LIGHT, PadVibration.fromStored(null))
        assertEquals(PadVibration.LIGHT, PadVibration.fromStored("ON"))
        assertEquals(PadVibration.LIGHT, PadVibration.fromStored(""))
    }
}
