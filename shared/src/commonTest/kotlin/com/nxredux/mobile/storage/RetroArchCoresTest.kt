package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetroArchCoresTest {
    private val sega = setOf("md", "sms", "gg", "segacd")

    @Test
    fun core_folder_ignores_case() {
        assertEquals("mgba", RetroArchCores.coreOf("mGBA")?.id)
        assertEquals("mgba", RetroArchCores.coreOf("MGBA")?.id)
        assertEquals("genesis_plus_gx", RetroArchCores.coreOf("Genesis Plus GX")?.id)
        assertEquals("mednafen_supafaust", RetroArchCores.coreOf("Beetle Supafaust")?.id)
        assertNull(RetroArchCores.coreOf("Snes9x 2010"))
    }

    @Test
    fun clue_from_tag_or_core() {
        assertEquals(listOf("gba"), RetroArchCores.consolesOf("GBA")!!.map { it.id })
        assertEquals(listOf("gba"), RetroArchCores.consolesOf("mGBA")!!.map { it.id })
        assertTrue(RetroArchCores.consolesOf("Genesis Plus GX")!!.map { it.id }.containsAll(sega))
        assertEquals(
            RetroArchCores.consolesOf("Genesis Plus GX")!!.map { it.id }.toSet(),
            RetroArchCores.consolesOf("GPGX")!!.map { it.id }.toSet(),
        )
        assertNull(RetroArchCores.consolesOf("My saves"))
    }

    @Test
    fun tags_of_core() {
        assertTrue("MGBA" in RetroArchCores.tagsOf(RetroArchCores.coreOf("mGBA")!!))
    }
}
