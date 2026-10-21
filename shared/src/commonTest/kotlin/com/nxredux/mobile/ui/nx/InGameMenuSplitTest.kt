package com.nxredux.mobile.ui.nx

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nxredux.mobile.options.FrontendOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The two-pane in-game menu: the Settings / RetroAchievements split rule on the overlay's own area, the root
 * beside the game panel (or the slot card), a deeper page beside its parent (column browser), and the split-only input.
 */
class InGameMenuSplitTest {
    private val tag = "N64"   // no Console Settings rows

    private fun model(split: Boolean = true) = InGameMenuModel("Tetris", slotCount = 8, initialSlot = 2).apply {
        setFrontend(FrontendOptions.defaults(tag), emptyList(), emptySet(), tag)
        this.split = split
    }

    private fun InGameMenuModel.onRow(label: String) = select(state.page.rows.indexOfFirst { it.label == label })

    // The window, as Settings decides, not the overlay's band-reduced area: portrait never splits.
    @Test fun theMenuSplitsOnTheWindowByTheSettingsRule() {
        assertTrue(inGameMenuSplit(DpSize(933.dp, 704.dp)))    // the unfolded Fold in landscape
        assertTrue(inGameMenuSplit(DpSize(800.dp, 360.dp)))    // a phone in landscape
        assertFalse(inGameMenuSplit(DpSize(412.dp, 915.dp)))   // a phone in portrait
        assertFalse(inGameMenuSplit(DpSize(673.dp, 841.dp)))   // the unfolded Fold in portrait
    }

    @Test fun theRootSitsBesideTheGamePanelOrTheSlotCard() {
        val m = model()
        m.onRow("Save")
        assertEquals(MenuPanes(m.state.stack[0], MenuPaneContent.Slots, focusLeft = true), menuPanes(m.state))
        // Every other row shows the panel; the Options row's page preview is gone.
        for (row in listOf("Options", "Continue", "Quit")) {
            m.onRow(row)
            assertEquals(MenuPanes(m.state.stack[0], MenuPaneContent.Panel, focusLeft = true), menuPanes(m.state))
        }
        assertEquals(1, m.state.stack.size)
    }

    @Test fun aDeeperPageSitsBesideItsParentOnTheRowThatOpenedIt() {
        val m = model()
        m.onRow("Options"); m.press(NxButton.A)
        m.onRow("Frontend"); m.press(NxButton.A)
        val panes = menuPanes(m.state)!!
        assertFalse(panes.focusLeft)
        assertEquals(PageKind.Options, panes.left.kind)
        assertEquals("Frontend", panes.left.rows[panes.left.selected].label)
        assertEquals(PageKind.Frontend, assertIs<MenuPaneContent.Page>(panes.right).page.kind)
    }

    @Test fun adjustAndMessagePagesDrawWhole() {
        val m = model()
        m.onRow("Options"); m.press(NxButton.A)
        m.onRow("Frontend"); m.press(NxButton.A)
        val visual = m.state.page.rows.indexOfFirst { (it as? MenuRow.Option)?.visual == true }
        m.activate(visual)
        assertIs<PageKind.Adjust>(m.state.page.kind)
        assertNull(menuPanes(m.state))
        m.showMessage("Hello", "World")
        assertNull(menuPanes(m.state))
    }

    @Test fun rightOnTheRootEntersThePanelAndOpensARowThatOpensAPageDeeper() {
        val m = model()
        m.onRow("Options")
        assertNull(m.press(NxButton.RIGHT))
        assertEquals(PageKind.Root, m.state.page.kind)
        assertEquals(PanelFocus.Tabs, m.state.panelFocus)
        m.press(NxButton.B)
        m.press(NxButton.A)
        m.onRow("Frontend")
        m.press(NxButton.RIGHT)
        assertEquals(PageKind.Frontend, m.state.page.kind)
    }

    @Test fun rightStillChangesValuesWhenSplit() {
        val m = model()
        m.onRow("Save")
        m.press(NxButton.RIGHT)
        assertEquals(3, m.state.slotIndex)
        assertEquals(1, m.state.stack.size)
    }

    @Test fun rightOnARootSubmenuEntersThePanelWhenNotSplit() {
        val m = model(split = false)
        m.onRow("Options")
        assertNull(m.press(NxButton.RIGHT))
        assertEquals(PageKind.Root, m.state.page.kind)
        assertEquals(PanelFocus.Tabs, m.state.panelFocus)
    }

    @Test fun rightOnASubmenuDeeperDoesNothingWhenNotSplit() {
        val m = model(split = false)
        m.onRow("Options"); m.press(NxButton.A)
        m.onRow("Frontend")
        assertNull(m.press(NxButton.RIGHT))
        assertEquals(PageKind.Options, m.state.page.kind)
    }

    @Test fun aTapOnTheParentPaneOpensThatRowInPlaceOfTheCurrentPage() {
        val m = model()
        m.onRow("Options"); m.press(NxButton.A)
        m.onRow("Frontend"); m.press(NxButton.A)
        val shaders = m.state.stack[1].rows.indexOfFirst { it.label == "Shaders" }
        m.tapParent(PageKind.Options, shaders)
        assertEquals(listOf(PageKind.Root, PageKind.Options, PageKind.Shaders), m.state.stack.map { it.kind })
        assertEquals(shaders, m.state.stack[1].selected)
        // nxTap's second call (and a tap on the row already open) finds it open: nothing changes.
        m.tapParent(PageKind.Options, shaders)
        assertEquals(listOf(PageKind.Root, PageKind.Options, PageKind.Shaders), m.state.stack.map { it.kind })
    }

    @Test fun aTapOnAParentRowThatOpensNothingFocusesThatLevel() {
        val m = model()
        m.onRow("Options"); m.press(NxButton.A)
        val quit = m.state.stack[0].rows.indexOfFirst { it.label == "Quit" }
        assertNull(m.tapParent(PageKind.Root, quit))
        assertEquals(listOf(PageKind.Root), m.state.stack.map { it.kind })
        assertEquals(quit, m.state.page.selected)
        // The second call of the same tap: the root is no longer a parent pane, so it does not quit.
        assertNull(m.tapParent(PageKind.Root, quit))
        assertEquals(1, m.state.stack.size)
    }
}
