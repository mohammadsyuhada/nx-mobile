package com.nxredux.mobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

// A List game list (and the switcher's List) shows the selected game's stats only in the bar's row: with
// Button hints hidden, or on a touch-only screen where there is no bar. With the bar shown, no stats.
class GameListInfoRowTest {
    @Test fun stats_show_in_the_bar_row_only_without_a_hint_bar() {
        assertEquals(false, gameListInfoInBarRow(emptyBarRoom = false, touch = false))   // the bar shown: no stats
        assertEquals(true, gameListInfoInBarRow(emptyBarRoom = true, touch = false))     // Button hints hidden
        assertEquals(true, gameListInfoInBarRow(emptyBarRoom = false, touch = true))     // touch only
    }
}
