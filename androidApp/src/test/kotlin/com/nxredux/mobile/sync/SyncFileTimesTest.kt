package com.nxredux.mobile.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncFileTimesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun time_sticks_on_a_normal_folder() { assertTrue(SyncFileTimes.check(tmp.newFolder("Roms"))) }
    @Test fun a_missing_folder_fails() { assertFalse(SyncFileTimes.check(File(tmp.root, "absent"))) }
    @Test fun leaves_no_temp_file() { val d = tmp.newFolder("R"); SyncFileTimes.check(d); assertEquals(0, d.list()!!.size) }
}
