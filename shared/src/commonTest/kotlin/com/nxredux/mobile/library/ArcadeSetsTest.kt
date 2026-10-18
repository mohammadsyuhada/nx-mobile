package com.nxredux.mobile.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArcadeSetsTest {
    private val table = ArcadeSets.parse(
        "mslug2\tMetal Slug 2 - Super Vehicle-001/II (NGM-2410 ~ NGH-2410)\t\tneogeo\t\n" +
            "neogeo\tNeo Geo\t\t\t1\n" +
            "sf2ce\tStreet Fighter II' - Champion Edition (street fighter 2' 920313 etc)\t\t\t\n" +
            "sf2cebltw\tStreet Fighter II' - Champion Edition (bootleg)\tsf2ce\tsf2ce\t\n" +
            "kof98a\tThe King of Fighters '98 (alt)\tkof98\tkof98\t\n" +
            "kof98\tThe King of Fighters '98\t\tneogeo\t",
    )

    @Test fun titles_and_bios() {
        assertEquals("Metal Slug 2 - Super Vehicle-001/II (NGM-2410 ~ NGH-2410)", table.title("mslug2"))
        assertEquals("Metal Slug 2 - Super Vehicle-001/II (NGM-2410 ~ NGH-2410)", table.title("MSLUG2"))
        assertTrue(table.isBios("neogeo"))
        assertFalse(table.isBios("mslug2"))
        assertNull(table.title("nope"))
    }
    @Test fun chain_follows_romof_to_the_bios() {
        assertEquals(listOf("neogeo"), table.chain("mslug2"))
        assertEquals(listOf("kof98", "neogeo"), table.chain("kof98a"))
        assertEquals(listOf("sf2ce"), table.chain("sf2cebltw"))
        assertEquals(emptyList(), table.chain("sf2ce"))
        assertEquals(emptyList(), table.chain("unknown"))
    }
    @Test fun chain_and_bios_ignore_case() {
        assertEquals(listOf("kof98", "neogeo"), table.chain("KOF98A"))
        assertEquals(table.chain("kof98a"), table.chain("Kof98A"))
        assertTrue(table.isBios("NEOGEO"))
        assertTrue(table.isBios("NeoGeo"))
    }
    @Test fun chain_stops_on_a_loop() {
        val loop = ArcadeSets.parse("a\tA\t\tb\t\nb\tB\t\ta\t\nc\tC\t\tc\t")
        assertEquals(listOf("b"), loop.chain("a"))
        assertEquals(emptyList(), loop.chain("c"))
    }
    @Test fun malformed_lines_are_skipped() {
        val t = ArcadeSets.parse("\n just text\nok\tOK\t\t\t\n\ttitle-without-name\t\t\t\n")
        assertEquals("OK", t.title("ok"))
        assertFalse(t.isEmpty)
    }
    @Test fun empty_table_knows_nothing() {
        assertTrue(ArcadeSets.EMPTY.isEmpty)
        assertNull(ArcadeSets.EMPTY.title("mslug"))
        assertEquals(emptyList(), ArcadeSets.EMPTY.chain("mslug"))
        assertTrue(ArcadeSets.parse("").isEmpty)
    }
}
