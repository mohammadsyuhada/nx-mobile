package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals

class MergedLabelsTest {
    private fun labels(vararg rows: Triple<String, String, String>) =
        MergedLabels.labels(rows.map { (name, file, tag) -> MergedLabels.Row(name, file, tag) })

    @Test fun a_name_no_other_row_shares_keeps_its_shown_name() {
        assertEquals(listOf(null, null), labels(Triple("Zelda", "Zelda.sfc", "SFC"), Triple("Mario", "Mario.sfc", "SUPA")))
    }

    @Test fun the_same_file_under_several_tags_is_name_and_tag() {
        assertEquals(
            listOf("Mario World (SFC)", "Mario World (SUPA)", null),
            labels(Triple("Mario World", "Mario World.sfc", "SFC"), Triple("Mario World", "Mario World.sfc", "SUPA"), Triple("Zelda", "Zelda.sfc", "SFC")),
        )
    }

    @Test fun different_files_under_different_tags_are_stem_and_tag() {
        assertEquals(
            listOf("Contra (USA) (SFC)", "Contra (Japan) (SUPA)"),
            labels(Triple("Contra", "Contra (USA).sfc", "SFC"), Triple("Contra", "Contra (Japan).sfc", "SUPA")),
        )
    }

    @Test fun different_files_under_one_tag_are_the_stem() {
        assertEquals(
            listOf("Tetris (USA)", "Tetris (Europe)"),
            labels(Triple("Tetris", "Tetris (USA).gba", "GBA"), Triple("Tetris", "Tetris (Europe).gba", "GBA")),
        )
    }

    @Test fun colliding_stems_keep_the_whole_file_names() {
        assertEquals(
            listOf("Tetris.gb (GB)", "Tetris.gbc (GBC)"),
            labels(Triple("Tetris", "Tetris.gb", "GB"), Triple("Tetris", "Tetris.gbc", "GBC")),
        )
    }

    @Test fun names_match_case_sensitively() {
        assertEquals(listOf(null, null), labels(Triple("Doom", "Doom.sfc", "SFC"), Triple("DOOM", "DOOM.sfc", "SUPA")))
    }

    @Test fun copies_of_one_file_under_one_tag_are_left_to_their_folder_subtitles() {
        assertEquals(listOf(null, null), labels(Triple("Mario", "Mario.gba", "GBA"), Triple("Mario", "Mario.gba", "GBA")))
    }
}
