package com.nxredux.mobile.runner

import org.junit.Test
import org.junit.Assert.assertEquals

class CoreLoadFailureTest {
    @Test fun the_loaders_reason_is_shown() {
        val recent = listOf("W some core warning", "E core load: dlopen failed: library \"libz.so\" not found")
        assertEquals(
            "The core mgba_libretro_android.so could not be loaded: dlopen failed: library \"libz.so\" not found",
            CoreLoadFailure.message("mgba_libretro_android.so", recent),
        )
    }

    @Test fun the_latest_reason_wins() {
        val recent = listOf("E core load: first", "E core load: second")
        assertEquals("The core a.so could not be loaded: second", CoreLoadFailure.message("a.so", recent))
    }

    @Test fun without_a_reason_the_code_is_shown() {
        assertEquals(
            "The core a.so could not be loaded (ERROR_LOAD_LIBRARY).",
            CoreLoadFailure.message("a.so", listOf("E something else")),
        )
    }
}
