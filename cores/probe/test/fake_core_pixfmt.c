/*
 * nx-mobile - fake libretro core whose retro_load_game fails unless the
 * frontend accepts SET_PIXEL_FORMAT, and which declares its options only after
 * that (as melonDS DS does). It also checks SET_HW_RENDER is refused, so the
 * probe keeps cores on software rendering.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 */
#define FAKE_LIBRARY_NAME "Fake Pixfmt"
#include "fake_core.h"

static struct retro_core_option_v2_category cats[] = {
   {NULL, NULL, NULL},
};

static struct retro_core_option_v2_definition defs[] = {
   {"fake_renderer", "Renderer", NULL, NULL, NULL, NULL,
    {{"software", NULL}, {"opengl", NULL}, {NULL, NULL}}, "software"},
   {NULL, NULL, NULL, NULL, NULL, NULL, {{NULL, NULL}}, NULL},
};

static struct retro_core_options_v2 options = {cats, defs};

RETRO_API void retro_set_environment(retro_environment_t cb) {
   fake_env = cb;
}

RETRO_API void retro_init(void) {}

RETRO_API bool retro_load_game(const struct retro_game_info *game) {
   if (!game || !game->path)
      return false;
   enum retro_pixel_format fmt = RETRO_PIXEL_FORMAT_XRGB8888;
   if (!fake_env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt))
      return false;
   struct retro_hw_render_callback hw;
   memset(&hw, 0, sizeof(hw));
   hw.context_type = RETRO_HW_CONTEXT_OPENGLES3;
   if (fake_env(RETRO_ENVIRONMENT_SET_HW_RENDER, &hw))
      return false;
   fake_env(RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2, &options);
   return true;
}
