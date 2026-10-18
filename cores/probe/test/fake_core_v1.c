/*
 * nx-mobile - fake libretro core: V1 options (RETRO_ENVIRONMENT_SET_CORE_OPTIONS).
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 */
#define FAKE_LIBRARY_NAME "Fake V1"
#include "fake_core.h"

static struct retro_core_option_definition defs[] = {
   {"fake_a", "Option A", NULL, {{"on", NULL}, {"off", NULL}, {NULL, NULL}}, "off"},
   {NULL, NULL, NULL, {{NULL, NULL}}, NULL},
};

RETRO_API void retro_set_environment(retro_environment_t cb) {
   fake_env = cb;
   cb(RETRO_ENVIRONMENT_SET_CORE_OPTIONS, defs);
}

RETRO_API void retro_init(void) {}

RETRO_API bool retro_load_game(const struct retro_game_info *game) {
   return game && game->path;
}
