package com.nxredux.mobile.cheats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheatMatchTest {
    private val gb = setOf("gb", "gbc", "dmg", "zip")

    @Test fun display_name_strips_extensions_and_trailing_groups() {
        assertEquals("Super Example World", CheatMatch.displayName("Super Example World (USA).zip"))
        assertEquals("Super Example World", CheatMatch.displayName("Super Example World (USA) [!].7z"))
        assertEquals("Super Example World", CheatMatch.displayName("Super Example World (USA) (Rev 1).rar"))
        assertEquals("Game", CheatMatch.displayName("Game.p8.png"))          // repeated short extensions
        assertEquals("Tetris", CheatMatch.displayName("Tetris"))
        assertEquals("(Weird)", CheatMatch.displayName("(Weird).gb"))         // never nuked to nothing
        assertEquals("a.longext", CheatMatch.displayName("a.longext"))        // 7-char extension stays
        assertTrue(CheatMatch.displayName(" (USA).gb").isNotEmpty())
        assertTrue(CheatMatch.displayName("   ").isNotEmpty())
    }

    @Test fun candidates_follow_minarch_order() {
        assertEquals(
            listOf(
                "Tetris (World) (Rev 1).gb.cht",
                "Tetris (World) (Rev 1).dmg.cht",
                "Tetris (World) (Rev 1).gbc.cht",
                "Tetris (World) (Rev 1).cht",
                "Tetris.cht",
            ),
            CheatMatch.candidates("Tetris (World) (Rev 1).gb", "Tetris", gb),
        )
    }

    @Test fun candidates_try_the_full_rom_stem_before_the_display_name() {
        // A GoodTools-style cheat named after the whole stem still matches exactly with the title cleaned.
        val c = CheatMatch.candidates("Tetris [!].gb", "Tetris", gb)
        assertEquals(listOf("Tetris [!].gb.cht", "Tetris [!].dmg.cht", "Tetris [!].gbc.cht", "Tetris [!].cht", "Tetris.cht"), c)
    }

    @Test fun candidates_for_a_zip_try_the_inner_extensions_and_the_title() {
        assertEquals(
            listOf("1941.zip.cht", "1941.dmg.cht", "1941.gb.cht", "1941.gbc.cht", "1941.cht", "1941: Counter Attack.cht"),
            CheatMatch.candidates("1941.zip", "1941: Counter Attack", gb),
        )
    }

    @Test fun candidates_without_an_extension_skip_the_extension_variants() {
        assertEquals(listOf("Tetris.cht"), CheatMatch.candidates("Tetris", "Tetris", gb))
        assertEquals(listOf("Tetris.cht"), CheatMatch.candidates("Tetris", "", gb))
    }

    // Ported from nx-redux tests/test_cheat_match.c.
    @Test fun exact_single_match_is_selected() {
        val files = listOf("Advance Wars (USA, Europe) (Code Breaker).cht")
        assertEquals(files, CheatMatch.select("Advance Wars", "Advance Wars (USA, Europe)", files))
    }

    @Test fun different_stems_are_rejected() {
        val files = listOf("Super Mario World (USA).cht", "Super Mario Bros (USA).cht")
        assertEquals(emptyList(), CheatMatch.select("Super Mario", "Super Mario (USA)", files))
    }

    @Test fun matching_region_is_preferred() {
        val files = listOf("Sonic (Japan).cht", "Sonic (USA).cht")
        assertEquals(listOf("Sonic (USA).cht"), CheatMatch.select("Sonic", "Sonic (USA)", files))
    }

    @Test fun same_region_variants_are_merged_in_input_order() {
        val files = listOf("Contra (USA) (GameShark).cht", "Contra (USA) (Code Breaker).cht", "Contra (USA).cht")
        assertEquals(
            listOf("Contra (USA).cht", "Contra (USA) (GameShark).cht", "Contra (USA) (Code Breaker).cht"),
            CheatMatch.select("Contra", "Contra (USA)", files),
        )
    }

    @Test fun variants_returns_the_same_group_no_source_first() {
        val files = listOf("Contra (USA) (GameShark).cht", "Contra (USA).cht", "Contra (Japan).cht", "Contra (USA) (Code Breaker).cht")
        assertEquals(
            listOf("Contra (USA).cht", "Contra (USA) (GameShark).cht", "Contra (USA) (Code Breaker).cht"),
            CheatMatch.variants("Contra (USA) (GameShark).cht", files),
        )
    }

    @Test fun plain_file_is_preferred_for_a_region_less_rom() {
        val files = listOf("Zelda.cht", "Zelda (USA).cht")
        assertEquals(listOf("Zelda.cht"), CheatMatch.select("Zelda", "Zelda", files))
    }

    @Test fun fewer_extra_qualifiers_win_then_the_smallest_key() {
        val files = listOf("Tetris (World) (Rev 1).cht", "Tetris (World).cht", "Tetris (Japan).cht")
        assertEquals(listOf("Tetris (World).cht"), CheatMatch.select("Tetris", "Tetris (World).gb", files))
        // Same overlap, same extras: the lexically smaller key ("japan" < "world") wins.
        assertEquals(listOf("Tetris (Japan).cht"), CheatMatch.select("Tetris", "Tetris.gb", listOf("Tetris (World).cht", "Tetris (Japan).cht")))
    }

    @Test fun group_key_tie_break_matches_the_c_code() {
        // "Game|rev 1," < "Game|rev," because ' ' < ','.
        val files = listOf("Game (Rev).cht", "Game (Rev 1).cht")
        assertEquals(listOf("Game (Rev 1).cht"), CheatMatch.select("Game", "Game.gb", files))
    }

    @Test fun stem_match_is_case_insensitive_and_non_cht_files_are_ignored() {
        val files = listOf("tetris (World).CHT", "Tetris (World).txt", "Tetris (World).cht.bak")
        assertEquals(listOf("tetris (World).CHT"), CheatMatch.select("Tetris", "Tetris.gb", files))
    }

    @Test fun metacharacters_in_the_name_are_literal() {
        val files = listOf("Q*bert (USA).cht", "Qbert (USA).cht", "Mr. Do! (USA).cht")
        assertEquals(listOf("Q*bert (USA).cht"), CheatMatch.select("Q*bert", "Q*bert (USA).gb", files))
        assertEquals(listOf("Mr. Do! (USA).cht"), CheatMatch.select("Mr. Do!", "Mr. Do! (USA).gb", files))
    }

    @Test fun source_label_keeps_case_and_is_empty_for_regions() {
        assertEquals("GameShark", CheatMatch.sourceLabel("Contra (USA) (GameShark).cht"))
        assertEquals("Code Breaker", CheatMatch.sourceLabel("Contra (USA, Europe) (Code Breaker).cht"))
        assertEquals("", CheatMatch.sourceLabel("Zelda (USA).cht"))
    }

    // libretro's PSP set names files "Name [US] [ULUS-10041].cht" (nx-redux #257): the stem ends at " [" and the
    // region code compares with the ROM's No-Intro region.
    @Test fun psp_bracket_region_matches_the_rom_region() {
        val files = listOf(
            "Grand Theft Auto - Liberty City Stories [EU] [ULES-00151].cht",
            "Grand Theft Auto - Liberty City Stories [JP] [ULJM-05255].cht",
            "Grand Theft Auto - Liberty City Stories [US] [ULUS-10041].cht",
        )
        val name = "Grand Theft Auto - Liberty City Stories"
        assertEquals(listOf(files[2]), CheatMatch.select(name, "$name (USA).iso", files))
        assertEquals(listOf(files[0]), CheatMatch.select(name, "$name (Europe) (En,Fr,De,Es,It).iso", files))
    }

    @Test fun psp_multi_region_code_matches_either_region() {
        val files = listOf("Lumines [JP] [ULJM-05013].cht", "Lumines [US-EU] [ULUS-10046].cht")
        assertEquals(listOf(files[1]), CheatMatch.select("Lumines", "Lumines (Europe)", files))
    }

    @Test fun psp_serial_in_the_rom_name_picks_that_file() {
        val files = listOf("Daxter [US] [UCUS-98618].cht", "Daxter [US] [UCUS-98646].cht")
        assertEquals(listOf(files[1]), CheatMatch.select("Daxter", "Daxter (USA) [UCUS-98646]", files))
    }

    @Test fun psp_retail_serial_beats_a_homebrew_id_of_the_same_region() {
        val files = listOf("Patapon 2 [US] [HB4B12D3BC].cht", "Patapon 2 [US] [UCUS-98732].cht", "Patapon 2 [EU] [UCES-01177].cht")
        assertEquals(listOf(files[1]), CheatMatch.select("Patapon 2", "Patapon 2 (USA) (En,Fr,Es).iso", files))
    }
}
