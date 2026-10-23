package com.nxredux.mobile.netplay.usb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbIdsTest {
    @Test fun nxlink_is_matched_by_class_only() {
        assertTrue(UsbIds.isNxlink(0xFF, 0x4E, 1))
        assertFalse(UsbIds.isNxlink(0xFF, 0x42, 1))   // adb
        assertFalse(UsbIds.isNxlink(0xFF, 0x4E, 2))
    }

    @Test fun accessory_mode_ids() {
        assertTrue(UsbIds.isAccessoryMode(0x18D1, 0x2D00))
        assertTrue(UsbIds.isAccessoryMode(0x18D1, 0x2D01))
        assertFalse(UsbIds.isAccessoryMode(0x18D1, 0x4EE1))   // a Pixel in MTP mode
    }

    @Test fun peripherals_never_get_the_aoa_switch() {
        assertTrue(UsbIds.isAoaCandidate(listOf(6 to 1)))                  // MTP / PTP
        assertTrue(UsbIds.isAoaCandidate(listOf(0xFF to 0xFF, 0xFF to 0x42)))   // vendor interfaces (MTP, adb)
        assertTrue(UsbIds.isAoaCandidate(emptyList()))                     // charging only
        assertFalse(UsbIds.isAoaCandidate(listOf(3 to 0)))                 // a controller
        assertFalse(UsbIds.isAoaCandidate(listOf(0xFF to 0xFF, 3 to 0)))   // a vendor pad with HID
        assertFalse(UsbIds.isAoaCandidate(listOf(1 to 1, 1 to 2)))         // audio
        assertFalse(UsbIds.isAoaCandidate(listOf(8 to 6)))                 // storage
        assertFalse(UsbIds.isAoaCandidate(listOf(9 to 0)))                 // hub
        assertFalse(UsbIds.isAoaCandidate(listOf(14 to 1)))                // video
    }

    @Test fun vendor_class_pads_never_get_the_aoa_switch() {
        assertFalse(UsbIds.isAoaCandidate(listOf(0xFF to 0x5D, 0xFF to 0x5D)))   // an XInput pad (Xbox 360)
        assertFalse(UsbIds.isAoaCandidate(listOf(0xFF to 0x47)))                // an Xbox One GIP pad
        assertFalse(UsbIds.isAoaCandidate(listOf(0xFF to 0xFF, 0xFF to 0x5D)))   // a vendor interface beside an XInput one
    }

    @Test fun a_usb_input_device_never_gets_the_aoa_switch() {
        val inputs = listOf(0x045E to 0x028E, 0x046D to 0xC52B)                  // an Xbox 360 pad, a Logitech receiver
        assertTrue(UsbIds.isInputDevice(0x045E, 0x028E, inputs))
        assertFalse(UsbIds.isInputDevice(0x18D1, 0x4EE1, inputs))                // a Pixel in MTP mode
        assertFalse(UsbIds.isInputDevice(0x045E, 0x028E, emptyList()))          // no input devices attached
    }

    @Test fun a_handheld_adb_gadget_never_gets_the_aoa_switch() {
        assertTrue(UsbIds.isHandheldAdbGadget(0x18D1, 0xD002))    // the Brick, "TRIMUI ADB"
        assertTrue(UsbIds.isHandheldAdbGadget(0x18D1, 0xD001))    // the other generic gadget adb id
        assertFalse(UsbIds.isHandheldAdbGadget(0x18D1, 0x4EE7))   // a Pixel with adb
        assertFalse(UsbIds.isHandheldAdbGadget(0x04E8, 0x6860))   // a Samsung
        assertFalse(UsbIds.isHandheldAdbGadget(0x18D1, 0x2D00))   // accessory mode, matched separately
    }

    @Test fun aoa_strings_in_protocol_order() {
        assertEquals(
            listOf("NX Redux", "NX Redux Mobile Link", "NX Redux Mobile cable link", "1", "https://github.com/nx-redux/nx-mobile", "NXREDUX0001"),
            UsbIds.AOA_STRINGS,
        )
    }
}
