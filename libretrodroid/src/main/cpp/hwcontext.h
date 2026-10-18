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

#ifndef LIBRETRODROID_HWCONTEXT_H
#define LIBRETRODROID_HWCONTEXT_H

namespace libretrodroid {

// Whether a core's SET_HW_RENDER request ([contextType] a retro_hw_context_type, [major].[minor] only read for
// OPENGLES_VERSION) can be served by our GLES context of version [glMajor].[glMinor]. Only GLES types are served:
// the view never creates a desktop GL context. VULKAN only with [allowVulkan] (bridged into GLES, see VulkanBridge).
bool canServeHwContext(unsigned contextType, unsigned major, unsigned minor, int glMajor, int glMinor,
                       bool allowVulkan = false);

}

#endif //LIBRETRODROID_HWCONTEXT_H
