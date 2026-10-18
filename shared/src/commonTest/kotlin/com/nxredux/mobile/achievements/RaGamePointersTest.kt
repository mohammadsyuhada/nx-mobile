package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class RaGamePointersTest {
    private val h = "a".repeat(32)
    private val home = RaGamePointer("GBA", "home", "Roms/Game Boy Advance (GBA)", "Emerald.gba", "Emerald")

    @Test fun rom_txt_is_relative_to_the_home_root() {
        assertEquals("Roms/Game Boy Advance (GBA)/Emerald.gba", RaRomRecord.text(home))
        assertEquals("Roms/PS/FF7 (USA)/FF7 (USA).m3u", RaRomRecord.text(RaGamePointer("PS", "home", "Roms/PS/FF7 (USA)", "FF7 (USA).m3u", "FF7")))
    }
    @Test fun extras_and_paths_outside_roms_get_no_rom_txt() {
        assertNull(RaRomRecord.text(home.copy(source = "x1", dir = "a/b")))
        assertNull(RaRomRecord.text(home.copy(dir = "Other")))
    }
    @Test fun rom_txt_reads_relative_and_old_absolute() {
        val p = RaRomRecord.pointer("Roms/Game Boy Advance (GBA)/Emerald.gba")!!
        assertEquals(RaGamePointer("GBA", "home", "Roms/Game Boy Advance (GBA)", "Emerald.gba", ""), p)
        assertEquals(p, RaRomRecord.pointer("/mnt/SDCARD/Roms/Game Boy Advance (GBA)/Emerald.gba\n"))
        assertNull(RaRomRecord.pointer("Roms/Emerald.gba"))            // directly in Roms/
        assertNull(RaRomRecord.pointer("Roms/NoTag/Emerald.gba"))      // no (TAG)
        assertNull(RaRomRecord.pointer("Roms/A (GBA)/../x.gba"))
    }
    @Test fun read_prefers_game_json_then_rom_txt() = runTest {
        val store = FakeRaStore()
        store.put(RaPaths.romRecord(h), "Roms/Game Boy Advance (GBA)/Emerald.gba")
        assertEquals("Emerald.gba", RaGamePointers.read(store, h)?.file)
        RaGamePointers.write(store, h, home.copy(file = "Other.gba"))
        assertEquals("Other.gba", RaGamePointers.read(store, h)?.file)
        assertEquals("Roms/Game Boy Advance (GBA)/Other.gba", store.text(RaPaths.romRecord(h)))
    }
    @Test fun remember_keeps_game_json_but_updates_rom_txt() = runTest {
        val store = FakeRaStore()
        RaGamePointers.write(store, h, home)
        RaGamePointers.remember(store, h, home.copy(dir = "Roms/GBA (GBA)", title = "New"))
        assertEquals("Emerald", RaGamePointers.read(store, h)?.title)
        assertEquals("Roms/GBA (GBA)/Emerald.gba", store.text(RaPaths.romRecord(h)))
    }
}
