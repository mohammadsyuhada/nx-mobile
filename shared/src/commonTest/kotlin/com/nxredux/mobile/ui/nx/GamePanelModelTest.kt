package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GamePanelModelTest {
    private val disabled = listOf(Shortcut.FAST_FORWARD, Shortcut.REWIND, Shortcut.SCREENSHOT, Shortcut.RESET)

    private fun shortcuts(discCount: Int = 1, discIndex: Int = 0, connected: Boolean = false, on: Boolean = true, netplay: Boolean = false) =
        GamePanelModel.shortcuts(discCount, discIndex, connected, on, netplay)

    @Test fun tabs_shortcuts_only() {
        assertEquals(listOf(GamePanelTab.SHORTCUTS), GamePanelModel.tabs(raAvailable = false, playerSlots = 1))
        assertEquals(listOf(GamePanelTab.SHORTCUTS), GamePanelModel.tabs(raAvailable = false, playerSlots = 0))
    }

    @Test fun tabs_with_ra_and_players() {
        assertEquals(listOf(GamePanelTab.SHORTCUTS, GamePanelTab.ACHIEVEMENTS, GamePanelTab.PLAYERS), GamePanelModel.tabs(raAvailable = true, playerSlots = 2))
        assertEquals(listOf(GamePanelTab.SHORTCUTS, GamePanelTab.ACHIEVEMENTS), GamePanelModel.tabs(raAvailable = true, playerSlots = 1))
        assertEquals(listOf(GamePanelTab.SHORTCUTS, GamePanelTab.PLAYERS), GamePanelModel.tabs(raAvailable = false, playerSlots = 4))
    }

    @Test fun resolve_tab_falls_back_when_gone() {
        val shortcutsOnly = GamePanelModel.tabs(raAvailable = false, playerSlots = 1)
        assertEquals(GamePanelTab.SHORTCUTS, GamePanelModel.resolveTab(GamePanelTab.ACHIEVEMENTS, shortcutsOnly))
        assertEquals(GamePanelTab.SHORTCUTS, GamePanelModel.resolveTab(GamePanelTab.PLAYERS, shortcutsOnly))
        val all = GamePanelModel.tabs(raAvailable = true, playerSlots = 2)
        assertEquals(GamePanelTab.PLAYERS, GamePanelModel.resolveTab(GamePanelTab.PLAYERS, all))
        assertEquals(GamePanelTab.ACHIEVEMENTS, GamePanelModel.resolveTab(GamePanelTab.ACHIEVEMENTS, all))
    }

    @Test fun swap_disc_only_multi_disc_and_not_netplay() {
        assertTrue(shortcuts(discCount = 1).none { it.shortcut == Shortcut.SWAP_DISC })
        assertTrue(shortcuts(discCount = 0).none { it.shortcut == Shortcut.SWAP_DISC })
        assertTrue(shortcuts(discCount = 3, netplay = true).none { it.shortcut == Shortcut.SWAP_DISC })
        val swap = shortcuts(discCount = 3, discIndex = 1).single { it.shortcut == Shortcut.SWAP_DISC }
        assertTrue(swap.enabled)
        assertEquals("2/3", swap.value)
        assertEquals("Swap disc", swap.label)
    }

    @Test fun second_screen_only_when_connected() {
        assertTrue(shortcuts(connected = false).none { it.shortcut == Shortcut.SECOND_SCREEN })
        val on = shortcuts(connected = true, on = true).single { it.shortcut == Shortcut.SECOND_SCREEN }
        assertTrue(on.enabled)
        assertEquals("On", on.value)
        assertEquals(true, on.on)
        assertEquals("Off", shortcuts(connected = true, on = false).single { it.shortcut == Shortcut.SECOND_SCREEN }.value)
        // Netplay hides only Swap disc.
        assertTrue(shortcuts(connected = true, netplay = true).any { it.shortcut == Shortcut.SECOND_SCREEN })
    }

    @Test fun future_shortcuts_always_disabled() {
        for (list in listOf(shortcuts(), shortcuts(discCount = 2, connected = true), shortcuts(netplay = true))) {
            val future = list.filter { it.shortcut in disabled }
            assertEquals(disabled, future.map { it.shortcut })
            assertTrue(future.none { it.enabled })
        }
        // Display order: Swap disc, Second screen, then the four disabled ones.
        assertEquals(Shortcut.entries.toList(), shortcuts(discCount = 2, connected = true).map { it.shortcut })
    }

    @Test fun disabled_shortcut_has_no_event() {
        for (s in shortcuts().filter { it.shortcut in disabled }) assertNull(GamePanelModel.eventFor(s))
        val list = shortcuts(discCount = 2, connected = true, on = true)
        assertEquals(GamePanelEvent.SwapDisc, GamePanelModel.eventFor(list.single { it.shortcut == Shortcut.SWAP_DISC }))
        assertEquals(GamePanelEvent.SetSecondScreen(false), GamePanelModel.eventFor(list.single { it.shortcut == Shortcut.SECOND_SCREEN }))
        val off = shortcuts(connected = true, on = false).single { it.shortcut == Shortcut.SECOND_SCREEN }
        assertEquals(GamePanelEvent.SetSecondScreen(true), GamePanelModel.eventFor(off))
        // The event follows the typed setting, not the display text.
        val typed = ShortcutState(Shortcut.SECOND_SCREEN, "Second screen", enabled = true, value = "whatever", on = true)
        assertEquals(GamePanelEvent.SetSecondScreen(false), GamePanelModel.eventFor(typed))
        // A swap shortcut forced disabled stays inert too.
        assertNull(GamePanelModel.eventFor(list.single { it.shortcut == Shortcut.SWAP_DISC }.copy(enabled = false)))
    }

    @Test fun player_tap_cycles_to_next_playing_pad() {
        val slots = listOf(PlayerSlotRow("a", "Pad A", true), PlayerSlotRow(null, "", false))
        val pads = listOf(PlayerPadRow("a", "Pad A", true), PlayerPadRow("b", "Pad B", true), PlayerPadRow("c", "Pad C", false))
        assertEquals(GamePanelEvent.SetPlayer(0, "b"), GamePanelModel.nextPlayer(0, slots, pads))
        assertEquals(GamePanelEvent.SetPlayer(1, "a"), GamePanelModel.nextPlayer(1, slots, pads))
        assertEquals(GamePanelEvent.SetPlayer(0, null), GamePanelModel.nextPlayer(0, listOf(PlayerSlotRow("b", "Pad B", true)), pads))
        assertFalse(GamePanelModel.nextPlayer(0, slots, pads).key == "c")   // a not-playing pad is skipped
    }

    @Test fun pad_tap_toggles_playing() {
        assertEquals(GamePanelEvent.SetPlaying("a", false), GamePanelModel.togglePlaying(PlayerPadRow("a", "Pad A", playing = true)))
        assertEquals(GamePanelEvent.SetPlaying("c", true), GamePanelModel.togglePlaying(PlayerPadRow("c", "Pad C", playing = false)))
    }

    @Test fun disc_toast_is_one_based() {
        assertEquals("Disc 2 of 3", GamePanelModel.discToast(1, 3))
        assertEquals("Disc 1 of 2", GamePanelModel.discToast(0, 2))
    }

    @Test fun next_disc_cycles() {
        assertEquals(1, GamePanelModel.nextDisc(0, 3))
        assertEquals(2, GamePanelModel.nextDisc(1, 3))
        assertEquals(0, GamePanelModel.nextDisc(2, 3))
        assertEquals(0, GamePanelModel.nextDisc(5, 3))   // a stale index out of range wraps too
    }
}
