package com.nxredux.mobile.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverlayResolverTest {
    private val sets = OverlaySets(
        p768 = listOf("Native.png", "Aspect.png", "Fullscreen.png"),
        p720 = listOf("Aspect.png", "Wide.png", "Native.png"),
        flat = listOf("Mine.png"),
    )

    @Test fun names_are_the_sorted_union_of_all_sets() {
        assertEquals(listOf("Aspect.png", "Fullscreen.png", "Mine.png", "Native.png", "Wide.png"), OverlayResolver.names(sets))
    }

    @Test fun a_name_in_several_sets_is_listed_once() {
        val dup = OverlaySets(p768 = listOf("Aspect.png"), p720 = listOf("Aspect.png"), flat = listOf("Aspect.png"))
        assertEquals(listOf("Aspect.png"), OverlayResolver.names(dup))
        assertEquals(listOf("None", "Aspect.png"), FrontendOptions.choices(FrontendOptions.OVERLAY, OverlayResolver.names(dup)))
    }

    @Test fun names_skip_hidden_and_non_png_files() {
        val messy = OverlaySets(p768 = listOf(".keep", "._Aspect.png", "notes.txt", "b.PNG", "a - LCD Grid.png"))
        assertEquals(listOf("a - LCD Grid.png", "b.PNG"), OverlayResolver.names(messy))
    }

    @Test fun portrait_uses_the_768p_set() {
        assertEquals(OverlayFile("Overlays/GB/768p/Aspect.png", 4f / 3f), OverlayResolver.resolve("GB", sets, "Aspect.png", landscape = false))
    }

    @Test fun landscape_uses_the_720p_set() {
        assertEquals(OverlayFile("Overlays/GB/720p/Aspect.png", 16f / 9f), OverlayResolver.resolve("GB", sets, "Aspect.png", landscape = true))
    }

    @Test fun a_name_missing_from_the_orientation_set_uses_the_other_set() {
        assertEquals(OverlayFile("Overlays/GB/768p/Fullscreen.png", 4f / 3f), OverlayResolver.resolve("GB", sets, "Fullscreen.png", landscape = true))
        assertEquals(OverlayFile("Overlays/GB/720p/Wide.png", 16f / 9f), OverlayResolver.resolve("GB", sets, "Wide.png", landscape = false))
    }

    @Test fun flat_files_count_as_768p() {
        assertEquals(OverlayFile("Overlays/GB/Mine.png", 4f / 3f), OverlayResolver.resolve("GB", sets, "Mine.png", landscape = false))
        assertEquals(OverlayFile("Overlays/GB/Mine.png", 4f / 3f), OverlayResolver.resolve("GB", sets, "Mine.png", landscape = true))
    }

    @Test fun the_768p_folder_beats_a_flat_file_of_the_same_name() {
        val both = OverlaySets(p768 = listOf("Aspect.png"), flat = listOf("Aspect.png"))
        assertEquals(OverlayFile("Overlays/GB/768p/Aspect.png", 4f / 3f), OverlayResolver.resolve("GB", both, "Aspect.png", landscape = false))
    }

    @Test fun none_missing_or_null_resolve_to_nothing() {
        assertNull(OverlayResolver.resolve("GB", sets, "None", landscape = false))
        assertNull(OverlayResolver.resolve("GB", sets, "Gone.png", landscape = true))
        assertNull(OverlayResolver.resolve("GB", sets, null, landscape = true))
        assertNull(OverlayResolver.resolve("N64", OverlaySets(), "Aspect.png", landscape = false))
    }
}
