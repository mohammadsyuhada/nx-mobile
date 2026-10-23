package com.nxredux.mobile.cheats

import kotlin.test.Test
import kotlin.test.assertEquals

class DolphinCheatsTest {
    private val builtIn = """
        cheats = 2
        cheat0_desc = "Infinite Health"
        cheat0_code = "AAAA0000 BBBB1111"
        cheat0_enable = true
        cheat1_desc = "Max Coins"
        cheat1_code = "CCCC2222 DDDD3333"
        cheat1_enable = false
    """.trimIndent()

    private fun player(vararg e: CheatEntry) = CheatSet(e.toList(), "Game.cht")
    private fun entry(desc: String, code: String, enabled: Boolean = false) = CheatEntry(desc, code, desc, enabled)

    @Test fun built_in_first_then_the_players() {
        val set = DolphinCheats.combine(builtIn, player(entry("Mine", "EEEE4444 FFFF5555", true)))
        assertEquals(listOf("Infinite Health", "Max Coins", "Mine"), set.entries.map { it.desc })
        assertEquals(listOf("Built-in: Infinite Health", "Built-in: Max Coins", "Mine"), set.entries.map { it.info })
        assertEquals(listOf(false, false, true), set.entries.map { it.enabled })   // the file's own enable is ignored
        assertEquals("AAAA0000 BBBB1111", set.entries[0].code)
        assertEquals("Game.cht", set.expected)
    }

    @Test fun an_ar_desc_gives_an_ar_code() {
        val set = DolphinCheats.combine(null, player(entry("[AR] Lives", "0011 2233"), entry("Plain", "4455 6677")))
        assertEquals(listOf("ar:0011 2233", "4455 6677"), set.entries.map { it.code })
    }

    @Test fun ram_patches_with_no_code_are_all_kept() {
        val a = CheatEntry("A", "", "A", false, mem = MemoryPatch(address = 0x10, value = 1))
        val b = CheatEntry("B", "", "B", false, mem = MemoryPatch(address = 0x20, value = 2))
        assertEquals(listOf("A", "B"), DolphinCheats.combine(null, player(a, b, a)).entries.map { it.desc })
    }

    @Test fun duplicate_codes_are_kept_once() {
        val set = DolphinCheats.combine(builtIn, player(entry("Same", "AAAA0000 BBBB1111"), entry("Twice", "9999 9999"), entry("Twice again", "9999 9999")))
        assertEquals(listOf("Infinite Health", "Max Coins", "Twice"), set.entries.map { it.desc })
    }

    @Test fun no_built_in_text_gives_the_players_set() {
        val mine = player(entry("Mine", "1111 2222"))
        assertEquals(mine, DolphinCheats.combine(null, mine))
    }

    @Test fun unparseable_built_in_text_gives_the_players_set() {
        val mine = player(entry("Mine", "1111 2222"))
        assertEquals(mine, DolphinCheats.combine("not a cht file", mine))
    }

    @Test fun refresh_adds_the_built_in_list_keeping_toggles_and_saved_built_ins() {
        val mine = player(entry("Mine", "1111 2222"), entry("Other", "3333 4444"))
        // Toggled meanwhile: Mine on. Saved: Other (a player key, superseded by the toggles) and a built-in one.
        val set = DolphinCheats.refresh(builtIn, mine, enabledKeys = listOf("Mine"), saved = listOf("Other", "${DolphinCheats.BUILT_IN} Max Coins"))
        assertEquals(listOf("Infinite Health", "Max Coins", "Mine", "Other"), set.entries.map { it.desc })
        assertEquals(listOf(false, true, true, false), set.entries.map { it.enabled })
    }

    @Test fun built_in_file_drops_the_extension() {
        assertEquals("/data/files/cheats/dolphin-emu/Super Game.cht", DolphinCheats.builtInFile("/data/files", "Super Game.rvz"))
        assertEquals("/f/cheats/dolphin-emu/a.b.cht", DolphinCheats.builtInFile("/f", "a.b.iso"))
    }
}
