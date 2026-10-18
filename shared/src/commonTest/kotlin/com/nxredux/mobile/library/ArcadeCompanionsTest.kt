package com.nxredux.mobile.library

import com.nxredux.mobile.runner.Companion
import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ArcadeCompanionsTest {
    private val sets = ArcadeSets.parse("kof98a\tKOF98 alt\tkof98\tkof98\t\nkof98\tKOF98\t\tneogeo\t\nneogeo\tNeo Geo\t\t\t1\n")
    private val folder = ContentRef("H/FBN")

    @Test fun parent_and_bios_beside_the_game_are_companions() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip", "kof98.zip", "neogeo.zip")))
        val found = ArcadeCompanions.find(src, folder, "kof98a.zip", sets)
        assertEquals(
            ArcadeLookup(
                companions = listOf(Companion("kof98.zip", ContentRef("H/FBN/kof98.zip")), Companion("neogeo.zip", ContentRef("H/FBN/neogeo.zip"))),
                systemSets = emptyList(),
            ),
            found,
        )
        assertEquals(listOf("kof98.zip", "neogeo.zip"), found.companions.map { it.fileName })
    }

    @Test fun finds_sets_whatever_their_case() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("KOF98A.ZIP", "Kof98.Zip", "NEOGEO.7z")))
        val found = ArcadeCompanions.find(src, folder, "KOF98A.ZIP", sets)
        assertEquals(listOf("kof98.zip", "neogeo.7z"), found.companions.map { it.fileName })
        assertEquals(listOf(ContentRef("H/FBN/Kof98.Zip"), ContentRef("H/FBN/NEOGEO.7z")), found.companions.map { it.ref })
        assertEquals(emptyList(), found.systemSets)
    }

    @Test fun zip_is_preferred_over_7z() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip", "kof98.7z", "kof98.zip", "neogeo.zip")))
        assertEquals("kof98.zip", ArcadeCompanions.find(src, folder, "kof98a.zip", sets).companions.first().fileName)
    }

    @Test fun sets_not_beside_the_game_are_left_for_the_system_dir() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip", "kof98.zip")))
        assertEquals(
            ArcadeLookup(listOf(Companion("kof98.zip", ContentRef("H/FBN/kof98.zip"))), listOf("neogeo")),
            ArcadeCompanions.find(src, folder, "kof98a.zip", sets),
        )
    }

    @Test fun sets_beside_the_game_are_found_when_the_folder_lists_empty() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip", "kof98.zip", "neogeo.zip")))
        src.listingFault = ListingFault.EMPTY
        assertEquals(
            ArcadeLookup(listOf(Companion("kof98.zip", ContentRef("H/FBN/kof98.zip")), Companion("neogeo.zip", ContentRef("H/FBN/neogeo.zip"))), emptyList()),
            ArcadeCompanions.find(src, folder, "kof98a.zip", sets),
        )
    }

    @Test fun an_unavailable_listing_leaves_unfound_bios_for_the_system_dir() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip", "kof98.zip")))
        src.listingFault = ListingFault.UNAVAILABLE
        assertEquals(
            ArcadeLookup(listOf(Companion("kof98.zip", ContentRef("H/FBN/kof98.zip"))), listOf("neogeo")),
            ArcadeCompanions.find(src, folder, "kof98a.zip", sets),
        )
    }

    @Test fun a_missing_parent_is_not_a_system_set_but_a_missing_bios_is() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98a.zip")))
        assertEquals(ArcadeLookup(emptyList(), listOf("neogeo")), ArcadeCompanions.find(src, folder, "kof98a.zip", sets))
    }

    @Test fun a_clone_without_its_parent_and_no_bios_needs_nothing_from_the_system_dir() = runTest {
        val cps = ArcadeSets.parse("sf2cebltw\tSF2CE bootleg\tsf2ce\tsf2ce\t\nsf2ce\tSF2CE\t\t\t\n")
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("sf2cebltw.zip")))
        assertEquals(ArcadeLookup(emptyList(), emptyList()), ArcadeCompanions.find(src, folder, "sf2cebltw.zip", cps))
    }

    @Test fun a_set_with_no_chain_has_nothing() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/FBN" to listOf("kof98.zip", "neogeo.zip")))
        assertEquals(ArcadeLookup(emptyList(), emptyList()), ArcadeCompanions.find(src, folder, "kof98.zip", ArcadeSets.EMPTY))
    }

    @Test fun missing_checks_system_and_fbneo_dirs() {
        assertNull(ArcadeCompanions.missing(listOf("neogeo"), setOf("neogeo.zip"), emptySet()))
        assertNull(ArcadeCompanions.missing(listOf("neogeo"), emptySet(), setOf("neogeo.7z")))
        assertEquals("neogeo", ArcadeCompanions.missing(listOf("neogeo", "pgm"), emptySet(), emptySet()))
        assertNull(ArcadeCompanions.missing(emptyList(), emptySet(), emptySet()))
    }

    @Test fun a_mixed_case_bios_in_the_system_dir_counts_as_missing() {
        // The app-private system dir is case-sensitive: fbneo opens <system>/neogeo.zip and would not find NeoGeo.zip.
        assertEquals("neogeo", ArcadeCompanions.missing(listOf("neogeo"), setOf("NeoGeo.zip"), setOf("NEOGEO.7z")))
    }

    private val naomi = NaomiSets.parse("mushik2e\t\tMushiking II\tnaomi\nmushik2k\tmushik2e\tMushiking II (Korea)\tnaomi\n")
    private val dc = ContentRef("H/DC")

    @Test fun a_naomi_clone_takes_its_parent_beside_it_zip_before_7z() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/DC" to listOf("MushiK2K.zip", "mushik2e.7z", "Mushik2E.Zip")))
        assertEquals(listOf(Companion("mushik2e.zip", ContentRef("H/DC/Mushik2E.Zip"))), ArcadeCompanions.findParent(src, dc, "MushiK2K.zip", naomi))
    }

    @Test fun a_naomi_set_without_a_parent_beside_it_takes_nothing() = runTest {
        val src = FakeRomsSource(dirs = mapOf("H/DC" to listOf("mushik2k.zip", "mushik2e.zip")))
        assertEquals(emptyList(), ArcadeCompanions.findParent(FakeRomsSource(dirs = mapOf("H/DC" to listOf("mushik2k.zip"))), dc, "mushik2k.zip", naomi))
        assertEquals(emptyList(), ArcadeCompanions.findParent(src, dc, "mushik2e.zip", naomi))   // a parent
        assertEquals(emptyList(), ArcadeCompanions.findParent(src, dc, "unknown.zip", naomi))
    }
}
