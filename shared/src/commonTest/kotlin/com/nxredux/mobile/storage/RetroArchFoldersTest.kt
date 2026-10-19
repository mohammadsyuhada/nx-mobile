package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class RetroArchFoldersTest {
    @Test fun the_fold_folder_comes_first() {
        assertEquals("Android/media/com.retroarch/RetroArch", RetroArchFolders.ROOTS.first())
        assertEquals("RetroArch", RetroArchFolders.ROOTS.last())
        assertEquals(listOf("saves", "states"), RetroArchFolders.TOPS)
    }
}
