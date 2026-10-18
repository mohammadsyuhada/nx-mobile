package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class TitleLeadTest {
    @Test fun the_name_before_the_first_separator_is_kept_whole() {
        assertEquals("RetroAchievements | " to "Super Mario Land", splitTitleLead("RetroAchievements | Super Mario Land"))
        assertEquals("Game Time | " to "Game Boy | Extra", splitTitleLead("Game Time | Game Boy | Extra"))
    }

    @Test fun a_title_without_a_separator_is_all_cuttable() {
        assertEquals("" to "Settings", splitTitleLead("Settings"))
        assertEquals("" to "", splitTitleLead(""))
    }
}
