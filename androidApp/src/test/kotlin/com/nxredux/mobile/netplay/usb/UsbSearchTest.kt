package com.nxredux.mobile.netplay.usb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbSearchTest {
    @Test fun a_reattach_skips_the_device_that_dropped() {
        assertEquals("/dev/bus/usb/001/005", UsbSearch(UsbSide.HOST, lost = "/dev/bus/usb/001/005").lost)
        assertEquals(null, UsbSearch(null).lost)
    }

    @Test fun the_first_bring_up_looks_everywhere_for_thirty_seconds() {
        val s = UsbSearch(null)
        assertTrue(s.accessory)
        assertTrue(s.devices)
        assertTrue(s.aoaSwitch)
        assertTrue(s.posts)
        assertEquals(30_000L, s.windowMs)
    }

    @Test fun a_host_reattach_looks_for_devices_for_twenty_eight_seconds() {
        val s = UsbSearch(UsbSide.HOST)
        assertFalse(s.accessory)
        assertTrue(s.devices)
        assertFalse(s.aoaSwitch)
        assertFalse(s.posts)
        assertEquals(28_000L, s.windowMs)
    }

    @Test fun a_device_reattach_looks_for_our_accessory_for_twenty_eight_seconds() {
        val s = UsbSearch(UsbSide.DEVICE)
        assertTrue(s.accessory)
        assertFalse(s.devices)
        assertFalse(s.aoaSwitch)
        assertFalse(s.posts)
        assertEquals(28_000L, s.windowMs)
    }
}
