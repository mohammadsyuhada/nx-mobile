/*
 * nx-mobile - fake libretro core that declares its options only in
 * retro_load_game (as FBNeo does), including a per-game DIP switch.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 */
#define FAKE_LIBRARY_NAME "Fake Load"
#include "fake_core.h"

static struct retro_core_option_v2_category cats[] = {
   {NULL, NULL, NULL},
};

static struct retro_core_option_v2_definition defs[] = {
   {"fake_cpu", "CPU Clock", NULL, NULL, NULL, NULL,
    {{"100", NULL}, {"150", NULL}, {NULL, NULL}}, "100"},
   {"fbneo-dipswitch-fake-Lives", "Lives", NULL, NULL, NULL, NULL,
    {{"3", NULL}, {"5", NULL}, {NULL, NULL}}, "3"},
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
   fake_env(RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2, &options);
   /* A content path containing "fail" declares its options, then fails the
    * load, as FBNeo does for a set it cannot open. */
   return strstr(game->path, "fail") == NULL;
}
