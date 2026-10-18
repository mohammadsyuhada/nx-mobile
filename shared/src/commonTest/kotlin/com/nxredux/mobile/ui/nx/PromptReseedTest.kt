package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A prompt field re-seeds only when the host's value itself changes ([nxPromptReseeds]). */
class PromptReseedTest {
    @Test fun aHostLaggingAKeystrokeBehindDoesNotUndoTyping() {
        assertFalse(nxPromptReseeds(fieldText = "ab", last = "a", value = "a"))    // the host still holds the old text
        assertFalse(nxPromptReseeds(fieldText = "ab", last = "a", value = "ab"))   // then echoes the edit
    }

    @Test fun aValueTheHostChangedItselfReseeds() {
        assertTrue(nxPromptReseeds(fieldText = "ab", last = "ab", value = ""))          // cleared
        assertTrue(nxPromptReseeds(fieldText = "Mario", last = "Mario", value = "Zelda"))
    }
}
