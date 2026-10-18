/*
 * nx-mobile - fake libretro cores for the nxprobe host tests.
 * Copyright (C) 2026 Mohammad Syuhada. GPL-3.0-or-later.
 *
 * Shared by every test/fake_core_*.c: the entry points nxprobe resolves that a
 * fake core has no reason to vary. Define FAKE_LIBRARY_NAME before including.
 * Each fake core defines retro_set_environment and retro_load_game itself.
 */
#ifndef NXPROBE_FAKE_CORE_H
#define NXPROBE_FAKE_CORE_H

#include "libretro.h"

#include <string.h>

static retro_environment_t fake_env;

RETRO_API unsigned retro_api_version(void) { return RETRO_API_VERSION; }

RETRO_API void retro_get_system_info(struct retro_system_info *info) {
   memset(info, 0, sizeof(*info));
   info->library_name = FAKE_LIBRARY_NAME;
   info->library_version = "1.0";
   info->valid_extensions = "bin";
   info->need_fullpath = true;
}

RETRO_API void retro_deinit(void) {}
RETRO_API void retro_unload_game(void) {}

#endif
