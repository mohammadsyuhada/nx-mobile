package com.nxredux.mobile.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRumbleTest {
    @Test fun defaultsToMedium() { assertEquals(GameRumble.MEDIUM, GameRumble.DEFAULT) }

    @Test fun stepsWrapBothWays() {
        assertEquals(GameRumble.LIGHT, GameRumble.OFF.step(1))
        assertEquals(GameRumble.MEDIUM, GameRumble.LIGHT.step(1))
        assertEquals(GameRumble.STRONG, GameRumble.MEDIUM.step(1))
        assertEquals(GameRumble.OFF, GameRumble.STRONG.step(1))
        assertEquals(GameRumble.STRONG, GameRumble.OFF.step(-1))
    }

    @Test fun labels() {
        assertEquals(listOf("Off", "Light", "Medium", "Strong"), GameRumble.entries.map { it.label })
    }

    @Test fun storedByNameAndUnknownReadsAsDefault() {
        for (v in GameRumble.entries) assertEquals(v, GameRumble.fromStored(v.name))
        assertEquals(GameRumble.MEDIUM, GameRumble.fromStored(null))
        assertEquals(GameRumble.MEDIUM, GameRumble.fromStored("ON"))
    }
}
