package com.nxredux.mobile.sync

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.settings.ExtraFolder
import com.nxredux.mobile.storage.FileRefs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AndroidSyncExtrasTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun roots_number_file_extras_in_order_and_skip_others() {
        val a = tmp.newFolder("a"); val b = tmp.newFolder("b")
        val roots = AndroidSyncExtras.roots(listOf(
            ExtraFolder("x", FileRefs.of(a.path)), ExtraFolder("saf", ContentRef("content://tree/1")), ExtraFolder("y", FileRefs.of(b.path))))
        assertEquals(listOf(ExtraRoot(0, "x", a.path), ExtraRoot(1, "y", b.path)), roots)
    }

    @Test fun missing_extra_root_is_skipped() {
        val roots = AndroidSyncExtras.roots(listOf(ExtraFolder("gone", FileRefs.of(File(tmp.root, "nope").path))))
        assertEquals(emptyList<ExtraRoot>(), roots)
    }
}
