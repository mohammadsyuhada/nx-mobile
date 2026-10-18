package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ArtworkResolverTest {
    private val source = FakeRomsSource(
        dirs = mapOf(
            "Roms" to listOf("Nintendo DS (NDS)", ".media"),
            "Roms/.media" to listOf("Nintendo DS (NDS).png"),
            "Roms/Nintendo DS (NDS)" to listOf("Mario Kart DS.nds", "Contra 4.nds", "Game.v1.2.nds", ".media"),
            "Roms/Nintendo DS (NDS)/.media" to listOf("Mario Kart DS.png", "Game.v1.2.png"),
            "Roms/Game Boy (GB)" to listOf("Tetris.gb"),
            "Roms/Game Boy (GB)/.media" to listOf("box2d"),
            "Roms/Game Boy (GB)/.media/box2d" to listOf("a.jpg", "a.png", "b.jpeg", "c.gif", "d.JPG"),
        ),
    )
    private val resolver = ArtworkResolver(source)

    @Test fun file_entries_map_to_media_png_by_base_name() = runTest {
        val map = resolver.mediaMap(source.listChildren(ContentRef("Roms/Nintendo DS (NDS)")))
        assertEquals(ContentRef("Roms/Nintendo DS (NDS)/.media/Mario Kart DS.png"), resolver.artFor(map, "Mario Kart DS.nds", false))
        assertEquals(ContentRef("Roms/Nintendo DS (NDS)/.media/Game.v1.2.png"), resolver.artFor(map, "Game.v1.2.nds", false))
        assertNull(resolver.artFor(map, "Contra 4.nds", false))
    }
    @Test fun system_folders_map_to_the_root_media_png() = runTest {
        val map = resolver.mediaMap(source.listChildren(ContentRef("Roms")))
        assertEquals(ContentRef("Roms/.media/Nintendo DS (NDS).png"), resolver.artFor(map, "Nintendo DS (NDS)", true))
    }
    @Test fun missing_media_folder_gives_an_empty_map() = runTest {
        assertEquals(emptyMap(), resolver.mediaMap(source.listChildren(ContentRef("Roms/Game Boy (GB)"))))
    }
    @Test fun subArtMapPrefersPngOverJpg() = runTest {
        val media = source.listChildren(ContentRef("Roms/Game Boy (GB)/.media"))
        val map = resolver.subArtMap(media, "box2d")
        assertEquals(
            mapOf(
                "a" to ContentRef("Roms/Game Boy (GB)/.media/box2d/a.png"),     // the .png wins whatever the listing order
                "b" to ContentRef("Roms/Game Boy (GB)/.media/box2d/b.jpeg"),
                "d" to ContentRef("Roms/Game Boy (GB)/.media/box2d/d.JPG"),
            ),
            map,
        )   // c.gif is not art
        assertEquals(emptyMap(), resolver.subArtMap(media, "hero"))
    }

    @Test fun subArtMapPrefersJpgOverJpegWhateverTheListingOrder() = runTest {
        for (names in listOf(listOf("e.jpeg", "e.jpg"), listOf("e.jpg", "e.jpeg"))) {
            val src = FakeRomsSource(dirs = mapOf("M" to listOf("box2d"), "M/box2d" to names))
            val map = ArtworkResolver(src).subArtMap(src.listChildren(ContentRef("M")), "box2d")
            assertEquals(mapOf("e" to ContentRef("M/box2d/e.jpg")), map, "$names")
        }
    }
}
