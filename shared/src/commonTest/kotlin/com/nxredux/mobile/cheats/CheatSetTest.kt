package com.nxredux.mobile.cheats

import com.nxredux.mobile.options.ConfigSource
import com.nxredux.mobile.options.FrontendOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheatSetTest {
    private val set = CheatSet(
        listOf(
            CheatEntry("Infinite Lives", "01", "Infinite Lives", enabled = false),
            CheatEntry("Max Score | Bonus ", "02", "Max Score | Bonus ", enabled = true),
            CheatEntry("No code", "", "No code", enabled = true),
        ),
        expected = "Tetris.gb.cht",
    )

    @Test fun toggle_flips_one_entry_and_keeps_the_rest() {
        val next = set.toggle(0, true)
        assertEquals(listOf(true, true, true), next.entries.map { it.enabled })
        assertEquals(listOf(true, false, true), next.toggle(1, false).entries.map { it.enabled })
        assertEquals(set, set.toggle(99, true))   // out of range: unchanged
    }

    @Test fun enabled_keys_are_normalised() {
        assertEquals(listOf("Max Score / Bonus", "No code"), set.enabledKeys())
        assertEquals("Max Score / Bonus|No code", CheatSet.encode(set.enabledKeys()))
        assertEquals(listOf("Max Score / Bonus", "No code"), CheatSet.decode("Max Score / Bonus|No code"))
        assertEquals(emptyList(), CheatSet.decode(""))
        assertEquals(listOf("a"), CheatSet.decode(" a | | "))
    }

    @Test fun enabled_codes_are_contiguous() {
        // The core gets only enabled cheats with a code, in order, so index i is always the i-th of these.
        assertEquals(listOf("02"), set.enabledCodes())
        assertEquals(listOf("01", "02"), set.toggle(0, true).enabledCodes())
    }

    @Test fun withSaved_overrides_the_file_seeds_both_ways() {
        val next = set.withSaved(listOf("Infinite Lives"))
        assertEquals(listOf(true, false, false), next.entries.map { it.enabled })
        assertEquals(listOf(false, false, false), set.withSaved(emptyList()).entries.map { it.enabled })
        // Matching uses the same normalisation as encode.
        assertEquals(listOf(false, true, false), set.withSaved(listOf("Max Score / Bonus")).entries.map { it.enabled })
    }

    @Test fun withSaved_ignores_unknown_keys() {
        val next = set.withSaved(listOf("Gone From File", "Infinite Lives"))
        assertEquals(listOf(true, false, false), next.entries.map { it.enabled })
        assertEquals(listOf("Infinite Lives"), next.enabledKeys())
    }

    @Test fun savedFrom_reads_the_game_config_only() {
        val values = mapOf(FrontendOptions.CHEATS to "a|b", "nx_overlay" to "None")
        assertEquals(listOf("a", "b"), CheatSet.savedFrom(values, ConfigSource.GAME))
        assertEquals(emptyList(), CheatSet.savedFrom(mapOf(FrontendOptions.CHEATS to ""), ConfigSource.GAME))
        assertNull(CheatSet.savedFrom(values, ConfigSource.CONSOLE))
        assertNull(CheatSet.savedFrom(values, ConfigSource.NONE))
        assertNull(CheatSet.savedFrom(emptyMap(), ConfigSource.GAME))
    }

    @Test fun saved_state_distinguishes_same_named_cheats_from_merged_sources() {
        val merged = CheatSet(
            listOf(
                CheatEntry("Infinite Lives", "01", "Infinite Lives", enabled = false),
                CheatEntry("Infinite Lives", "02", "(GameShark) Infinite Lives", enabled = true),
            ),
            expected = "Contra (USA).gb.cht",
        )
        assertEquals(listOf("(GameShark) Infinite Lives"), merged.enabledKeys())
        assertEquals(listOf(false, true), merged.withSaved(listOf("(GameShark) Infinite Lives")).entries.map { it.enabled })
        assertEquals(listOf(true, false), merged.withSaved(listOf("Infinite Lives")).entries.map { it.enabled })
    }

    private val dupes = CheatSet(
        listOf(
            CheatEntry("Max HP", "01", "Max HP", enabled = false),
            CheatEntry("Max HP", "02", "Max HP", enabled = false),
            CheatEntry("Max HP ", "03", "Max HP ", enabled = false),
            CheatEntry("Other", "04", "Other", enabled = false),
        ),
        expected = "Game.cht",
    )

    @Test fun cheats_sharing_a_description_are_saved_and_restored_one_by_one() {
        val second = dupes.toggle(1, true)
        assertEquals(listOf("Max HP#2"), second.enabledKeys())   // the nth repeat of a desc (after normalising) is desc#n
        assertEquals(listOf(false, true, false, false), dupes.withSaved(second.enabledKeys()).entries.map { it.enabled })
        val third = dupes.toggle(2, true).toggle(3, true)
        assertEquals(listOf("Max HP#3", "Other"), third.enabledKeys())
        assertEquals(listOf(false, false, true, true), dupes.withSaved(CheatSet.decode(CheatSet.encode(third.enabledKeys()))).entries.map { it.enabled })
    }

    @Test fun a_saved_plain_description_still_enables_the_first_of_its_repeats() {
        // Saved before repeats were keyed apart: the bare desc names the first one only.
        assertEquals(listOf(true, false, false, false), dupes.withSaved(listOf("Max HP")).entries.map { it.enabled })
        assertEquals(listOf("Max HP"), dupes.toggle(0, true).enabledKeys())
    }

    @Test fun ram_patches_go_to_the_frontend_not_the_core() {
        val patch = MemoryPatch(address = 1, value = 2, size = 3, type = 1)
        val mixed = CheatSet(
            listOf(
                CheatEntry("Code", "01", "Code", enabled = true),
                CheatEntry("Ram", "", "Ram", enabled = true, mem = patch),
                CheatEntry("Ram off", "", "Ram off", enabled = false, mem = patch.copy(value = 9)),
            ),
            "",
        )
        assertEquals(listOf("01"), mixed.enabledCodes())
        assertEquals(listOf(patch), mixed.enabledPatches())
    }

    @Test fun a_core_without_codes_keeps_only_ram_patches() {
        val patch = MemoryPatch(address = 1, value = 2, size = 3, type = 1)
        val mixed = CheatSet(
            listOf(CheatEntry("Code", "01", "Code", enabled = true), CheatEntry("Ram", "", "Ram", enabled = false, mem = patch)),
            "Game.cht",
        )
        assertEquals(CheatSet(listOf(mixed.entries[1]), "Game.cht"), mixed.ramPatchesOnly())
    }
}
