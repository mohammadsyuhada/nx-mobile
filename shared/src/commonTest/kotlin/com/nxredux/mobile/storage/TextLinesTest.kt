package com.nxredux.mobile.storage

import com.nxredux.mobile.library.MapTxt
import com.nxredux.mobile.library.SidecarTxt
import com.nxredux.mobile.options.CoreConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class TextLinesTest {
    private val bom = "﻿"

    @Test fun split_strips_one_leading_bom_and_any_line_ending() {
        assertEquals(listOf("a", "b", "c", ""), TextLines.split("${bom}a\r\nb\rc\n"))
        assertEquals(listOf("x${bom}y"), TextLines.split("x${bom}y"))   // only a leading one is a mark
    }

    @Test fun every_user_file_reader_ignores_a_bom() {
        assertEquals(mapOf("Mario.gba" to "Super Mario"), MapTxt.parse("${bom}Mario.gba\tSuper Mario\n"))
        assertEquals(mapOf("Zelda.gba" to "MGBA"), SidecarTxt.parse("${bom}Zelda.gba\tMGBA\r\n"))
        assertEquals(listOf("/Roms/A (A)/a.gb"), PathLines.parse("$bom/Roms/A (A)/a.gb\n"))
        assertEquals(listOf(Recent("/Roms/A (A)/a.gb", "A")), Recents.parse("$bom/Roms/A (A)/a.gb\tA\n"))
        assertEquals(listOf(Shortcut("/Roms/A (A)/a.gb", "A")), Shortcuts.parse("$bom/Roms/A (A)/a.gb\tA\n"))
        assertEquals(mapOf("gpsp_bios" to "on"), CoreConfig.parse("${bom}gpsp_bios = on\n").values)
    }
}
