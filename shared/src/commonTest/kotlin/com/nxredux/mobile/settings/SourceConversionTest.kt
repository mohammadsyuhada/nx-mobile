package com.nxredux.mobile.settings

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.FileRefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SourceConversionTest {
    private val t = "content://com.android.externalstorage.documents/tree/"
    private val folders = setOf("/storage/emulated/0/NXRedux", "/storage/1234-ABCD/Roms")
    private suspend fun convert(home: ContentRef?, extras: List<ExtraFolder>) = SourceConversion.convert(home, extras) { it in folders }

    @Test fun a_mapped_home_and_extra_that_are_folders_become_file_refs_with_ids_kept() = runTest {
        val card = ExtraFolder("Roms-1a2b3c4d", ContentRef("${t}1234-ABCD%3ARoms"))
        val out = convert(ContentRef("${t}primary%3ANXRedux"), listOf(card))
        assertEquals(FileRefs.of("/storage/emulated/0/NXRedux"), out.home)
        assertEquals(listOf(ExtraFolder("Roms-1a2b3c4d", FileRefs.of("/storage/1234-ABCD/Roms"))), out.extras)
    }

    @Test fun unmapped_missing_and_file_refs_stay_as_they_are() = runTest {
        val download = ExtraFolder("dl-1", ContentRef("content://com.android.providers.downloads.documents/tree/raw%3A%2Fx"))
        val gone = ExtraFolder("gone-1", ContentRef("${t}primary%3AGone"))        // mapped, but no such folder
        val files = ExtraFolder("f-1", FileRefs.of("/storage/emulated/0/Loose"))
        val out = convert(null, listOf(download, gone, files))
        assertEquals(null, out.home)
        assertEquals(listOf(download, gone, files), out.extras)
    }
}
