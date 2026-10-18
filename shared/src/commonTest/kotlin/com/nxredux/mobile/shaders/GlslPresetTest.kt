package com.nxredux.mobile.shaders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlslPresetTest {
    private val stock = "#if defined(VERTEX)\nvoid main(){}\n#elif defined(FRAGMENT)\nvoid main(){}\n#endif\n"
    private val withPragmas = "#pragma parameter SCAN \"Scanline weight\" 0.5 0.0 1.0 0.05\n#pragma parameter MASK \"Mask\" 1.0 0.0 2.0\n$stock"

    private fun files(vararg entries: Pair<String, String>): (String) -> String? = { p -> entries.toMap()[p] }

    @Test fun single_pass_defaults() {
        val chain = GlslPreset.parse("crt/a.glslp", files("crt/a.glslp" to "shaders = 1\nshader0 = shaders/a.glsl\n", "crt/shaders/a.glsl" to stock))
        val p = chain.passes.single()
        assertEquals(stock, p.source)
        assertNull(p.filterLinear)
        assertEquals(WrapMode.EDGE, p.wrap)
        assertEquals(ScaleType.NONE, p.scaleTypeX); assertEquals(ScaleType.NONE, p.scaleTypeY)
        assertEquals(1f, p.scaleX); assertEquals(1f, p.scaleY)
        assertEquals(0, p.frameCountMod); assertEquals("", p.alias)
        assertTrue(chain.luts.isEmpty()); assertTrue(chain.params.isEmpty()); assertTrue(chain.warnings.isEmpty())
    }

    @Test fun quoted_values_comments_crlf_and_bom_are_tolerated() {
        val text = "\uFEFFshaders = \"1\"\r\n# comment\r\nshader0 = \"shaders/a.glsl\"  \r\nfilter_linear0 = \"true\"\r\nwrap_mode0 = repeat\r\n"
        val chain = GlslPreset.parse("crt/a.glslp", files("crt/a.glslp" to text, "crt/shaders/a.glsl" to stock))
        assertEquals(true, chain.passes[0].filterLinear)
        assertEquals(WrapMode.REPEAT, chain.passes[0].wrap)
    }

    @Test fun scale_type_precedence_and_per_axis() {
        val preset = "shaders = 3\nshader0 = a.glsl\nscale_type_x0 = viewport\nscale_x0 = 2\n" +
            "shader1 = a.glsl\nscale_type1 = absolute\nscale_type_x1 = source\nscale1 = 320\n" +
            "shader2 = a.glsl\nscale_type_y2 = source\nscale_y2 = 0.5\nscale2 = 3\n"
        val c = GlslPreset.parse("p.glslp", files("p.glslp" to preset, "a.glsl" to stock))
        assertEquals(ScaleType.VIEWPORT, c.passes[0].scaleTypeX); assertEquals(ScaleType.SOURCE, c.passes[0].scaleTypeY)
        assertEquals(2f, c.passes[0].scaleX); assertEquals(1f, c.passes[0].scaleY)
        assertEquals(ScaleType.ABSOLUTE, c.passes[1].scaleTypeX); assertEquals(ScaleType.ABSOLUTE, c.passes[1].scaleTypeY)
        assertEquals(320f, c.passes[1].scaleX); assertEquals(320f, c.passes[1].scaleY)
        assertEquals(ScaleType.SOURCE, c.passes[2].scaleTypeX); assertEquals(ScaleType.SOURCE, c.passes[2].scaleTypeY)
        assertEquals(3f, c.passes[2].scaleX); assertEquals(3f, c.passes[2].scaleY)   // scaleN wins over scale_yN
    }

    @Test fun paths_resolve_relative_to_the_preset_and_cannot_escape_the_root() {
        val c = GlslPreset.parse("crt/sub/p.glslp", files("crt/sub/p.glslp" to "shaders = 1\nshader0 = ../shaders/a.glsl\n", "crt/shaders/a.glsl" to stock))
        assertEquals(stock, c.passes[0].source)
        assertFailsWith<PresetException> {
            GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 1\nshader0 = ../../x.glsl\n", "../../x.glsl" to stock))
        }
        assertEquals("a/c.glsl", GlslPreset.normalize("a/b/../c.glsl"))
        assertNull(GlslPreset.normalize("../x"))
    }

    @Test fun missing_shader_file_and_missing_shaders_key_are_errors() {
        assertFailsWith<PresetException> { GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 1\nshader0 = a.glsl\n")) }
        assertFailsWith<PresetException> { GlslPreset.parse("p.glslp", files("p.glslp" to "shader0 = a.glsl\n", "a.glsl" to stock)) }
        assertFailsWith<PresetException> { GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 9\n")) }
        assertFailsWith<PresetException> { GlslPreset.parse("missing.glslp", files()) }
    }

    @Test fun textures_with_defaults_and_a_missing_file_warning() {
        val preset = "shaders = 1\nshader0 = a.glsl\ntextures = \"PAL;BG\"\nPAL = res/pal.png\nPAL_linear = false\nBG = res/bg.png\nBG_wrap_mode = mirrored_repeat\n"
        val c = GlslPreset.parse("h/p.glslp", files("h/p.glslp" to preset, "h/a.glsl" to stock))
        assertEquals(listOf(ShaderLut("PAL", "h/res/pal.png", false, WrapMode.EDGE), ShaderLut("BG", "h/res/bg.png", true, WrapMode.MIRRORED)), c.luts)
        val c2 = GlslPreset.parse("h/p.glslp", files("h/p.glslp" to "shaders = 1\nshader0 = a.glsl\ntextures = X\n", "h/a.glsl" to stock))
        assertTrue(c2.luts.isEmpty())
        assertTrue(c2.warnings.any { it.contains("X") })
    }

    @Test fun pragmas_are_parsed_stripped_and_overridden_by_the_preset() {
        val c = GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 1\nshader0 = a.glsl\nSCAN = \"0.75\"\n", "a.glsl" to withPragmas))
        assertEquals(listOf(ShaderParam("SCAN", "Scanline weight", 0.75f, 0f, 1f, 0.05f), ShaderParam("MASK", "Mask", 1f, 0f, 2f, 0.2f)), c.params)
        assertEquals(stock, c.passes[0].source)
        assertEquals(ShaderParam("MASK", "Mask", 1f, 0f, 2f, 0.2f), GlslPreset.pragmas(withPragmas)[1])   // step absent → 0.1×(max−min)
    }

    @Test fun a_bom_is_stripped_from_pass_sources() {
        val bom = "\uFEFF#version 120\n#pragma parameter X \"X\" 0.5 0.0 1.0 0.1\nvoid main() {}\n"
        assertTrue(GlslPreset.stripPragmas(bom).startsWith("#version 120"))
        assertEquals(listOf(ShaderParam("X", "X", 0.5f, 0f, 1f, 0.1f)), GlslPreset.pragmas("\uFEFF#pragma parameter X \"X\" 0.5 0.0 1.0 0.1\n"))
    }

    @Test fun malformed_pragmas_are_skipped_and_defaults_clamped() {
        assertTrue(GlslPreset.pragmas("#pragma parameter X \"X\" 0.5 1.0 0.0\n").isEmpty())   // min > max
        assertTrue(GlslPreset.pragmas("#pragma parameter X \"X\" NaN 0.0 1.0 0.1\n").isEmpty())
        assertTrue(GlslPreset.pragmas("#pragma parameter X \"X\" 0.5 0.0 Infinity 0.1\n").isEmpty())
        assertEquals(1f, GlslPreset.pragmas("#pragma parameter X \"X\" 5.0 0.0 1.0 0.1\n").single().default)
        assertEquals(ShaderParam("X", "X", 0.5f, 0f, 1f, 0.1f), GlslPreset.pragmas("#pragma parameter X \"X\" 0.5 0.0 1.0 0.1 // trailing\n").single())
    }

    @Test fun preset_override_is_clamped_and_non_finite_is_ignored() {
        val over = { v: String -> GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 1\nshader0 = a.glsl\nSCAN = $v\n", "a.glsl" to withPragmas)).params[0].default }
        assertEquals(1f, over("7"))
        assertEquals(0.5f, over("NaN"))
    }

    @Test fun textures_beyond_the_limit_warn() {
        val ids = (0..GlslPreset.MAX_LUTS).map { "T$it" }
        val preset = "shaders = 1\nshader0 = a.glsl\ntextures = ${ids.joinToString(";")}\n" + ids.joinToString("") { "$it = $it.png\n" }
        val c = GlslPreset.parse("p.glslp", files("p.glslp" to preset, "a.glsl" to stock))
        assertEquals(GlslPreset.MAX_LUTS, c.luts.size)
        assertEquals(listOf("textures beyond ${GlslPreset.MAX_LUTS} ignored"), c.warnings)
    }

    @Test fun duplicate_pragma_ids_across_passes_keep_the_first() {
        val other = "#pragma parameter SCAN \"Other\" 0.1 0.0 1.0 0.1\n$stock"
        val c = GlslPreset.parse("p.glslp", files("p.glslp" to "shaders = 2\nshader0 = a.glsl\nshader1 = b.glsl\n", "a.glsl" to withPragmas, "b.glsl" to other))
        assertEquals(listOf("SCAN", "MASK"), c.params.map { it.id })
        assertEquals("Scanline weight", c.params[0].label)
    }

    @Test fun bare_glsl_is_a_one_pass_preset() {
        val c = GlslPreset.parse("glsl/lcd3x.glsl", files("glsl/lcd3x.glsl" to withPragmas))
        assertEquals(1, c.passes.size)
        assertNull(c.passes[0].filterLinear)
        assertEquals(ScaleType.NONE, c.passes[0].scaleTypeX)
        assertEquals(2, c.params.size)
    }

    @Test fun ignored_keys_warn_and_reference_is_an_error() {
        val preset = "shaders = 1\nshader0 = a.glsl\nfloat_framebuffer0 = true\nmipmap_input0 = true\nfeedback_pass = 0\nparameters = \"SCAN\"\nframe_count_mod0 = 4\nalias0 = \"P0\"\n"
        val c = GlslPreset.parse("p.glslp", files("p.glslp" to preset, "a.glsl" to stock))
        assertEquals(4, c.warnings.size)
        assertEquals(4, c.passes[0].frameCountMod); assertEquals("P0", c.passes[0].alias)
        assertFailsWith<PresetException> { GlslPreset.parse("p.glslp", files("p.glslp" to "#reference \"other.glslp\"\n")) }
    }
}
