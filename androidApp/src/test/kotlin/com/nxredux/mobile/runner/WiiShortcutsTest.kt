package com.nxredux.mobile.runner

import android.view.KeyEvent
import com.nxredux.mobile.ui.nx.NxButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WiiShortcutsTest {
    // The pad's HOME reaches Dolphin as R3 (its Home on every Wii controller); a physical R3 still passes through as itself.
    @Test fun the_pads_home_is_sent_as_r3_but_r3_is_not_read_back_as_home() {
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, PadKeymap.keyCodeFor(NxButton.HOME))
        assertNull(PadKeymap.buttonFor(KeyEvent.KEYCODE_BUTTON_THUMBR))
    }

    @Test fun a_physical_l3_recenters_wii_motion_only_on_the_wii() {
        assertTrue(WiiShortcuts.isRecenter(wii = true, KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertFalse(WiiShortcuts.isRecenter(wii = false, KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertFalse(WiiShortcuts.isRecenter(wii = true, KeyEvent.KEYCODE_BUTTON_THUMBR))   // R3 is Home
    }

    // A physical R3 on the Wii is Home, taken by the app (a resume point first) rather than passed straight to Dolphin.
    @Test fun a_physical_r3_is_home_only_on_the_wii() {
        assertTrue(WiiShortcuts.isHome(wii = true, KeyEvent.KEYCODE_BUTTON_THUMBR))
        assertFalse(WiiShortcuts.isHome(wii = false, KeyEvent.KEYCODE_BUTTON_THUMBR))
        assertFalse(WiiShortcuts.isHome(wii = true, KeyEvent.KEYCODE_BUTTON_THUMBL))
    }
}
