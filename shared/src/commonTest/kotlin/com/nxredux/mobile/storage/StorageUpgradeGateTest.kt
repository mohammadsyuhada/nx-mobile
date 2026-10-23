package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageUpgradeGateTest {
    private val fileHome = FileRefs.of("/storage/emulated/0/NXRedux")
    private val saf = ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux")

    @Test fun freshInstallNotDue() {
        assertNull(StorageUpgradeGate.due(onboardingDone = false, done = false, granted = false, saved = saf))
        assertNull(StorageUpgradeGate.due(onboardingDone = null, done = false, granted = true, saved = null))
    }

    @Test fun doneNotDue() {
        assertNull(StorageUpgradeGate.due(onboardingDone = true, done = true, granted = false, saved = saf))
        assertNull(StorageUpgradeGate.due(onboardingDone = true, done = true, granted = true, saved = fileHome))
    }

    @Test fun fileHomeWithAccessIsQuiet() {
        assertEquals(UpgradeCase.QUIET, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = true, saved = fileHome))
    }

    @Test fun pickerHomeAsks() {
        assertEquals(UpgradeCase.ASK, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = false, saved = saf))
        assertEquals(UpgradeCase.ASK, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = true, saved = saf))
    }

    // A file home with access off is not the media move: it asks for access once, and after Not now it waits quietly.
    @Test fun fileHomeWithoutAccessAsksForItOnce() {
        assertEquals(UpgradeCase.ASK_ACCESS, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = false, saved = fileHome))
        assertEquals(UpgradeCase.QUIET, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = false, saved = fileHome, waiting = true))
    }

    @Test fun noHomeAsks() {
        assertEquals(UpgradeCase.ASK, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = false, saved = null))
        assertEquals(UpgradeCase.ASK, StorageUpgradeGate.due(onboardingDone = true, done = false, granted = true, saved = null))
    }
}
