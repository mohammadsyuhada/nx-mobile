/*
 * nx-mobile - fake libretro core: legacy RETRO_ENVIRONMENT_SET_VARIABLES.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 */
#define FAKE_LIBRARY_NAME "Fake Vars"
#include "fake_core.h"

static const struct retro_variable vars[] = {
   {"fake_mode", "Mode; auto|manual"},
   {NULL, NULL},
};

RETRO_API void retro_set_environment(retro_environment_t cb) {
   fake_env = cb;
   cb(RETRO_ENVIRONMENT_SET_VARIABLES, (void *)vars);
}

RETRO_API void retro_init(void) {}

RETRO_API bool retro_load_game(const struct retro_game_info *game) {
   return game && game->path;
}
