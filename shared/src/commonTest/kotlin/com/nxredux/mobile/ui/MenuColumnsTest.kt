package com.nxredux.mobile.ui

import com.nxredux.mobile.ui.nx.NxButton
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuColumnsTest {
    @Test fun columnsRule() {
        assertEquals(1, menuColumns(8, fitsTwo = true))
        assertEquals(2, menuColumns(9, fitsTwo = true))
        assertEquals(2, menuColumns(16, fitsTwo = true))
        assertEquals(1, menuColumns(17, fitsTwo = true))
        assertEquals(1, menuColumns(12, fitsTwo = false))
    }

    @Test fun tenItemsTwoColumns() {
        fun step(i: Int, b: NxButton) = menuStep(i, 10, 2, b)
        assertEquals(8, step(7, NxButton.DOWN))   // the left column's foot: on to the right column's top
        assertEquals(4, step(3, NxButton.DOWN))
        assertEquals(7, step(8, NxButton.UP))     // the right column's top: back to the left column's foot
        assertEquals(9, step(8, NxButton.DOWN))
        assertEquals(0, step(9, NxButton.DOWN))   // the last item: round to the first
        assertEquals(9, step(0, NxButton.UP))     // the first: round to the last
        assertEquals(9, step(3, NxButton.RIGHT))
        assertEquals(9, step(1, NxButton.RIGHT))
        assertEquals(8, step(0, NxButton.RIGHT))
        assertEquals(9, step(9, NxButton.RIGHT))
        assertEquals(1, step(9, NxButton.LEFT))
        assertEquals(0, step(8, NxButton.LEFT))
        assertEquals(3, step(3, NxButton.LEFT))
    }

    @Test fun oneColumnLoops() {
        assertEquals(4, menuStep(0, 5, 1, NxButton.UP))     // the top: round to the bottom
        assertEquals(3, menuStep(2, 5, 1, NxButton.DOWN))
        assertEquals(0, menuStep(4, 5, 1, NxButton.DOWN))   // the bottom: round to the top
        assertEquals(2, menuStep(2, 5, 1, NxButton.LEFT))
        assertEquals(2, menuStep(2, 5, 1, NxButton.RIGHT))
    }

    @Test fun nineItemsRightColumnHoldsOnlyTheLast() {
        for (i in 0..7) assertEquals(8, menuStep(i, 9, 2, NxButton.RIGHT))
        assertEquals(0, menuStep(8, 9, 2, NxButton.LEFT))
        assertEquals(0, menuStep(8, 9, 2, NxButton.DOWN))
        assertEquals(7, menuStep(8, 9, 2, NxButton.UP))
    }
}
