package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class ListWindowTest {
    @Test fun allRowsFitStaysAtZero() {
        assertEquals(0, windowStart(selected = 3, scroll = 0, perPage = 6, count = 4))
        assertEquals(0, windowStart(selected = 3, scroll = 2, perPage = 6, count = 4))
    }

    @Test fun movingDownPastBottomScrollsMinimally() {
        assertEquals(1, windowStart(selected = 6, scroll = 0, perPage = 6, count = 10))
        assertEquals(4, windowStart(selected = 9, scroll = 1, perPage = 6, count = 10))
    }

    @Test fun movingUpPastTopScrollsMinimally() {
        assertEquals(2, windowStart(selected = 2, scroll = 4, perPage = 6, count = 10))
    }

    @Test fun selectionInsideWindowKeepsScroll() {
        assertEquals(3, windowStart(selected = 5, scroll = 3, perPage = 6, count = 10))
    }

    @Test fun wrapAroundJumpsToEnds() {
        assertEquals(0, windowStart(selected = 0, scroll = 4, perPage = 6, count = 10))
        assertEquals(4, windowStart(selected = 9, scroll = 0, perPage = 6, count = 10))
    }

    @Test fun clampsStaleScrollAfterListShrinks() {
        assertEquals(1, windowStart(selected = 2, scroll = 8, perPage = 6, count = 7))
    }

    @Test fun degenerateInputs() {
        assertEquals(0, windowStart(selected = 0, scroll = 5, perPage = 6, count = 0))
        assertEquals(0, windowStart(selected = 0, scroll = 5, perPage = 0, count = 3))
        assertEquals(2, windowStart(selected = 2, scroll = 0, perPage = 1, count = 3))
        assertEquals(2, windowStart(selected = 99, scroll = 0, perPage = 1, count = 3))
    }
}
