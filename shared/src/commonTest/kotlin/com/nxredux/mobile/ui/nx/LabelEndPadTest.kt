package com.nxredux.mobile.ui.nx

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelEndPadTest {
    @Test fun labelEndsAtNinetyPercentOfTheScreenWithoutInsets() {
        // 1000 dp screen: the text ends at 900 dp, so the row keeps 100 dp minus the 14 dp pill inset.
        assertEquals(86f, nxLabelEndPad(1000.dp, 0.dp, 0.dp, 14.dp).value, 0.001f)
    }

    @Test fun measuresFromTheScreenEdgeIncludingTheInsets() {
        // 40 dp left + 920 dp list + 40 dp right: the text still ends 900 dp from the screen's left edge.
        val pad = nxLabelEndPad(920.dp, 40.dp, 40.dp, 14.dp)
        assertEquals(900f, 40f + 920f - pad.value - 14f, 0.001f)
    }

    @Test fun neverNegative() {
        assertEquals(0f, nxLabelEndPad(500.dp, 0.dp, 120.dp, 14.dp).value)
    }
}
