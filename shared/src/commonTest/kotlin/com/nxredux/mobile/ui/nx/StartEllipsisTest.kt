package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class StartEllipsisTest {
    private fun upTo(n: Int): (String) -> Boolean = { it.length <= n }

    @Test fun a_line_that_fits_is_unchanged() {
        assertEquals("/sdcard/NXRedux", startEllipsized("/sdcard/NXRedux", upTo(15)))
    }

    @Test fun a_long_path_drops_whole_leading_folders_first() {
        assertEquals("…/Games/NXRedux", startEllipsized("/storage/1234-ABCD/Games/NXRedux", upTo(16)))
        assertEquals("…/NXRedux", startEllipsized("/storage/1234-ABCD/Games/NXRedux", upTo(12)))
    }

    @Test fun a_last_folder_too_long_for_the_line_is_cut_from_the_start() {
        assertEquals("…Redux", startEllipsized("/sdcard/NXRedux", upTo(6)))
    }
}
