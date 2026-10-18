package com.nxredux.mobile.cheats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheatFileTest {
    // Shape of a libretro-database file (Nintendo - Game Boy).
    private val sample = """
        cheats = 3

        cheat0_desc = "Infinite Lives"
        cheat0_code = "0109C0C1"
        cheat0_enable = false

        cheat1_desc = "Start On Level 9 (\"Type A\")"
        cheat1_code = "0109E0C2+010AE1C2"
        cheat1_enable = true

        cheat2_desc = "Max Score &quot;Bonus&quot;"
        cheat2_code = "01FFC5C1"
        cheat2_enable = false
    """.trimIndent()

    @Test fun parses_desc_code_and_enable_in_index_order() {
        assertEquals(
            listOf(
                Cheat("Infinite Lives", "0109C0C1", false),
                Cheat("Start On Level 9 (\"Type A\")", "0109E0C2+010AE1C2", true),
                Cheat("Max Score \"Bonus\"", "01FFC5C1", false),
            ),
            CheatFile.parse(sample),
        )
    }

    @Test fun count_line_must_come_first_and_be_positive() {
        assertNull(CheatFile.parse(""))
        assertNull(CheatFile.parse("cheat0_desc = \"x\"\ncheat0_code = \"y\"\ncheats = 1\n"))
        assertNull(CheatFile.parse("cheats = 0\n"))
        assertNull(CheatFile.parse("cheats = abc\n"))
        assertEquals(listOf(Cheat("x", "y")), CheatFile.parse("\n\n  cheats=1\ncheat0_desc = \"x\"\ncheat0_code = \"y\"\n"))
    }

    @Test fun a_quoted_count_is_read() {   // libretro's PSP files: cheats = "213"
        val text = "cheats = \"2\"\n\ncheat0_desc = \"x\"\ncheat0_code =  \"_L 0x1 0x2\"\ncheat1_desc = \"y\"\ncheat1_code = \"z\"\n"
        assertEquals(listOf(Cheat("x", "_L 0x1 0x2"), Cheat("y", "z")), CheatFile.parse(text))
        assertNull(CheatFile.parse("cheats = \"2\n"))   // half-quoted
        assertNull(CheatFile.parse("cheats = \"0\"\n"))
    }

    @Test fun crlf_and_bom_are_tolerated() {
        val text = "\uFEFFcheats = 1\r\ncheat0_desc = \"Lives\"\r\ncheat0_code = \"01\"\r\ncheat0_enable = true\r\n"
        assertEquals(listOf(Cheat("Lives", "01", true)), CheatFile.parse(text))
    }

    @Test fun enable_is_a_bare_case_insensitive_boolean() {
        val base = "cheats = 1\ncheat0_desc = \"d\"\ncheat0_code = \"c\"\n"
        assertEquals(true, CheatFile.parse(base + "cheat0_enable = TRUE\n")!![0].enabled)
        assertEquals(false, CheatFile.parse(base + "cheat0_enable = False\n")!![0].enabled)
        // Quoted or garbage: the line is skipped, the cheat stays disabled.
        assertEquals(false, CheatFile.parse(base + "cheat0_enable = \"true\"\n")!![0].enabled)
        assertEquals(false, CheatFile.parse(base + "cheat0_enable = yes\n")!![0].enabled)
    }

    @Test fun other_keys_and_out_of_range_indices_are_ignored() {
        val text = """
            cheats = 1
            cheat0_desc = "d"
            cheat0_code = "c"
            cheat0_address = 1234
            cheat0_handler = 1
            cheat0_big_endian = false
            cheat1_desc = "out of range"
            cheat1_code = "zz"
            cheat7_enable = true
        """.trimIndent()
        assertEquals(listOf(Cheat("d", "c")), CheatFile.parse(text))
    }

    @Test fun a_cheat_without_a_code_is_dropped() {
        val text = "cheats = 3\ncheat0_desc = \"a\"\ncheat0_code = \"1\"\ncheat1_desc = \"no code\"\ncheat2_desc = \"c\"\ncheat2_code = \"3\"\n"
        assertEquals(listOf(Cheat("a", "1"), Cheat("c", "3")), CheatFile.parse(text))
        // Empty desc or code counts as missing.
        assertEquals(emptyList(), CheatFile.parse("cheats = 1\ncheat0_desc = \"\"\ncheat0_code = \"1\"\n"))
        assertEquals(emptyList(), CheatFile.parse("cheats = 1\ncheat0_desc = \"a\"\ncheat0_code = \"\"\n"))
    }

    @Test fun a_bad_line_is_skipped_not_fatal() {
        val text = "cheats = 2\ncheat0_desc = \"unterminated\ncheat0_code = \"1\"\ncheat1_desc = \"ok\"\ncheat1_code = \"2\"\n"
        assertEquals(listOf(Cheat("ok", "2")), CheatFile.parse(text))
        // A value without quotes is a bad line too.
        assertEquals(listOf(Cheat("ok", "2")), CheatFile.parse("cheats = 2\ncheat0_desc = bare\ncheat0_code = \"1\"\ncheat1_desc = \"ok\"\ncheat1_code = \"2\"\n"))
    }

    @Test fun a_line_over_510_bytes_is_skipped() {
        val long = "cheat0_code = \"" + "A".repeat(600) + "\""
        assertEquals(emptyList(), CheatFile.parse("cheats = 1\ncheat0_desc = \"d\"\n$long\n"))
        val fits = "cheat0_code = \"" + "A".repeat(490) + "\""
        assertEquals(490, CheatFile.parse("cheats = 1\ncheat0_desc = \"d\"\n$fits\n")!![0].code.length)
    }

    @Test fun duplicate_keys_last_wins_and_code_is_verbatim() {
        val text = "cheats = 1\ncheat0_desc = \"first\"\ncheat0_desc = \"second\"\ncheat0_code = \" 01 + 02 \"\n"
        assertEquals(listOf(Cheat("second", " 01 + 02 ")), CheatFile.parse(text))
    }

    @Test fun count_is_capped() {
        val text = "cheats = 999999\ncheat0_desc = \"d\"\ncheat0_code = \"c\"\ncheat5000_desc = \"far\"\ncheat5000_code = \"x\"\n"
        assertEquals(listOf(Cheat("d", "c")), CheatFile.parse(text))
    }
}
