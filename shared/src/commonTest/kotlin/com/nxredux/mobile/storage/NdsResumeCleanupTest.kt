package com.nxredux.mobile.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NdsResumeCleanupTest {
    private val rom = "Mario Kart DS (USA).nds"
    private val ds = NxPaths("NDS", "melondsds", rom)
    private val legacy = NxPaths("NDS", "melonds", rom)
    private val legacyStates = ".userdata/shared/NDS-melonds/"

    private fun victims(marker: String?, files: Set<String>, tag: String = "NDS", coreId: String = "melondsds") =
        NdsResumeCleanup.victims(tag, coreId, rom, marker) { it in files }

    @Test fun a_marker_without_a_melonds_ds_state_goes_with_its_thumbnails() {
        // The old core's states and thumbnails are all there; none of melonDS DS's.
        val files = setOf(
            ds.lastSlot, ds.thumb(2), ds.thumb(NxPaths.AUTO_SLOT),
            legacy.state(2), legacy.state(NxPaths.AUTO_SLOT),
        )
        assertEquals(listOf(ds.lastSlot, ds.thumb(2), ds.thumb(NxPaths.AUTO_SLOT)), victims("2", files))
    }

    @Test fun a_melonds_ds_state_in_the_marker_slot_keeps_everything() {
        val files = setOf(ds.lastSlot, ds.thumb(2), ds.state(2))
        assertEquals(emptyList(), victims("2", files))
    }

    @Test fun a_melonds_ds_auto_state_keeps_everything() {
        val files = setOf(ds.lastSlot, ds.thumb(2), ds.thumb(NxPaths.AUTO_SLOT), ds.state(NxPaths.AUTO_SLOT))
        assertEquals(emptyList(), victims("2", files))
    }

    @Test fun no_marker_means_nothing_to_do() {
        val files = setOf(ds.thumb(2), legacy.state(2))
        assertEquals(emptyList(), victims(null, files))
    }

    @Test fun a_thumbnail_whose_slot_has_a_melonds_ds_state_stays() {
        // The marker names slot 3 (no state), but slot 5 holds a melonDS DS save: its thumbnail is not stale.
        val files = setOf(ds.lastSlot, ds.thumb(3), ds.thumb(5), ds.state(5))
        assertEquals(listOf(ds.lastSlot, ds.thumb(3)), victims("3", files))
    }

    @Test fun only_nds_on_melonds_ds_is_cleaned() {
        val files = setOf(ds.lastSlot, ds.thumb(2))
        assertEquals(emptyList(), victims("2", files, coreId = "melonds"))
        val gba = NxPaths("GBA", "melondsds", rom)
        assertEquals(emptyList(), victims("2", setOf(gba.lastSlot, gba.thumb(2)), tag = "GBA"))
    }

    @Test fun nothing_under_the_old_cores_state_dir_is_ever_returned() {
        val everything = buildSet {
            add(ds.lastSlot)
            for (slot in (0 until NxPaths.SLOT_COUNT) + NxPaths.AUTO_SLOT) {
                add(ds.thumb(slot)); add(legacy.state(slot))
            }
        }
        val out = victims("9", everything)
        assertTrue(out.isNotEmpty())
        assertTrue(out.none { it.startsWith(legacyStates) }, out.toString())
    }

    @Test fun a_state_under_another_format_name_keeps_the_marker() {
        val files = setOf(".userdata/shared/.minui/NDS/G.nds.txt", ".userdata/shared/NDS-melondsds/G.nds.st0")
        assertTrue(NdsResumeCleanup.victims("NDS", "melondsds", "G.nds", "0", SaveNaming.DEFAULT) { it in files }.isEmpty())
    }

    @Test fun a_state_under_the_extracted_name_keeps_the_marker() {
        val files = setOf(".userdata/shared/.minui/NDS/G.zip.txt", ".userdata/shared/NDS-melondsds/Inner.state")
        val naming = SaveNaming(useExtractedFileName = true)
        assertTrue(NdsResumeCleanup.victims("NDS", "melondsds", "G.zip", "0", naming, "Inner.nds") { it in files }.isEmpty())
    }
}
