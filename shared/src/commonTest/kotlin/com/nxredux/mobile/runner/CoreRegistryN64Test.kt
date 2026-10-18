package com.nxredux.mobile.runner

import com.nxredux.mobile.options.CoreConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreRegistryN64Test {
    private val n64 get() = CoreRegistry.forTag("N64")!!

    @Test fun n64_uses_independent_c_buttons_locked_on() {
        assertEquals("True", n64.defaultOptions["mupen64plus-alt-map"])
        assertTrue("mupen64plus-alt-map" in n64.lockedOptions)
        // A .cfg saved before this default stores the core's own "False"; the locked default wins.
        val game = CoreConfig.parse("mupen64plus-alt-map = False\n")
        val r = CoreConfig.resolve(n64.defaultOptions, null, game, forced = n64.lockedOptions)
        assertEquals("True", r.values["mupen64plus-alt-map"])
        assertTrue("mupen64plus-alt-map" in r.locked)
    }
}
