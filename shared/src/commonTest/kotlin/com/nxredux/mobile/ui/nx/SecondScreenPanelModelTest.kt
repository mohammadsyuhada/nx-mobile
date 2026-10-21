package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecondScreenPanelModelTest {
    private val full = SlotUi("Slot 2", "Oct 18, 10:00", "file:///t.bmp", occupied = true)
    private val empty = SlotUi("Slot 3", "empty", null, occupied = false)
    private val auto = SlotUi("Auto", "Oct 21, 09:00", "file:///a.bmp", occupied = true)

    @Test fun tabs_without_achievements() = assertEquals(listOf(PanelTab.STATES), PanelModel.tabs(achievementsAvailable = false))
    @Test fun tabs_with_achievements() = assertEquals(listOf(PanelTab.STATES, PanelTab.ACHIEVEMENTS), PanelModel.tabs(achievementsAvailable = true))

    @Test fun tap_occupied_loads() {
        assertEquals(PanelEvent.Load(2), PanelModel.tap(2, full))
        assertEquals(PanelEvent.Load(0), PanelModel.tap(0, auto))
    }
    @Test fun tap_empty_does_nothing() = assertNull(PanelModel.tap(3, empty))

    @Test fun long_press_manual_saves() {
        assertEquals(PanelEvent.Save(2), PanelModel.longPress(2))
        assertEquals(PanelEvent.Save(3), PanelModel.longPress(3))
    }
    @Test fun long_press_auto_ignored() = assertNull(PanelModel.longPress(0))
}
