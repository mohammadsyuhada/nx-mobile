package com.nxredux.mobile.runner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NxStorageSweepTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(root: File, path: String, age: Long) =
        File(root, path).apply { parentFile.mkdirs(); writeText("x"); setLastModified(System.currentTimeMillis() - age) }

    @Test fun sweeps_only_old_write_temps() {
        val root = tmp.newFolder("sd")
        val old = 5 * 60_000L
        val writeTemp = file(root, "Saves/GB/T.srm.1a2b.tmp", old)
        val writeTempFresh = file(root, "Saves/GB/U.srm.3c4d.tmp", 0)
        val other = file(root, ".userdata/shared/GB-gambatte/.T.st0.sync.tmp", old)
        val real = file(root, ".userdata/shared/GB-gambatte/T.st0", old)
        NxStorage.sweepTemps(root)
        assertFalse(writeTemp.exists())
        assertTrue(writeTempFresh.exists()); assertTrue(other.exists()); assertTrue(real.exists())
    }
}
