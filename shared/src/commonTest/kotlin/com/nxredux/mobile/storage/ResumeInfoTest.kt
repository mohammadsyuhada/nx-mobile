package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResumeInfoTest {
    @Test fun no_slot_file_means_not_resumable() {
        assertNull(ResumeInfo.slotFrom(null))
        assertFalse(ResumeInfo.NONE.canResume)
    }

    @Test fun valid_slots_are_kept() {
        assertEquals(9, ResumeInfo.slotFrom("9"))
        assertEquals(0, ResumeInfo.slotFrom("0\n"))
        assertEquals(3, ResumeInfo.slotFrom(" 3 "))
        assertTrue(ResumeInfo(3).canResume)
    }

    @Test fun corrupt_slot_files_resume_slot_zero() {
        assertEquals(0, ResumeInfo.slotFrom("abc"))
        assertEquals(0, ResumeInfo.slotFrom("12"))
        assertEquals(0, ResumeInfo.slotFrom("8"))
        assertEquals(0, ResumeInfo.slotFrom("-1"))
        assertEquals(0, ResumeInfo.slotFrom(""))
    }
}
