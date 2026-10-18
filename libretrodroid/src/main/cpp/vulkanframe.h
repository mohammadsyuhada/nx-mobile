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

#ifndef LIBRETRODROID_VULKANFRAME_H
#define LIBRETRODROID_VULKANFRAME_H

#include <algorithm>

namespace libretrodroid {

struct FrameExtent {
    unsigned width;
    unsigned height;
};

// The region of the core's image the Vulkan bridge copies: what the core reports, within [limitWidth] x [limitHeight]
// (0 = no limit known), never empty. The limit is the core's max geometry, or the bridge's surface for a core that
// renders into a swapchain on it (Dolphin reports its EFB size, which can exceed that image).
inline FrameExtent boundFrame(unsigned width, unsigned height, unsigned limitWidth, unsigned limitHeight) {
    unsigned w = limitWidth > 0 ? std::min(width, limitWidth) : width;
    unsigned h = limitHeight > 0 ? std::min(height, limitHeight) : height;
    return { std::max(1u, w), std::max(1u, h) };
}

}

#endif //LIBRETRODROID_VULKANFRAME_H
