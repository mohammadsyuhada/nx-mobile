package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WiiControllerTest {
    private fun c(flags: String) = WiiControls.parse(flags)

    @Test fun unknownGamesAndPlainWiimoteGamesAreSideways() {
        assertEquals(WiiPad.SIDEWAYS, WiiController.auto(null))
        assertEquals(WiiPad.SIDEWAYS, WiiController.auto(c("")))
    }

    @Test fun classicWinsOverNunchukAndUpright() {
        assertEquals(WiiPad.CLASSIC, WiiController.auto(c("CN")))
        assertEquals(WiiPad.CLASSIC, WiiController.auto(c("CU")))
    }

    // The hand-kept upright list overrides GameTDB's region-inconsistent Nunchuk flag (Wii Play, RHAK01: "NU").
    @Test fun uprightThenNunchuk() {
        assertEquals(WiiPad.NUNCHUK, WiiController.auto(c("N")))
        assertEquals(WiiPad.UPRIGHT, WiiController.auto(c("U")))
        assertEquals(WiiPad.UPRIGHT, WiiController.auto(c("NU")))
    }

    @Test fun motionPlusGamesNeverGetClassic() {
        assertEquals(WiiPad.NUNCHUK, WiiController.auto(c("CNM")))
        assertEquals(WiiPad.UPRIGHT, WiiController.auto(c("CMU")))
        assertEquals(WiiPad.SIDEWAYS, WiiController.auto(c("CM")))
    }

    @Test fun aForcedSettingWinsAndAnUnknownValueIsAuto() {
        assertEquals(WiiPad.CLASSIC, WiiController.resolve(WiiController.CLASSIC, c("N")))
        assertEquals(WiiPad.NUNCHUK, WiiController.resolve("bogus", c("N")))
        assertEquals(WiiPad.NUNCHUK, WiiController.resolve(null, c("N")))
    }

    @Test fun motionPlusFollowsTheFlagExceptOnClassic() {
        assertTrue(WiiController.motionPlus(WiiPad.SIDEWAYS, c("M")))
        assertFalse(WiiController.motionPlus(WiiPad.CLASSIC, c("M")))
        assertFalse(WiiController.motionPlus(WiiPad.NUNCHUK, c("N")))
        assertFalse(WiiController.motionPlus(WiiPad.UPRIGHT, null))
    }

    @Test fun deviceIds() {
        assertEquals(0x201, WiiController.device(WiiPad.SIDEWAYS, false))
        assertEquals(0x001, WiiController.device(WiiPad.UPRIGHT, false))
        assertEquals(0x301, WiiController.device(WiiPad.NUNCHUK, false))
        assertEquals(0x501, WiiController.device(WiiPad.CLASSIC, false))
        assertEquals(0x801, WiiController.device(WiiPad.SIDEWAYS, true))
        assertEquals(0x701, WiiController.device(WiiPad.UPRIGHT, true))
        assertEquals(0x901, WiiController.device(WiiPad.NUNCHUK, true))
        assertEquals(0x501, WiiController.device(WiiPad.CLASSIC, true))   // no MotionPlus Classic
    }

    @Test fun motionAutoIsOnForMotionPlusGamesOnly() {
        assertTrue(WiiMotion.resolve(WiiMotion.AUTO, WiiPad.SIDEWAYS, c("M"), hasSensors = true))
        assertFalse(WiiMotion.resolve(WiiMotion.AUTO, WiiPad.SIDEWAYS, c(""), hasSensors = true))
        assertTrue(WiiMotion.resolve(WiiMotion.ON, WiiPad.SIDEWAYS, c(""), hasSensors = true))
        assertFalse(WiiMotion.resolve(WiiMotion.OFF, WiiPad.SIDEWAYS, c("M"), hasSensors = true))
    }

    @Test fun motionNeedsSensorsAndAWiimote() {
        assertFalse(WiiMotion.resolve(WiiMotion.ON, WiiPad.SIDEWAYS, c(""), hasSensors = false))
        assertFalse(WiiMotion.resolve(WiiMotion.ON, WiiPad.CLASSIC, c(""), hasSensors = true))
    }
}
