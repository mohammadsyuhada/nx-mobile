package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NdsDsvSaveTest {
    private val save = ByteArray(512) { (it % 251).toByte() }

    private fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
    private fun u32At(b: ByteArray, at: Int) = (0 until 4).sumOf { (b[at + it].toInt() and 0xFF) shl (8 * it) }

    /** DeSmuME's footer: its 82-byte text, size, padded size, type, address size, memory size, version 0, the magic. */
    private fun footerOf(size: Int, padSize: Int = size) =
        "|<--Snip above here to create a raw sav by excluding this DeSmuME savedata footer:".encodeToByteArray() +
            le32(size) + le32(padSize) + le32(1) + le32(2) + le32(size) + le32(0) + NdsDsvSave.DSV_MAGIC.encodeToByteArray()

    private val footer = footerOf(512)
    /** The runner's battery-save candidates for `Game.nds` (NxPaths.saveRamCandidates, file names only). */
    private val candidates = listOf("Game.srm", "Game.nds.sav", "Game.sav")
    private fun plan(files: Map<String, ByteArray>) = NdsDsvSave.plan("Game", candidates, files.keys) { files[it] }

    @Test fun the_footer_is_122_bytes_ending_in_the_magic() {
        assertEquals(122, footer.size)
        assertContentEquals(save, NdsDsvSave.stripDsvFooter(save + footer))
        assertContentEquals(footer, NdsDsvSave.footerOf(save + footer))
        assertNull(NdsDsvSave.footerOf(save))
        assertNull(NdsDsvSave.stripDsvFooter(save))
    }

    @Test fun a_lone_dsv_is_played_in_place_with_its_footer_kept() {
        val p = plan(mapOf("Game.dsv" to save + footer))!!
        assertEquals("Game.dsv", p.name)
        assertContentEquals(save, p.bytes)
        assertContentEquals(footer, p.footer)
        val changed = ByteArray(512) { 7 }
        assertContentEquals(changed + footer, NdsDsvSave.join(changed, p.footer))
    }

    /** The real files: a 256 KiB save is a 262266-byte `.dsv`, 512 KiB 524410, 512 bytes 634 (save + 122-byte footer). */
    @Test fun the_footer_records_the_save_size_at_82_and_86() {
        for (n in listOf(512, 262_144, 524_288)) {
            val file = ByteArray(n) + footerOf(n)
            assertEquals(n + 122, file.size)
            val f = NdsDsvSave.footerOf(file)!!
            assertEquals(n, u32At(f, 82))
            assertEquals(n, u32At(f, 86))
        }
    }

    /** A write of another size than the footer records rewrites the size fields, nothing else. */
    @Test fun a_write_of_another_size_rewrites_the_footers_sizes() {
        val bigger = ByteArray(8192) { 3 }
        val out = NdsDsvSave.join(bigger, footer)
        assertEquals(8192 + 122, out.size)
        assertContentEquals(bigger, out.copyOfRange(0, 8192))
        val f = out.copyOfRange(8192, out.size)
        assertEquals(8192, u32At(f, 82))
        assertEquals(8192, u32At(f, 86))
        for (i in f.indices) if (i !in 82 until 90) assertEquals(footer[i], f[i], "footer byte $i")
        // The loaded footer itself is left as it was.
        assertEquals(512, u32At(footer, 82))
        // Same size: the footer as loaded, a padded one (size below its padded size) included.
        assertContentEquals(save + footer, NdsDsvSave.join(save, footer))
        val padded = footerOf(500, padSize = 512)
        assertContentEquals(save + padded, NdsDsvSave.join(save, padded))
    }

    @Test fun a_dsv_without_the_footer_is_loaded_and_written_raw() {
        val p = plan(mapOf("Game.dsv" to save))!!
        assertContentEquals(save, p.bytes)
        assertEquals(0, p.footer.size)
        assertContentEquals(save, NdsDsvSave.join(save, p.footer))
    }

    @Test fun a_save_under_any_candidate_name_wins() {
        assertNull(plan(mapOf("Game.srm" to save, "Game.dsv" to save + footer)))
        assertNull(plan(mapOf("Game.sav" to save, "Game.dsv" to save + footer)))
        assertNull(plan(mapOf("Game.nds.sav" to save, "Game.dsv" to save + footer)))
        // an unreadable candidate may hold a save: it wins too
        assertNull(NdsDsvSave.plan("Game", candidates, setOf("Game.nds.sav", "Game.dsv")) { if (it == "Game.dsv") save + footer else null })
        // a file outside the candidates does not
        assertNotNull(plan(mapOf("Other.sav" to save, "Game.dsv" to save + footer)))
    }

    @Test fun nothing_usable_plans_nothing() {
        assertNull(plan(emptyMap()))
        assertNull(plan(mapOf("Other.dsv" to save + footer, "Game.DX.sav" to save)))   // stems match exactly
        assertNull(plan(mapOf("Game.dsv" to ByteArray(0))))
        assertNull(plan(mapOf("Game.dsv" to footer)))                                   // footer only: nothing to play
        assertNull(NdsDsvSave.plan("Game", candidates, setOf("Game.dsv")) { null })    // unreadable
    }

    @Test fun an_empty_candidate_holds_no_save() {
        assertNotNull(plan(mapOf("Game.sav" to ByteArray(0), "Game.dsv" to save + footer)))
    }

    @Test fun the_magic_must_be_the_files_last_bytes() {
        val p = plan(mapOf("Game.dsv" to save + footer + byteArrayOf(0)))!!
        assertEquals(0, p.footer.size)
        assertEquals(save.size + footer.size + 1, p.bytes.size)
        // shorter than a footer but ending in the magic: no valid footer
        val short = ByteArray(40) + NdsDsvSave.DSV_MAGIC.encodeToByteArray()
        assertEquals(0, plan(mapOf("Game.dsv" to short))!!.footer.size)
    }
}
