package com.swordfish.libretrodroid

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NxLogTest {
    private val lines = mutableListOf<String>()
    private val flags = mutableListOf<Boolean>()

    @Before fun hooks() {
        NxLog.sink = { priority, tag, msg, _ -> lines += "$priority $tag $msg" }
        NxLog.nativeFlag = { flags += it }
        NxLog.enabled = false
        lines.clear(); flags.clear()
    }

    @After fun restore() {
        NxLog.nativeFlag = { }   // no native library on the JVM
        NxLog.enabled = false
        NxLog.sink = NxLog.logcat
        NxLog.nativeFlag = NxLog.libretroDroidFlag
    }

    @Test fun off_skips_everything_below_an_error() {
        NxLog.v("T", "v"); NxLog.d("T", "d"); NxLog.i("T", "i"); NxLog.w("T", "w")
        assertEquals(emptyList<String>(), lines)
    }

    @Test fun errors_print_when_off() {
        NxLog.e("T", "boom")
        assertEquals(listOf("${NxLog.ERROR} T boom"), lines)
    }

    @Test fun on_prints_every_level() {
        NxLog.enabled = true
        NxLog.v("T", "v"); NxLog.d("T", "d"); NxLog.i("T", "i"); NxLog.w("T", "w"); NxLog.e("T", "e")
        assertEquals(
            listOf("${NxLog.VERBOSE} T v", "${NxLog.DEBUG} T d", "${NxLog.INFO} T i", "${NxLog.WARN} T w", "${NxLog.ERROR} T e"),
            lines,
        )
    }

    // The first set loads the native library (on the UI thread, from the Settings toggle): a library that fails to load
    // must not crash the app; the Kotlin side still follows the setting.
    @Test fun a_native_library_that_will_not_load_is_logged_not_thrown() {
        NxLog.nativeFlag = { throw UnsatisfiedLinkError("dlopen failed") }
        NxLog.enabled = true
        assertEquals(true, NxLog.enabled)
        assertEquals(1, lines.count { it.startsWith("${NxLog.ERROR} ") && it.contains("native") })
        NxLog.i("T", "still on")
        assertEquals("${NxLog.INFO} T still on", lines.last())
    }

    @Test fun setting_the_flag_sets_the_native_flag() {
        NxLog.enabled = true
        NxLog.enabled = false
        assertEquals(listOf(true, false), flags)
    }
}
