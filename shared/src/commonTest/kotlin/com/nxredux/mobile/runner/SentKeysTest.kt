package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals

class SentKeysTest {
    @Test fun key_up_repeats_what_its_key_down_sent() {
        val s = SentKeys<Pair<Int, Int>>()
        assertEquals(97, s.down(1 to 99, 97))
        assertEquals(97, s.up(1 to 99, 103))      // the mapping changed mid-press: still releases 97
        assertEquals(103, s.up(1 to 99, 103))     // nothing held any more: the fallback
    }

    @Test fun clear_forgets_everything() {
        val s = SentKeys<String>()
        s.down("x", 1); s.clear()
        assertEquals(5, s.up("x", 5))
    }

    @Test fun keys_are_separate() {
        val s = SentKeys<Pair<Int, Int>>()
        s.down(1 to 96, 10); s.down(2 to 96, 20)
        assertEquals(20, s.up(2 to 96, 0)); assertEquals(10, s.up(1 to 96, 0))
    }

    @Test fun held_reads_without_releasing() {
        val s = SentKeys<Pair<Int, Int>>()
        assertEquals(null, s.held(1 to 96))
        s.down(1 to 96, 97)
        assertEquals(97, s.held(1 to 96))          // a key-repeat sends what the first down recorded
        assertEquals(97, s.up(1 to 96, 0))
        assertEquals(null, s.held(1 to 96))
    }
}
