package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WiiControlsTableTest {
    private val table = WiiControlsTable.parse(
        "# GameTDB wiitdb 1, https://www.gametdb.com\n# comment\nRMCE01\tC\nSMNE01\t\nSOUE01\tNM\n\nbad line\n",
    )

    @Test fun exactIdsResolve() {
        assertEquals(WiiControls(classic = true, nunchuk = false, motionPlus = false, upright = false), table.lookup("RMCE01"))
        assertEquals(WiiControls(false, false, false, false), table.lookup("SMNE01"))
    }

    @Test fun anotherRegionFallsBackToTheGameCode() {
        assertEquals(table.lookup("SOUE01"), table.lookup("SOUP01"))
    }

    @Test fun unknownOrMissingIdsAreNull() {
        assertNull(table.lookup("ZZZE01"))
        assertNull(table.lookup(null))
        assertNull(WiiControlsTable.EMPTY.lookup("RMCE01"))
    }
}
