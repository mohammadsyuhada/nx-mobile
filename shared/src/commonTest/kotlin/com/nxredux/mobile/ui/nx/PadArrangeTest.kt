package com.nxredux.mobile.ui.nx

import androidx.compose.ui.geometry.Rect
import com.nxredux.mobile.settings.GroupTweak
import com.nxredux.mobile.settings.PadGroup
import com.nxredux.mobile.settings.PadLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PadArrangeTest {
    private fun arrange(layout: PadLayout) = PadArrange().apply {
        this.layout = layout
        unitPx = 50f
        bounds = Rect(0f, 0f, 1000f, 400f)
        defaults[PadGroup.DPAD] = Rect(100f, 150f, 250f, 300f)
        defaults[PadGroup.FACE] = Rect(750f, 150f, 900f, 300f)
    }

    @Test fun an_untweaked_group_is_at_its_default() =
        assertEquals(Rect(750f, 150f, 900f, 300f), arrange(PadLayout.EMPTY).placed(PadGroup.FACE))

    @Test fun a_tweaked_group_is_placed() =
        assertEquals(Rect(200f, 150f, 350f, 300f), arrange(PadLayout.EMPTY.with(PadGroup.DPAD, GroupTweak(dx = 2f))).placed(PadGroup.DPAD))

    @Test fun an_unmeasured_group_has_no_rect() = assertNull(arrange(PadLayout.EMPTY).placed(PadGroup.C_STICK))
}
