package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PadFaceLayoutTest {
    @Test fun nintendo_vendor_is_nintendo_layout() {
        assertEquals(PadFaceLayout.NINTENDO, PadFaceLayout.detect(0x057e, "Pro Controller"))
        assertEquals(PadFaceLayout.NINTENDO, PadFaceLayout.detect(0x057e, ""))
    }

    @Test fun the_pocket_taco_is_nintendo_layout_by_name() {
        assertEquals(PadFaceLayout.NINTENDO, PadFaceLayout.detect(0x3537, "GameSir-Pocket 1"))
        assertEquals(PadFaceLayout.NINTENDO, PadFaceLayout.detect(0, "gamesir pocket 1"))
    }

    @Test fun anything_else_is_xbox_layout() {
        assertEquals(PadFaceLayout.XBOX, PadFaceLayout.detect(0x045e, "Xbox Wireless Controller"))
        assertEquals(PadFaceLayout.XBOX, PadFaceLayout.detect(0x054c, "DualSense Wireless Controller"))
        assertEquals(PadFaceLayout.XBOX, PadFaceLayout.detect(0x3537, "GameSir-T4 Pro"))
        assertEquals(PadFaceLayout.XBOX, PadFaceLayout.detect(0, ""))
    }

    @Test fun no_physical_device_has_no_layout() {
        assertNull(PadFaceLayout.detectDevice(-1, isVirtual = true, 0, "Virtual"))   // adb input keyevent
        assertNull(PadFaceLayout.detectDevice(-1, isVirtual = false, 0x057e, "Pro Controller"))
        assertNull(PadFaceLayout.detectDevice(7, isVirtual = true, 0x045e, "Xbox Wireless Controller"))
        assertEquals(PadFaceLayout.XBOX, PadFaceLayout.detectDevice(7, isVirtual = false, 0x045e, "Xbox Wireless Controller"))
        assertEquals(PadFaceLayout.NINTENDO, PadFaceLayout.detectDevice(0, isVirtual = false, 0x057e, "Pro Controller"))
    }
}
