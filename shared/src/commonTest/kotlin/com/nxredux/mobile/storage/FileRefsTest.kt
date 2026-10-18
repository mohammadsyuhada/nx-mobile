package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileRefsTest {
    @Test fun a_file_ref_is_the_raw_path_after_the_scheme() {
        val ref = FileRefs.of("/storage/emulated/0/NXRedux")
        assertEquals(ContentRef("file:///storage/emulated/0/NXRedux"), ref)
        assertTrue(FileRefs.isFile(ref))
        assertEquals("/storage/emulated/0/NXRedux", FileRefs.path(ref))
        assertFalse(FileRefs.isFile(ContentRef("content://com.android.externalstorage.documents/tree/primary%3ANXRedux")))
        assertFalse(FileRefs.isFile(ContentRef("Home")))
    }

    @Test fun names_stay_raw_never_percent_encoded() {
        val rom = FileRefs.child(FileRefs.of("/storage/emulated/0/Roms/GB"), "Pokémon #1 (100%)?.gb")
        assertEquals("file:///storage/emulated/0/Roms/GB/Pokémon #1 (100%)?.gb", rom.value)
        assertEquals("/storage/emulated/0/Roms/GB/Pokémon #1 (100%)?.gb", FileRefs.path(rom))
    }

    @Test fun of_trims_a_trailing_slash_so_child_never_doubles_it() {
        assertEquals(ContentRef("file:///storage/1234-ABCD"), FileRefs.of("/storage/1234-ABCD/"))
        assertEquals("file:///storage/1234-ABCD/Roms", FileRefs.child(FileRefs.of("/storage/1234-ABCD/"), "Roms").value)
        assertEquals("file:///x", FileRefs.child(FileRefs.of("/"), "x").value)
    }

    @Test fun a_plain_name_is_one_path_segment() {
        for (bad in listOf("", ".", "..", "a/b", "/")) assertFalse(FileRefs.isPlainName(bad), bad)
        for (ok in listOf("a b", ".media", "x..y", "Pokémon #1?")) assertTrue(FileRefs.isPlainName(ok), ok)
    }

    @Test fun art_on_shared_storage_maps_to_its_path_and_other_models_do_not() {
        assertEquals(
            "/storage/emulated/0/Roms/GB/.media/boxart/Pokémon #1?.png",
            FileRefs.artPath("file:///storage/emulated/0/Roms/GB/.media/boxart/Pokémon #1?.png"),
        )
        // App-private versioned refs (AndroidArtFiles) keep Coil's own file:// handling: the ?v= query is their cache key.
        assertNull(FileRefs.artPath("file:///data/user/0/com.nxredux.mobile/files/android-art/com.a.png?v=1000"))
        assertNull(FileRefs.artPath("content://com.android.externalstorage.documents/tree/primary%3AX/document/primary%3AX%2Fa.png"))
        assertNull(FileRefs.artPath("nx-abstract://seed/1"))
    }

    @Test fun access_is_off_only_for_a_file_home_without_the_grant() {
        val file = ContentRef("file:///storage/emulated/0/Roms")
        assertTrue(FileRefs.accessOff(file, granted = false))
        assertFalse(FileRefs.accessOff(file, granted = true))
        assertFalse(FileRefs.accessOff(ContentRef("content://x/tree/a"), granted = false))
        assertFalse(FileRefs.accessOff(null, granted = false))
    }
}
