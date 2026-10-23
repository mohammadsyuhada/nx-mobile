package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeChoiceTest {
    private val home = FileRefs.of("/storage/emulated/0/NX")
    private val media = FileRefs.of("/storage/emulated/0/Android/media/app")
    private val saf = ContentRef("content://tree/primary%3ANX")

    @Test fun fullAccessHomeWins() = assertEquals(home, HomeChoice.effective(home, true, { true }, media))
    @Test fun accessOffFallsBackToMedia() = assertEquals(media, HomeChoice.effective(home, false, { true }, media))
    @Test fun unmountedHomeFallsBackToMedia() = assertEquals(media, HomeChoice.effective(home, true, { false }, media))
    @Test fun noSavedHomeIsMedia() = assertEquals(media, HomeChoice.effective(null, false, { true }, media))
    @Test fun legacyPickerHomeKeptUntilUpgrade() = assertEquals(saf, HomeChoice.effective(saf, false, { true }, media))
}
