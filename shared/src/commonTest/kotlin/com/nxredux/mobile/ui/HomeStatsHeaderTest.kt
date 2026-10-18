package com.nxredux.mobile.ui

import com.nxredux.mobile.ui.nx.HomeFrame
import com.nxredux.mobile.ui.nx.MENU_TAB_ROW_HEIGHT
import com.nxredux.mobile.ui.nx.homeStatsHeader
import com.nxredux.mobile.ui.nx.homeStripLineH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Grid (and Carousel) Home with the tab row hidden: the stats strip sits in a sticky header in the tab row's place, and the
// page below lays out as with the tab row shown and the strip hidden.
class HomeStatsHeaderTest {
    private val cover = HomeFrame(624f, 380f, portrait = false, titleHidden = true)

    @Test fun no_header_with_the_tab_row_shown() {
        assertNull(homeStatsHeader(cover.copy(titleHidden = false), stripLines = 2))
    }

    @Test fun the_header_takes_the_tab_rows_place_and_the_body_the_rest() {
        val header = homeStatsHeader(cover, stripLines = 2)!!
        assertTrue(header.height >= MENU_TAB_ROW_HEIGHT.value)
        assertTrue(header.height >= homeStripLineH(cover) * 2)                  // two lines fit
        assertEquals(cover.h - header.height, header.body.h)
        assertEquals(false, header.body.titleHidden)                              // laid out as under a tab row
        assertEquals(true, header.body.statsHidden)                               // the strip is in the header, not the page
        assertEquals(cover.w, header.body.w)
    }

    // Carousel Home takes the header too, in either orientation: the portrait type's two lines fit it.
    @Test fun the_portrait_type_has_the_header_too() {
        val portrait = HomeFrame(475f, 335f, portrait = true, titleHidden = true)
        val header = homeStatsHeader(portrait, stripLines = 2)!!
        assertEquals(2, header.lines)
        assertTrue(header.height >= homeStripLineH(portrait) * 2)
        assertEquals(portrait.h - header.height, header.body.h)
    }

    @Test fun no_strip_still_keeps_the_band_row() {
        assertEquals(MENU_TAB_ROW_HEIGHT.value, homeStatsHeader(cover, stripLines = 0)!!.height)
    }
}
