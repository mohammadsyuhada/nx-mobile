package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathDocIdsTest {
    @Test fun a_child_of_the_volume_root_has_no_slash() =
        assertEquals("primary:Roms", pathChildDocId("primary:", "Roms"))

    @Test fun a_nested_child_is_joined_with_a_slash() =
        assertEquals("primary:Roms/GB (GB)", pathChildDocId("primary:Roms", "GB (GB)"))

    @Test fun a_trailing_slash_is_not_doubled() =
        assertEquals("primary:Roms/x", pathChildDocId("primary:Roms/", "x"))

    @Test fun names_that_are_not_one_segment_are_rejected() {
        assertNull(pathChildDocId("primary:Roms", "a/b"))
        assertNull(pathChildDocId("primary:Roms", "."))
        assertNull(pathChildDocId("primary:Roms", ".."))
        assertNull(pathChildDocId("primary:Roms", ""))
    }

    @Test fun a_volume_that_is_not_mounted_is_offline() {
        assertTrue(volumeOffline("4913-1501:SDRoms", emptyList()))
        assertTrue(volumeOffline("4913-1501:", listOf("ABCD-0000")))
    }

    @Test fun a_mounted_volume_is_online_whatever_the_case() {
        assertFalse(volumeOffline("4913-1501:SDRoms", listOf("4913-1501")))
        assertFalse(volumeOffline("abcd-ef01:Roms", listOf("ABCD-EF01")))
    }

    @Test fun primary_and_home_and_odd_ids_are_never_offline() {
        assertFalse(volumeOffline("primary:NXRedux", emptyList()))
        assertFalse(volumeOffline("primary_dcim:Camera", emptyList()))                   // a composite root on internal storage
        assertFalse(volumeOffline("PRIMARY:NXRedux", emptyList()))
        assertFalse(volumeOffline("home:Roms", emptyList()))
        assertFalse(volumeOffline("no-colon", emptyList()))
        assertFalse(volumeOffline(":Roms", emptyList()))
    }

    @Test fun the_parent_of_a_nested_folder_is_one_level_up() =
        assertEquals("primary:Roms", pathParentDocId("primary:Roms/My Arcade", "primary:Roms"))

    @Test fun the_parent_of_a_top_level_folder_is_the_volume_root() =
        assertEquals("primary:", pathParentDocId("primary:Roms", "primary:"))

    @Test fun a_tree_root_has_no_parent_inside_its_tree() {
        assertNull(pathParentDocId("primary:NXRedux", "primary:NXRedux"))
        assertNull(pathParentDocId("primary:", "primary:"))
    }

    @Test fun a_folder_outside_the_tree_has_no_parent() {
        assertNull(pathParentDocId("primary:Other/x", "primary:NXRedux"))
        assertNull(pathParentDocId("primary:NXReduxOld/x", "primary:NXRedux"))
    }

    @Test fun a_trailing_slash_is_ignored() =
        assertEquals("primary:NXRedux", pathParentDocId("primary:NXRedux/Roms/", "primary:NXRedux"))
}
