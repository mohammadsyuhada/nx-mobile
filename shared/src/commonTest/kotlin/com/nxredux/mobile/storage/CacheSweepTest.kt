package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheSweepTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    private fun victims(vararg entries: CacheEntry, cap: Long = 100, maxAge: Long = 30 * day, keep: Set<String> = emptySet()) =
        CacheSweep.victims(entries.toList(), now, cap, maxAge, keep)

    @Test fun nothing_to_do_under_the_cap_and_fresh() {
        assertEquals(emptyList(), victims(CacheEntry("a", 40, now - day), CacheEntry("b", 50, now - 2 * day)))
        assertEquals(emptyList(), CacheSweep.victims(emptyList(), now, 100, 30 * day, emptySet()))
    }

    @Test fun stale_entries_go_even_under_the_cap() {
        assertEquals(listOf("old"), victims(CacheEntry("old", 1, now - 31 * day), CacheEntry("new", 1, now - 29 * day)))
    }

    @Test fun exactly_max_age_is_not_stale() {
        assertEquals(emptyList(), victims(CacheEntry("a", 1, now - 30 * day)))
        assertEquals(listOf("a"), victims(CacheEntry("a", 1, now - 30 * day - 1)))
    }

    @Test fun over_the_cap_least_recently_used_goes_first_until_it_fits() {
        val v = victims(CacheEntry("new", 40, now - day), CacheEntry("oldest", 40, now - 5 * day), CacheEntry("mid", 40, now - 3 * day))
        assertEquals(listOf("oldest"), v)   // 120 -> 80
        val w = victims(CacheEntry("new", 80, now - day), CacheEntry("oldest", 30, now - 5 * day), CacheEntry("mid", 30, now - 3 * day))
        assertEquals(listOf("oldest", "mid"), w)   // 140 -> 110 -> 80
    }

    @Test fun exactly_the_cap_is_kept() {
        assertEquals(emptyList(), victims(CacheEntry("a", 60, now - day), CacheEntry("b", 40, now - 2 * day)))
    }

    @Test fun stale_bytes_do_not_count_toward_the_cap() {
        // 130 in all, but the stale 40 go first, leaving 90.
        assertEquals(listOf("stale"), victims(CacheEntry("stale", 40, now - 40 * day), CacheEntry("a", 50, now - day), CacheEntry("b", 40, now - 2 * day)))
    }

    @Test fun a_single_entry_larger_than_the_cap_goes() {
        assertEquals(listOf("big"), victims(CacheEntry("big", 500, now)))
    }

    @Test fun kept_entries_are_never_victims_but_their_bytes_count() {
        assertEquals(emptyList(), victims(CacheEntry("big", 500, now - 90 * day), keep = setOf("big")))
        // The kept 80 fill most of the cap, so the unkept 40 must go even though they are newer.
        assertEquals(listOf("other"), victims(CacheEntry("running", 80, now - 5 * day), CacheEntry("other", 40, now - day), keep = setOf("running")))
    }

    @Test fun an_entry_over_the_cap_alone_stays_while_in_use_and_goes_on_a_later_sweep() {
        val disc = CacheEntry("Crash (USA).zip.412345678", 700L shl 20, now)   // a zipped CD image, bigger than the default cap
        val other = CacheEntry("Tetris.zip.20480", 32L shl 10, now - day)
        assertEquals(listOf(other.name), CacheSweep.victims(listOf(disc, other), now, keep = setOf(disc.name)))
        assertEquals(listOf(disc.name), CacheSweep.victims(listOf(disc), now + 1))   // the game has quit
    }

    @Test fun the_default_cap_is_256_mib_and_the_max_age_30_days() {
        assertEquals(256L * 1024 * 1024, CacheSweep.CAP_BYTES)
        assertEquals(30 * day, CacheSweep.MAX_AGE_MILLIS)
        val full = CacheEntry("a", CacheSweep.CAP_BYTES, now)
        assertEquals(emptyList(), CacheSweep.victims(listOf(full), now))
        assertEquals(listOf("a"), CacheSweep.victims(listOf(full.copy(bytes = CacheSweep.CAP_BYTES + 1)), now))
    }

    @Test fun everything_kept_leaves_the_cache_over_the_cap() {
        assertEquals(emptyList(), victims(CacheEntry("a", 80, now - 40 * day), CacheEntry("b", 80, now), keep = setOf("a", "b")))
    }

    @Test fun ties_on_last_use_go_by_name() {
        val t = now - day
        assertEquals(listOf("a", "b"), victims(CacheEntry("c", 40, t), CacheEntry("b", 40, t), CacheEntry("a", 40, t), cap = 40))
    }

    @Test fun a_future_timestamp_counts_as_just_used() {
        assertEquals(listOf("past"), victims(CacheEntry("future", 60, now + day), CacheEntry("past", 60, now - day)))
    }
}
