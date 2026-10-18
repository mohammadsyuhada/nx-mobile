/*
 *     Copyright (C) 2026  Mohammad Syuhada
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

#include "hwcontext.h"
#include "libretro.h"

namespace libretrodroid {

bool canServeHwContext(unsigned contextType, unsigned major, unsigned minor, int glMajor, int glMinor,
                       bool allowVulkan) {
    switch (contextType) {
        case RETRO_HW_CONTEXT_OPENGLES2:
            return glMajor >= 2;
        case RETRO_HW_CONTEXT_OPENGLES3:
            return glMajor >= 3;
        case RETRO_HW_CONTEXT_OPENGLES_VERSION:
            return (int) major < glMajor || ((int) major == glMajor && (int) minor <= glMinor);
        case RETRO_HW_CONTEXT_VULKAN:
            return allowVulkan;
        default:
            return false;
    }
}

}
