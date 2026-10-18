/*
 *     Copyright (C) 2026  nx-mobile contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "coreretention.h"

#include <atomic>
#include <dlfcn.h>

namespace libretrodroid::coreretention {

namespace {
std::atomic<bool> keepLoaded { false };
}

void setKeepLoaded(bool keep) {
    keepLoaded.store(keep);
}

int dlopenFlags() {
    // With RTLD_NODELETE the library stays mapped after dlclose, and the next dlopen of the same path returns it again
    // at the same address with its globals as the last session left them (the core's retro_init must reset them).
    return keepLoaded.load() ? RTLD_NODELETE : 0;
}

}
