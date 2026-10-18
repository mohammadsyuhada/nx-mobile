package com.nxredux.mobile.ui.nx

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DreamcastPadColorsTest {
    @Test fun dreamcast_face_buttons_take_the_controller_colours() {
        // Bottom NxButton.B is the red "A", right A the blue "B", left Y the yellow "X", top X the green "Y".
        val a = PadProfile.DREAMCAST.faceColors(NxButton.B)!!
        assertSame(DreamcastPadColors.a, a.first)
        assertEquals(NxDefaults.colors.main, a.second)
        val b = PadProfile.DREAMCAST.faceColors(NxButton.A)!!
        assertSame(DreamcastPadColors.b, b.first)
        assertEquals(NxDefaults.colors.main, b.second)
        val x = PadProfile.DREAMCAST.faceColors(NxButton.Y)!!
        assertSame(DreamcastPadColors.x, x.first)
        assertEquals(Color(0xFF2B2826), x.second)
        val y = PadProfile.DREAMCAST.faceColors(NxButton.X)!!
        assertSame(DreamcastPadColors.y, y.first)
        assertEquals(NxDefaults.colors.main, y.second)
    }

    @Test fun dreamcast_other_buttons_keep_the_theme() {
        val face = setOf(NxButton.A, NxButton.B, NxButton.X, NxButton.Y)
        for (b in NxButton.entries - face) assertNull(PadProfile.DREAMCAST.faceColors(b), b.name)
    }

    @Test fun standard_and_n64_have_no_face_colours() {
        for (b in NxButton.entries) {
            assertNull(PadProfile.STANDARD.faceColors(b), b.name)
            assertNull(PadProfile.N64.faceColors(b), b.name)
        }
    }
}
