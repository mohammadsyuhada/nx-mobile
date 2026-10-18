package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class HintOrderTest {
    @Test fun hintsFollowThePhysicalPadLeftToRight() {
        val hints = listOf(NxButton.A, NxButton.B, NxButton.X, NxButton.Y, NxButton.START, NxButton.MENU, NxButton.SELECT, NxButton.LEFT).map { Hint(it, it.name) }
        assertEquals(
            listOf(NxButton.MENU, NxButton.LEFT, NxButton.SELECT, NxButton.START, NxButton.Y, NxButton.X, NxButton.B, NxButton.A),
            hints.inPadOrder().map { it.button },
        )
    }

    @Test fun optionsLeadThenSystemKeysThenFaceButtons() {
        val hints = listOf(NxButton.A, NxButton.START, NxButton.B, NxButton.MENU, NxButton.Y, NxButton.SELECT, NxButton.X).map { Hint(it, it.name) }
        assertEquals(
            listOf(NxButton.MENU, NxButton.SELECT, NxButton.START, NxButton.Y, NxButton.X, NxButton.B, NxButton.A),
            hints.inPadOrder().map { it.button },
        )
    }

    @Test fun shouldersFlankTheRestAndDpadHintsKeepTheirOrder() {
        val hints = listOf(Hint(NxButton.R1, "r"), Hint(NxButton.B, "b"), Hint(NxButton.UP, "next"), Hint(NxButton.L1, "l"), Hint(NxButton.LEFT, "change"))
        assertEquals(listOf("next", "change", "l", "b", "r"), hints.inPadOrder().map { it.label })
    }
}
