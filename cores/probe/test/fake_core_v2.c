/*
 * nx-mobile - fake libretro core: V2 options with one category.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 */
#define FAKE_LIBRARY_NAME "Fake V2"
#include "fake_core.h"

#include <stdlib.h>

static struct retro_core_option_v2_category cats[] = {
   {"video", "Video", "Video options"},
   {NULL, NULL, NULL},
};

static struct retro_core_option_v2_definition defs[] = {
   {"fake_speed", "Speed", NULL, NULL, NULL, NULL,
    {{"normal", "Normal"}, {"fast", "Fast"}, {NULL, NULL}}, "normal"},
   {"fake_res", "Resolution (restart)", "Resolution", NULL, NULL, "video",
    {{"1x", NULL}, {"2x", NULL}, {"3x", NULL}, {NULL, NULL}}, "2x"},
   {NULL, NULL, NULL, NULL, NULL, NULL, {{NULL, NULL}}, NULL},
};

static struct retro_core_options_v2 options = {cats, defs};

RETRO_API void retro_set_environment(retro_environment_t cb) {
   fake_env = cb;
   unsigned version = 0;
   if (cb(RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION, &version) && version >= 2)
      cb(RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2, &options);
}

/* A core that reads its options during init must see the declared default:
 * anything else aborts, which the test sees as a non-zero exit. */
RETRO_API void retro_init(void) {
   /* Stands in for a retro_init that cannot run in a shell (PPSSPP's Vulkan probe). */
   if (getenv("FAKE_CORE_ABORT_INIT"))
      abort();
   struct retro_variable var = {"fake_res", NULL};
   if (!fake_env(RETRO_ENVIRONMENT_GET_VARIABLE, &var) || !var.value || strcmp(var.value, "2x") != 0)
      abort();
}

RETRO_API bool retro_load_game(const struct retro_game_info *game) {
   return game && game->path;
}
