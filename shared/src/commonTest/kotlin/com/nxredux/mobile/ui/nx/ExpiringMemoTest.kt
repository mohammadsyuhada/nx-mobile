package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

/** An installed app's version is looked up once per package per TTL, not per tile per frame. */
class ExpiringMemoTest {
    @Test fun aValueIsKeptForItsTtlThenLoadedAgain() {
        var now = 0L
        var loads = 0
        val memo = ExpiringMemo<String, String?>(ttlMs = 100) { now }
        val load = { _: String -> loads++; if (loads == 1) "v1" else "v2" }
        assertEquals("v1", memo.get("pkg", load))
        now = 99
        assertEquals("v1", memo.get("pkg", load))
        assertEquals(1, loads)
        now = 100
        assertEquals("v2", memo.get("pkg", load))
        assertEquals(2, loads)
    }

    @Test fun aNullValueIsKeptAndKeysAreApart() {
        var loads = 0
        val memo = ExpiringMemo<String, String?>(ttlMs = 100) { 0L }
        repeat(3) { memo.get("gone") { loads++; null } }
        memo.get("other") { loads++; "1" }
        assertEquals(2, loads)
    }
}
