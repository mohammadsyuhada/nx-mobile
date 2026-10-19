package com.nxredux.mobile.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsoleSettingsTest {
    @Test fun nds_offers_its_screen_rows_without_the_ds_prefix() {
        val rows = ConsoleSettings.tableFor("NDS").filter { it !in PadOptions.all }
        assertEquals(
            listOf(
                NdsOptions.LAYOUT_PORTRAIT, NdsOptions.LAYOUT_PORTRAIT_CONTROLLER, NdsOptions.LAYOUT_LANDSCAPE,
                NdsOptions.BIG_SCREEN, NdsOptions.PIP_CORNER, NdsOptions.PIP_SIZE, NdsOptions.PIP_OPACITY,
            ),
            rows.map { it.key },
        )
        assertEquals(
            listOf(
                "Layout (portrait)", "Layout (portrait, controller)", "Layout (landscape)",
                "Big Screen", "Inset Corner", "Inset Size", "Inset Opacity",
            ),
            rows.map { it.label },
        )
        assertTrue(rows.none { it.label.startsWith("DS") })
        assertEquals(NdsOptions.table, rows)                  // one definition
    }

    @Test fun dc_offers_graphics_api_and_the_shared_controller_layout_row() {
        assertEquals(listOf(GraphicsApiOptions.row, PadOptions.layoutRow), ConsoleSettings.tableFor("DC"))
        assertEquals(listOf("Graphics API", "Controller Layout"), ConsoleSettings.tableFor(" dc ").map { it.label })
    }

    @Test fun other_consoles_have_the_layout_row_only_and_n64_adds_graphics_api() {
        assertEquals(listOf(PadOptions.layoutRow), ConsoleSettings.tableFor("GBA"))
        assertEquals(listOf(PadOptions.layoutRow), ConsoleSettings.tableFor(""))
        assertEquals(listOf(GraphicsApiOptions.row, PadOptions.layoutRow), ConsoleSettings.tableFor("N64"))
        assertEquals(listOf(PadOptions.segaPadRow, PadOptions.layoutRow), ConsoleSettings.tableFor("MD"))
    }
}
