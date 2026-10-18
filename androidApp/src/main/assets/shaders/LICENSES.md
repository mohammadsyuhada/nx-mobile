# Bundled shaders: origins

Every file keeps its own license header; the terms in that header apply. This list only records where each
file came from. Nothing here is modified from its source unless noted.

## libretro glsl-shaders

From https://github.com/libretro/glsl-shaders (master, commit f8e23ff880668f0f0e837a05a316534d82a7f31b),
copied with the upstream relative layout:

- `crt/crt-easymode.glslp`
- `crt/crt-geom.glslp`
- `crt/crt-pi.glslp`
- `crt/shaders/crt-easymode.glsl`
- `crt/shaders/crt-geom.glsl`
- `crt/shaders/crt-pi.glsl`
- `crt/shaders/zfast_crt.glsl`
- `crt/zfast-crt.glslp`
- `handheld/dot.glslp`
- `handheld/gameboy.glslp`
- `handheld/lcd3x.glslp`
- `handheld/shaders/dot.glsl`
- `handheld/shaders/gameboy/resources/background.png`
- `handheld/shaders/gameboy/resources/palette.png`
- `handheld/shaders/gameboy/shader-files/gb-pass0.glsl`
- `handheld/shaders/gameboy/shader-files/gb-pass1.glsl`
- `handheld/shaders/gameboy/shader-files/gb-pass2.glsl`
- `handheld/shaders/gameboy/shader-files/gb-pass3.glsl`
- `handheld/shaders/gameboy/shader-files/gb-pass4.glsl`
- `handheld/shaders/lcd3x.glsl`
- `interpolation/shaders/sharp-bilinear.glsl`
- `interpolation/sharp-bilinear.glslp`
- `xbrz/shaders/xbrz-freescale.glsl`
- `xbrz/xbrz-freescale.glslp`

`handheld/shaders/gameboy/` carries only the five `shader-files/gb-pass*.glsl` passes and the default
`resources/palette.png` and `resources/background.png` (upstream's sample palettes/backgrounds and README are
not bundled).

## nx-redux

From nx-redux `skeleton/BASE/Shaders/glsl/` (commit 0b2cb7ff), unmodified unless noted. Several are themselves
glsl-shaders ports; `crt-perfect`, `dmg-perfect`, `lcd-perfect` and `pixel-perfect` are MIT (sinedied):

- `glsl/aa-shader-4.0.glsl`
- `glsl/barrel-distortion.glsl`
- `glsl/bicubic.glsl`
- `glsl/crt-perfect.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/dmg-perfect.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/edge1pixel.glsl`
- `glsl/fast-sharpen.glsl`
- `glsl/lcd-perfect.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/lcd1x.glsl`
- `glsl/lcd3x.glsl`
- `glsl/pixel-perfect.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/pixel_art_AA.glsl`
- `glsl/pixellate.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/res-independent-scanlines.glsl`
- `glsl/retro-v2.glsl`
- `glsl/scale3x.glsl`
- `glsl/scanline.glsl`
- `glsl/sharp-bilinear.glsl`
- `glsl/sharp-shimmerless-grid.glsl`
- `glsl/sharp-shimmerless-subpixel-vrgb.glsl`
- `glsl/sharp-shimmerless.glsl`
- `glsl/stock.glsl`
- `glsl/waterpaint.glsl`

## perfect-retroshaders

From https://github.com/sinedied/perfect-retroshaders (MIT, sinedied; main, commit
4bb6af9d61ee0ce77b23d674f96e896bd1d60392), the GLSL versions from `shaders/`. The nx-redux copies of
`crt-perfect` (v14), `dmg-perfect` (v11), `lcd-perfect` (v10) and `pixel-perfect` (v8) above are identical to this
commit's and are kept as they are.

- `glsl/colour-mini.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/crt-mini.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/dmg-mini.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/lcd-mini.glsl` (modified: vertex-stage COMPAT_PRECISION highp)
- `glsl/unflat-mini.glsl` (modified: vertex-stage COMPAT_PRECISION highp)

## NX Redux

Ports of LibretroDroid's built-in shaders (GPL-3.0, Filippo Scognamiglio; from the `libretrodroid` module's
`shadermanager.cpp`) to RetroArch's single-file GLSL form:

- `nx/line.glsl`, `nx/line.glslp`: the CRT shader ("Line")
- `nx/grid.glsl`, `nx/grid.glslp`: the LCD shader ("Grid")
