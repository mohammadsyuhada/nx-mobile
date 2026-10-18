package com.nxredux.mobile.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoreOptionTablesTest {
    private val json = """
        {"core":"gpsp","emulator":"gpSP","soSha256":"ab","probedAt":"2026-09-29","sections":[
         {"key":"","label":"","info":"","items":[
          {"key":"gpsp_frameskip","label":"Frameskip","labelCategorized":"","info":"Skip frames",
           "values":["disabled","auto"],"labels":["Disabled",""],"default":"disabled"}]},
         {"key":"video","label":"Video","info":"Video things","items":[
          {"key":"gpsp_color","label":"Color Correction (restart)","labelCategorized":"Color Correction","info":"",
           "values":["disabled","enabled"],"labels":["",""],"default":"enabled"}]}]}
    """.trimIndent()

    @Test fun parses_sections_into_categories_and_defaults_into_values() {
        val t = CoreOptionTables.parse(json)!!
        assertEquals("gpsp", t.core); assertEquals("gpSP", t.emulator)
        val skip = t.options[0]
        assertEquals("gpsp_frameskip", skip.key); assertEquals("", skip.category)
        assertEquals("disabled", skip.value); assertEquals("disabled", skip.defaultValue)
        assertEquals(listOf("Disabled", ""), skip.valueLabels)
        val color = t.options[1]
        assertEquals("video", color.category); assertEquals("Video", color.categoryLabel); assertEquals("Video things", color.categoryInfo)
        assertEquals("Color Correction", color.labelCategorized)
    }

    @Test fun malformed_json_is_null() {
        assertNull(CoreOptionTables.parse("{"))
        assertNull(CoreOptionTables.parse("""{"core":"x"}"""))
    }

    @Test fun unknown_fields_are_ignored() {
        val t = CoreOptionTables.parse("""{"core":"a","emulator":"A","extra":1,"sections":[]}""")!!
        assertEquals(emptyList(), t.options)
    }

    @Test fun a_default_outside_the_values_falls_back_to_the_first_value() {
        // genesis_plus_gx_overclock declares "100%" but lists "100", "125", ... (RetroArch uses values[0] then).
        val t = CoreOptionTables.parse("""{"core":"g","emulator":"G","sections":[{"items":[
            {"key":"oc","values":["100","125"],"default":"100%"},
            {"key":"free","values":[],"default":"x"}]}]}""")!!
        assertEquals("100", t.options[0].defaultValue); assertEquals("100", t.options[0].value)
        assertEquals("x", t.options[1].defaultValue)
    }
}
